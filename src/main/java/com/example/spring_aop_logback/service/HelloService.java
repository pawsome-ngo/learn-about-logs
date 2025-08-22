package com.example.spring_aop_logback.service;

import com.example.spring_aop_logback.enums.LoggingEnum;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class HelloService {

    @Autowired
    private WelcomeService welcomeService;

    public Mono<String> getHelloMessage(String name) {
        if (name.equalsIgnoreCase("error")) {
            MDC.put(LoggingEnum.ERROR_DESC.name(), "Invalid name provided: " + name);
            return Mono.error(new IllegalArgumentException("Invalid name provided"));
        }
        return welcomeService.getWelcomeMessage().map(welcomeMsg -> "Hello, " + name + "! " + welcomeMsg);
    }
}