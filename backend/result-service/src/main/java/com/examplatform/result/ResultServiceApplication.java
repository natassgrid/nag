package com.examplatform.result;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Result Service.
 * Aggregates evaluation scores, computes ranks and percentiles, generates
 * scorecard PDFs, and integrates with DigiLocker for credential issuance.
 */
@SpringBootApplication
@EnableScheduling
public class ResultServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResultServiceApplication.class, args);
    }
}
