package com.voyagesaffaires.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway : le frontend Angular n'appelle que http://localhost:8080/api/**,
 * la gateway route chaque requete vers le bon microservice trouve dans Eureka (lb://nom-du-service).
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
