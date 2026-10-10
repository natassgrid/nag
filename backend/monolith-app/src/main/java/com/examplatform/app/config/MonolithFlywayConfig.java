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

package com.examplatform.app.config;

import com.examplatform.shared.db.MultiSchemaFlywayRunner;
import com.examplatform.shared.db.MultiSchemaFlywayRunner.SchemaMigrationSpec;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;

/**
 * Executes sequential Flyway database migrations for all 14 schemas within the Monolith JVM.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "platform.flyway.auto-migrate", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class MonolithFlywayConfig {

    private final DataSource dataSource;

    private static final String[][] SERVICE_SCHEMAS = {
            {"identity_service", "identity"},
            {"candidate_service", "candidate"},
            {"examination_service", "examination"},
            {"paper_generator", "paper_generator"},
            {"delivery_service", "delivery"},
            {"response_service", "response"},
            {"evaluation_service", "evaluation"},
            {"result_service", "result"},
            {"audit_service", "audit"},
            {"notification_service", "notification"},
            {"admin_service", "admin"},
            {"analytics_service", "analytics"},
            {"asset_service", "asset"},
            {"practice_service", "practice"},
            {"recommendation_service", "recommendation"}
    };

    @PostConstruct
    public void migrate() {
        List<SchemaMigrationSpec> specs = new java.util.ArrayList<>();
        specs.add(SchemaMigrationSpec.of("question_service",
                "classpath:db/migration/question",
                "classpath:db/migration/question/seeds",
                "classpath:db/migration/question/seeds/rrb_ntpc",
                "classpath:db/migration/question/seeds/sbi_po",
                "classpath:db/migration/question/seeds/statement_and_conclusion"));
        for (String[] mapping : SERVICE_SCHEMAS) {
            specs.add(SchemaMigrationSpec.of(mapping[0], "classpath:db/migration/" + mapping[1]));
        }
        MultiSchemaFlywayRunner.runMigrations(dataSource, specs);
    }
}
