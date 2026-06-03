package com.examplatform.response;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Response Service.
 * Captures, persists, and manages candidate responses during live exam sessions.
 * Supports auto-save, manual save, navigation-triggered saves, and offline sync.
 */
@SpringBootApplication
@EnableScheduling
public class ResponseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResponseServiceApplication.class, args);
    }
}
