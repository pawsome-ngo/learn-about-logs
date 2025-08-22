package com.example.spring_aop_logback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class SpringAopLogbackApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringAopLogbackApplication.class, args);
	}

}