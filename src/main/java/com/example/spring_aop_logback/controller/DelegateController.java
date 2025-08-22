package com.example.spring_aop_logback.controller;

import com.example.spring_aop_logback.dto.DelegateCreationRequest;
import com.example.spring_aop_logback.dto.DelegateCreationResponse;
import com.example.spring_aop_logback.service.DelegateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class DelegateController {

    @Autowired
    private DelegateService delegateService;

    @PostMapping("/createDelegate")
    public Mono<DelegateCreationResponse> createDelegate(@RequestBody DelegateCreationRequest request) {
        return delegateService.createDelegate(request);
    }
}