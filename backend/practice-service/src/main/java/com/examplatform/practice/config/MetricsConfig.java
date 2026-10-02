// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.config;

import com.examplatform.practice.repository.PracticeSessionRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    private final MeterRegistry meterRegistry;
    private final PracticeSessionRepository practiceSessionRepository;

    @PostConstruct
    public void registerCustomMetrics() {
        Gauge.builder("active_practice_sessions", practiceSessionRepository,
                repo -> repo.count()) // Approximation since we don't have a countByStatus method
            .description("Number of total practice sessions (placeholder for active)")
            .tag("service", "practice-service")
            .register(meterRegistry);
    }
}
