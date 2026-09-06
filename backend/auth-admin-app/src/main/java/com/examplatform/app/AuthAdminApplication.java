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

package com.examplatform.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Macro-service aggregator for Identity, Candidate, Admin, and Notification services.
 * Combines authentication, user profile management, administration, and notification dispatch
 * into a single unified deployment unit while preserving modular domain separation.
 */
@SpringBootApplication(
        exclude = {
                org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration.class
        },
        scanBasePackages = {
                "com.examplatform.identity",
                "com.examplatform.candidate",
                "com.examplatform.admin",
                "com.examplatform.notification",
                "com.examplatform.shared",
                "com.examplatform.app"
        }
)
@EnableJpaRepositories(
        basePackages = {
                "com.examplatform.identity.repository",
                "com.examplatform.candidate.repository",
                "com.examplatform.admin.repository",
                "com.examplatform.notification.repository"
        }
)
@EnableAsync
@EnableScheduling
public class AuthAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthAdminApplication.class, args);
    }
}
