package com.examplatform.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Audit Service.
 * Provides immutable, tamper-evident audit trail for all platform events.
 * Supports 7-year retention with range-partitioned storage.
 */
@SpringBootApplication
@EnableScheduling
public class AuditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}
