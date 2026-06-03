package com.examplatform.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Notification Service.
 * Handles multi-channel notification delivery (Email, Push, In-App)
 * with retry logic and delivery tracking.
 */
@SpringBootApplication
@EnableScheduling
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
