package com.voyagesaffaires.voyage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** Microservice "Gestion des voyages" (port 8082). */
@SpringBootApplication(scanBasePackages = "com.voyagesaffaires")
@EnableFeignClients
public class VoyageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoyageServiceApplication.class, args);
    }
}
