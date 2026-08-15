/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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
