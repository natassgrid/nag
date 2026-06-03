package com.examplatform.delivery.config;

import com.examplatform.delivery.domain.ExamSession.ExamSessionStatus;
import com.examplatform.delivery.repository.ExamSessionRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Registers custom Micrometer metrics for HPA auto-scaling.
 *
 * The {@code active_exam_sessions} gauge reports the current number of ACTIVE
 * exam sessions. Kubernetes HPA uses this metric (target: 5,000 per pod) to
 * scale the delivery-service deployment horizontally.
 */
@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    private final MeterRegistry meterRegistry;
    private final ExamSessionRepository examSessionRepository;

    @PostConstruct
    public void registerCustomMetrics() {
        Gauge.builder("active_exam_sessions", examSessionRepository,
                repo -> repo.countByStatus(ExamSessionStatus.ACTIVE))
            .description("Number of currently active exam sessions (HPA target: 5000/pod)")
            .tag("service", "delivery-service")
            .register(meterRegistry);
    }
}
