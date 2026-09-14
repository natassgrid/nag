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

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate configuration for outbound calls to delivery-service.
 * Uses short timeouts to keep response-save latency within the 200ms p99 SLA.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate for delivery-service integrity calls.
     * Connect timeout: 50ms, Read timeout: 100ms (circuit-breaker threshold).
     */
    @Bean(name = "deliveryRestTemplate")
    public RestTemplate deliveryRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofMillis(50))
                .readTimeout(Duration.ofMillis(100))
                .build();
    }
}
