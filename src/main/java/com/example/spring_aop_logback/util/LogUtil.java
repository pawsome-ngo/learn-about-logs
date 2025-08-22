package com.example.spring_aop_logback.util;

import com.example.spring_aop_logback.enums.LoggingEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class LogUtil {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Processes and formats log entries from MDC.
     */
    public void processLogs() {
        String logs = Arrays.stream(LoggingEnum.values())
                // -- NEW: Only include keys that have a value in the MDC --
                .filter(e -> StringUtils.hasText(MDC.get(e.name())))
                .map(e -> e.name() + "=" + MDC.get(e.name()))
                .collect(Collectors.joining("; "));

        boolean hasError = StringUtils.hasText(MDC.get(LoggingEnum.ERROR_DESC.name()));
//        System.out.println("IF Block Below - starts");
        if (hasError) {
//            System.out.println("YES ERROR");
            log.error(logs);
        } else {
//            System.out.println("NO ERROR");
            log.info(logs);
        }
//        System.out.println("IF Block ENDS....");
    }
}