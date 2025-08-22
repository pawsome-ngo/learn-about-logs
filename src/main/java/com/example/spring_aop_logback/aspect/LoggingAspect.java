package com.example.spring_aop_logback.aspect;

import com.example.spring_aop_logback.enums.LoggingEnum;
import com.example.spring_aop_logback.util.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * Aspect for handling application-wide logging for the controller and service layers.
 * It ensures a Correlation ID (CR_ID) is present for all requests and logs
 * entry/exit points for controllers and service methods.
 */
@Aspect
@Component
public class LoggingAspect {
    private static final String CORRELATION_ID_HEADER_NAME = "X-Correlation-ID";
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private LogUtil logUtil;

    /**
     * Pointcut that matches all public methods in any class within the 'controller' package.
     */
    @Pointcut("within(com.example.spring_aop_logback.controller..*)")
    public void controllerPointcut() {
    }

    /**
     * Pointcut that matches all public methods in any class within the 'service' package.
     */
    @Pointcut("execution(* com.example.spring_aop_logback.service.*.*(..))")
    public void servicePointcut() {
    }

    /**
     * An 'Around' advice that wraps controller methods to provide comprehensive request/response logging.
     * It manages the CR_ID, captures request metadata, and handles context propagation for reactive types (Mono).
     */
    @Around("controllerPointcut()")
    public Object logAroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        // Establish the Correlation ID: either from the incoming request header or generate a new one.
        String correlationId = request.getHeader(CORRELATION_ID_HEADER_NAME);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put("CR_ID", correlationId);

        long startTime = System.currentTimeMillis();

        // Populate MDC with initial request details.
        MDC.put(LoggingEnum.RQST_RCVD_TS.name(), Instant.now().toString());
        MDC.put(LoggingEnum.HTTP_METHOD.name(), request.getMethod());
        MDC.put(LoggingEnum.METHOD.name(), joinPoint.getSignature().getName());
        MDC.put(LoggingEnum.URI.name(), request.getRequestURI());

        try {
            Object result = joinPoint.proceed();

            // CRITICAL: If the controller returns a Mono, we must bridge the MDC to the
            // reactive context to prevent context loss across threads.
            if (result instanceof Mono) {
                Map<String, String> mdcContextMap = MDC.getCopyOfContextMap();
                return ((Mono<?>) result)
                        .doFinally(signalType -> {
                            // This block runs when the Mono completes.
                            // We must restore the MDC here for the final log.
                            if (mdcContextMap != null) {
                                MDC.setContextMap(mdcContextMap);
                            }
                            long timeTaken = System.currentTimeMillis() - startTime;
                            MDC.put(LoggingEnum.RQST_RESP_TS.name(), Instant.now().toString());
                            MDC.put(LoggingEnum.EXECUTION_TIME.name(), String.valueOf(timeTaken) + "ms");
                            MDC.put(LoggingEnum.SOURCE.name(), "CONTROLLER");
                            logUtil.processLogs();
                            MDC.clear();
                        })
                        .contextWrite(Context.of("mdcContext", mdcContextMap));
            }

            // For non-reactive (synchronous) controller methods, return the result directly.
            // The 'finally' block below will handle the logging.
            return result;

        } finally {
            // This 'finally' block will only handle logging for NON-REACTIVE methods.
            // The Mono's doFinally handles the reactive case.
            boolean isReactive = (joinPoint.getSignature().getDeclaringType().getMethod(joinPoint.getSignature().getName(), ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getParameterTypes()).getReturnType() == Mono.class);
            if (!isReactive) {
                long timeTaken = System.currentTimeMillis() - startTime;
                MDC.put(LoggingEnum.RQST_RESP_TS.name(), Instant.now().toString());
                MDC.put(LoggingEnum.EXECUTION_TIME.name(), String.valueOf(timeTaken) + "ms");
                MDC.put(LoggingEnum.SOURCE.name(), "CONTROLLER");
                logUtil.processLogs();
                MDC.clear();
            }
        }
    }

    /**
     * A 'Before' advice that logs the entry into any service layer method.
     * This provides a clear trace of the business logic being executed.
     */
    @Before("servicePointcut()")
    public void logBeforeService(JoinPoint joinPoint) {
        log.info("SOURCE=SERVICE; METHOD={}; ARGS={}",
                joinPoint.getSignature().getName(),
                Arrays.toString(joinPoint.getArgs()));
    }
}