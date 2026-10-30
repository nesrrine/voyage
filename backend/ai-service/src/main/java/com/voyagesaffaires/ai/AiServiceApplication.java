package com.voyagesaffaires.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** Microservice "IA" (port 8084) : n'a pas de base de donnees, il interroge les autres services. */
@SpringBootApplication(scanBasePackages = "com.voyagesaffaires")
@EnableFeignClients
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
