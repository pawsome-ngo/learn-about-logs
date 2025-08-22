package com.example.spring_aop_logback.aspect;

import com.example.spring_aop_logback.enums.ExternalLoggingEnum;
import com.example.spring_aop_logback.util.ExternalLogUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

/**
 * Aspect for providing detailed, structured logging for all outgoing external API calls.
 * This class intercepts methods in external web clients, captures the full request/response cycle,
 * and logs it in a consistent format. It is designed to handle reactive types (Mono) and
 * correctly propagate the MDC context to ensure tracing IDs (like CR_ID) are never lost.
 */
@Aspect
@Component
public class ExternalServiceLoggingAspect {

    @Autowired
    private ExternalLogUtil externalLogUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${welcome.service.url}")
    private String welcomeServiceBaseUrl;

    @Value("${uce.service.url}")
    private String uceServiceBaseUrl;

    /**
     * Defines a pointcut that targets all methods within any class in the 'util.web' package.
     * This is the central point for adding new external clients to be logged. To add a new client,
     * simply place its class in this package, and its methods will be intercepted automatically.
     */
    @Pointcut("execution(* com.example.spring_aop_logback.util.web.*.*(..))")
    public void externalServiceClientPointcut() {
    }

    /**
     * An 'Around' advice that wraps the execution of the intercepted external client methods.
     * This is where all the logging logic is orchestrated.
     *
     * @param joinPoint Represents the intercepted method (e.g., getWelcomeMessage).
     * @return The original result of the method call, wrapped in logging logic.
     * @throws Throwable If the intercepted method throws an exception.
     */
    @Around("externalServiceClientPointcut()")
    public Object logAroundExternalCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String serviceName = joinPoint.getSignature().getDeclaringType().getSimpleName().replace("Client", "").toUpperCase();
        String url = getUrlForService(serviceName, joinPoint);

        // Capture the request body if the intercepted method has arguments.
        if (joinPoint.getArgs().length > 0) {
            Object requestBody = joinPoint.getArgs()[0];
            try {
                MDC.put(ExternalLoggingEnum.RQST_BODY.name(), objectMapper.writeValueAsString(requestBody));
            } catch (Exception e) {
                MDC.put(ExternalLoggingEnum.RQST_BODY.name(), "Error serializing request body");
            }
        }

        // CRITICAL: Capture the MDC map from the current thread to pass into the reactive context.
        Map<String, String> mdcContextMap = MDC.getCopyOfContextMap();

        Object result = joinPoint.proceed();

        if (result instanceof Mono) {
            return ((Mono<?>) result)
                    .flatMap(responseBody -> {
                        // Restore the MDC to ensure the CR_ID is available on the current (potentially different) thread.
                        if (mdcContextMap != null) {
                            MDC.setContextMap(mdcContextMap);
                        }
                        try {
                            String responseBodyString = objectMapper.writeValueAsString(responseBody);
                            populateMdc(serviceName, url, startTime, 200, responseBodyString, null);
                        } catch (Exception e) {
                            populateMdc(serviceName, url, startTime, 200, "Error serializing response body", null);
                        }
                        externalLogUtil.processLogs();
                        // CRITICAL: Only remove keys specific to this aspect to avoid clearing the CR_ID.
                        Arrays.stream(ExternalLoggingEnum.values()).forEach(e -> MDC.remove(e.name()));
                        return Mono.just(responseBody);
                    })
                    .onErrorResume(error -> {
                        // Restore the MDC for error logging.
                        if (mdcContextMap != null) {
                            MDC.setContextMap(mdcContextMap);
                        }
                        populateMdc(serviceName, url, startTime, 500, null, error);
                        externalLogUtil.processLogs();
                        // CRITICAL: Only remove keys specific to this aspect.
                        Arrays.stream(ExternalLoggingEnum.values()).forEach(e -> MDC.remove(e.name()));
                        return Mono.error(error);
                    })
                    // Pass the captured MDC map into the reactive stream's context.
                    .contextWrite(Context.of("mdcContext", mdcContextMap));
        }

        return result;
    }

    /**
     * Helper method to populate the MDC with all necessary details for external logging.
     */
    private void populateMdc(String serviceName, String url, long startTime, int statusCode, String body, Throwable error) {
        MDC.put(ExternalLoggingEnum.SOURCE.name(), "EXTERNAL");
        MDC.put(ExternalLoggingEnum.SERVICE_NAME.name(), serviceName);
        MDC.put(ExternalLoggingEnum.URL.name(), url);
        MDC.put(ExternalLoggingEnum.RQST_TS.name(), Instant.ofEpochMilli(startTime).toString());
        MDC.put(ExternalLoggingEnum.RESP_TS.name(), Instant.now().toString());
        MDC.put(ExternalLoggingEnum.HTTP_STATUS.name(), String.valueOf(statusCode));
        long timeTaken = System.currentTimeMillis() - startTime;
        MDC.put(ExternalLoggingEnum.EXECUTION_TIME.name(), timeTaken + "ms");

        if (body != null && !body.isEmpty()) {
            MDC.put(ExternalLoggingEnum.RESP_BODY.name(), body);
        }
        if (error != null) {
            MDC.put(ExternalLoggingEnum.ERROR_DESC.name(), error.getMessage());
        }
    }

    /**
     * Helper to get the target URL by combining the base URL from properties with the endpoint path.
     */
    private String getUrlForService(String serviceName, ProceedingJoinPoint joinPoint) {
        if ("WELCOME".equals(serviceName)) {
            return welcomeServiceBaseUrl + "/welcome";
        }
        if ("UCE".equals(serviceName)) {
            return uceServiceBaseUrl + "/entitlementCheck";
        }
        return "UNKNOWN_URL";
    }
}