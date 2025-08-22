package com.example.spring_aop_logback.service;

import com.example.spring_aop_logback.dto.WelcomeRequest;
import com.example.spring_aop_logback.dto.WelcomeResponse;
import com.example.spring_aop_logback.util.web.WelcomeServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class WelcomeService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private WelcomeServiceClient welcomeServiceClient;

    public Mono<String> getWelcomeMessage() {
        return welcomeServiceClient.getWelcomeMessage();
    }

    /**
     * NEW: Add a method to handle the business logic for the POST call.
     * This method will now be intercepted by your LoggingAspect.
     */
    public Mono<WelcomeResponse> processWelcomeMessage(WelcomeRequest request) {
        // Here you could add business logic, validation, etc.
        log.info("This is from the WELCOME SERVICE");
        return welcomeServiceClient.postWelcomeMessage(request);
    }
}