package com.example.spring_aop_logback.util.web;

import com.example.spring_aop_logback.dto.WelcomeRequest;
import com.example.spring_aop_logback.dto.WelcomeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class WelcomeServiceClient {

    @Autowired
    private WebClient webClient;

    @Value("${welcome.service.url}")
    private String welcomeServiceBaseUrl;

    public Mono<String> getWelcomeMessage() {
        return this.webClient.get()
                .uri(welcomeServiceBaseUrl + "/welcome")
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<WelcomeResponse> postWelcomeMessage(WelcomeRequest request) {
        return this.webClient.post()
                .uri(welcomeServiceBaseUrl + "/welcome")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(WelcomeResponse.class);
    }
}