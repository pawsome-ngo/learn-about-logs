package com.example.spring_aop_logback.util;

import com.example.spring_aop_logback.enums.ExternalLoggingEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class ExternalLogUtil {

    private final Logger log = LoggerFactory.getLogger("external-log"); // Use a dedicated logger category

    /**
     * Processes and formats log entries from MDC for external calls.
     */
    public void processLogs() {
        String logs = Arrays.stream(ExternalLoggingEnum.values())
                .filter(e -> StringUtils.hasText(MDC.get(e.name())))
                .map(e -> e.name() + "=" + MDC.get(e.name()))
                .collect(Collectors.joining("; "));

        boolean hasError = StringUtils.hasText(MDC.get(ExternalLoggingEnum.ERROR_DESC.name()));

        if (hasError) {
            log.error(logs);
        } else {
            log.info(logs);
        }
    }
}