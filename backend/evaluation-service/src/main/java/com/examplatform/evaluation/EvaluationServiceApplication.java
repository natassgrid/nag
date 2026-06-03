package com.examplatform.evaluation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Evaluation Service.
 * Handles auto-evaluation of objective answers and orchestrates manual evaluation
 * workflows for subjective questions.
 */
@SpringBootApplication
@EnableScheduling
public class EvaluationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvaluationServiceApplication.class, args);
    }
}
