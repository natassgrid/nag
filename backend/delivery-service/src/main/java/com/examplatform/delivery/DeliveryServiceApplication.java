package com.examplatform.delivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Delivery Service.
 * Manages live exam sessions, shift-key decryption, and real-time delivery of
 * question papers to candidates.
 */
@SpringBootApplication
@EnableScheduling
public class DeliveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliveryServiceApplication.class, args);
    }
}
