package com.voyagesaffaires.forum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Microservice "Forum" (port 8083). */
@SpringBootApplication(scanBasePackages = "com.voyagesaffaires")
public class ForumServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ForumServiceApplication.class, args);
    }
}
