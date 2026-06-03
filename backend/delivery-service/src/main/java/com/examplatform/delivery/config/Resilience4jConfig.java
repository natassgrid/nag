package com.examplatform.delivery.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j circuit breaker configuration.
 * Configures the "questionBank" circuit breaker:
 * - OPEN after 5 failures in a sliding window of 10 calls (50% failure rate)
 * - HALF_OPEN probe after 30 seconds
 * - CLOSED on recovery (3 permitted calls in half-open succeed)
 *
 * Validates: design error-handling
 */
@Configuration
public class Resilience4jConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig questionBankConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();

        return CircuitBreakerRegistry.of(
                java.util.Map.of("questionBank", questionBankConfig)
        );
    }
}
