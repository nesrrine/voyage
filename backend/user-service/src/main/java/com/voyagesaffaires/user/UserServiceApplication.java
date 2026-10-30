package com.voyagesaffaires.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** Microservice "Gestion des utilisateurs" (port 8081). */
@SpringBootApplication(scanBasePackages = "com.voyagesaffaires")
@EnableFeignClients
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
