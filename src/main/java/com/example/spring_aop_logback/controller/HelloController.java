package com.example.spring_aop_logback.controller;

import com.example.spring_aop_logback.dto.WelcomeRequest;
import com.example.spring_aop_logback.dto.WelcomeResponse;
import com.example.spring_aop_logback.service.HelloService;
import com.example.spring_aop_logback.service.WelcomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
public class HelloController {

    @Autowired
    private HelloService helloService;

    // Inject WelcomeService instead of the client directly
    @Autowired
    private WelcomeService welcomeService;

    @GetMapping("/hello")
    public Mono<String> hello(@RequestParam(value = "name", defaultValue = "World") String name) {
        return helloService.getHelloMessage(name);
    }

    @PostMapping("/welcome")
    public Mono<WelcomeResponse> postWelcome(@RequestBody WelcomeRequest request) {
        // --- CORRECTED: Call the service layer ---
        return welcomeService.processWelcomeMessage(request);
    }
}