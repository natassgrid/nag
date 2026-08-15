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

package com.examplatform.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for DDoS mitigation.
 * Controls the per-origin rate threshold and the sliding window duration.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "gateway.ddos")
public class DDoSProperties {

    /**
     * Maximum number of requests allowed from a single origin (IP) within the window.
     * Default: 10,000 requests per second.
     */
    private int threshold = 10000;

    /**
     * Duration of the sliding window in seconds.
     * Default: 1 second.
     */
    private int windowSeconds = 1;
}
