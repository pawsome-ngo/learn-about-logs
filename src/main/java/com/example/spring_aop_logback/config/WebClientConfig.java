package com.example.spring_aop_logback.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    private static final String CORRELATION_ID_HEADER_NAME = "X-Correlation-ID";

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .filter(correlationIdFilter())
                .build();
    }

    private ExchangeFilterFunction correlationIdFilter() {
        return (clientRequest, next) -> {
            // --- CORRECTED: Use the raw string "CR_ID" ---
            String correlationId = MDC.get("CR_ID");

            ClientRequest newRequest = ClientRequest.from(clientRequest)
                    .header(CORRELATION_ID_HEADER_NAME, correlationId)
                    .build();
            return next.exchange(newRequest);
        };
    }
}