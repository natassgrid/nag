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

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Single JVM Monolith Application.
 * Aggregates all 14 platform domain services into a single Spring Boot runtime.
 * Supports zero-broker embedded in-memory event dispatch, single shared database pool,
 * and optional lightweight observability.
 */
@SpringBootApplication(
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
        exclude = {
                org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration.class
        },
        scanBasePackages = {
                "com.examplatform.identity",
                "com.examplatform.candidate",
                "com.examplatform.questionbank",
                "com.examplatform.examination",
                "com.examplatform.papergenerator",
                "com.examplatform.delivery",
                "com.examplatform.response",
                "com.examplatform.evaluation",
                "com.examplatform.result",
                "com.examplatform.audit",
                "com.examplatform.notification",
                "com.examplatform.admin",
                "com.examplatform.analytics",
                "com.examplatform.asset",
                "com.examplatform.shared",
                "com.examplatform.app"
        }
)
@EnableJpaRepositories(
        basePackages = {
                "com.examplatform.identity",
                "com.examplatform.candidate",
                "com.examplatform.questionbank",
                "com.examplatform.examination",
                "com.examplatform.papergenerator",
                "com.examplatform.delivery",
                "com.examplatform.response",
                "com.examplatform.evaluation",
                "com.examplatform.result",
                "com.examplatform.audit",
                "com.examplatform.notification",
                "com.examplatform.admin",
                "com.examplatform.analytics",
                "com.examplatform.asset"
        }
)
@EnableAsync
@EnableScheduling
public class MonolithApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(MonolithApplication.class)
                .beanNameGenerator(new FullyQualifiedAnnotationBeanNameGenerator())
                .run(args);
    }
}
