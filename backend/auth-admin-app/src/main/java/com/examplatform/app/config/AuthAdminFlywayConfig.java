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
 * Executes Flyway database migrations for identity_service, candidate_service,
 * admin_service, and notification_service sequentially within auth-admin-app.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "platform.flyway.auto-migrate", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AuthAdminFlywayConfig {

    private final DataSource dataSource;

    @PostConstruct
    public void migrate() {
        List<SchemaMigrationSpec> specs = List.of(
                SchemaMigrationSpec.of("identity_service", "classpath:db/migration/identity"),
                SchemaMigrationSpec.of("candidate_service", "classpath:db/migration/candidate"),
                SchemaMigrationSpec.of("admin_service", "classpath:db/migration/admin"),
                SchemaMigrationSpec.of("notification_service", "classpath:db/migration/notification")
        );
        MultiSchemaFlywayRunner.runMigrations(dataSource, specs);
    }
}
