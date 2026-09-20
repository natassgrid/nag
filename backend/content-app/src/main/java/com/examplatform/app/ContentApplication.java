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
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Macro-service aggregator for Question Bank, Examination, Paper Generator, and Asset services.
 * Manages the entire pre-exam content lifecycle: question authoring, translation, exam blueprints,
 * paper generation, and asset management in a single deployment unit.
 */
@SpringBootApplication(
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
        scanBasePackages = {
                "com.examplatform.questionbank",
                "com.examplatform.examination",
                "com.examplatform.papergenerator",
                "com.examplatform.asset",
                "com.examplatform.shared",
                "com.examplatform.app"
        }
)
@EnableJpaRepositories(
        basePackages = {
                "com.examplatform.questionbank",
                "com.examplatform.examination",
                "com.examplatform.papergenerator",
                "com.examplatform.asset"
        }
)
@EnableAsync
@EnableScheduling
public class ContentApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ContentApplication.class);
        app.setBeanNameGenerator(new FullyQualifiedAnnotationBeanNameGenerator());
        app.run(args);
    }
}
