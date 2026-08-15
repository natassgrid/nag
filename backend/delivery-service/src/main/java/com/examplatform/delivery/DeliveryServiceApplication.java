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

package com.examplatform.delivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Delivery Service.
 * Manages live exam sessions, shift-key decryption, and real-time delivery of
 * question papers to candidates.
 *
 * RedisRepositoriesAutoConfiguration is excluded because Redis is used only for
 * hot exam session state (via RedisTemplate), not for Spring Data Redis repositories.
 * Without this exclusion, Spring Data enters multi-store strict mode and scans every
 * JPA repository interface against both stores, adding significant startup latency.
 */
@SpringBootApplication(exclude = RedisRepositoriesAutoConfiguration.class)
@EnableJpaRepositories(basePackages = "com.examplatform.delivery.repository")
@EnableScheduling
public class DeliveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliveryServiceApplication.class, args);
    }
}
