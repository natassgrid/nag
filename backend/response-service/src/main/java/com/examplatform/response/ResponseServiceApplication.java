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

package com.examplatform.response;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Response Service.
 * Captures, persists, and manages candidate responses during live exam sessions.
 * Supports auto-save, manual save, navigation-triggered saves, and offline sync.
 *
 * RedisRepositoriesAutoConfiguration is excluded because Redis is used only for
 * hot session state (via RedisTemplate), not for Spring Data Redis repositories.
 * Without this exclusion, Spring Data enters multi-store strict mode and scans every
 * JPA repository interface against both stores, adding significant startup latency.
 */
@SpringBootApplication(exclude = { org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration.class })
@EnableJpaRepositories(basePackages = "com.examplatform.response.repository")
@EnableScheduling
public class ResponseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResponseServiceApplication.class, args);
    }
}
