package com.example.spring_aop_logback.enums;

public enum ExternalLoggingEnum {
    SOURCE,
    SERVICE_NAME,
    URL,
    HTTP_STATUS,    // <-- RENAMED
    RQST_TS,        // <-- ADDED
    RESP_TS,        // <-- ADDED
    EXECUTION_TIME,
    RQST_BODY,      // <-- ADDED
    RESP_BODY,      // <-- ADDED
    ERROR_DESC;
}