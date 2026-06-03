package com.examplatform.response.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Registers custom Micrometer metrics for HPA auto-scaling.
 *
 * The {@code response_save_rate} counter tracks the number of response saves.
 * Kubernetes HPA uses the rate of this counter (target: 50,000 saves/min/pod)
 * to scale the response-service deployment horizontally.
 *
 * <p>Usage: inject this config and call {@code getResponseSaveCounter().increment()}
 * on each successful response save.
 */
@Configuration
@Getter
public class MetricsConfig {

    private final MeterRegistry meterRegistry;
    private Counter responseSaveCounter;

    public MetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void registerCustomMetrics() {
        responseSaveCounter = Counter.builder("response_save_rate")
            .description("Number of response saves (HPA target: 50,000 saves/min/pod)")
            .tag("service", "response-service")
            .register(meterRegistry);
    }
}
