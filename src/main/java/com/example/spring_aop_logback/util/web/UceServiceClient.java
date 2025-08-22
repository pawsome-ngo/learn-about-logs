package com.example.spring_aop_logback.util.web;

import com.example.spring_aop_logback.dto.UceRequest;
import com.example.spring_aop_logback.dto.UceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class UceServiceClient {

    @Autowired
    private WebClient webClient;

    @Value("${uce.service.url}")
    private String uceServiceBaseUrl;

    public Mono<UceResponse> checkEntitlement(UceRequest request) {
        return this.webClient.post()
                .uri(uceServiceBaseUrl + "/entitlementCheck")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UceResponse.class);
    }
}