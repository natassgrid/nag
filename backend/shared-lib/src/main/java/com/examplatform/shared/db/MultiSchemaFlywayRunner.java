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

package com.examplatform.shared.db;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.util.List;

/**
 * Utility for running Flyway migrations across multiple schemas sequentially within a single JVM.
 * Ensures zero migration version collisions by executing an isolated Flyway run per schema
 * against its dedicated location and schema history table.
 */
@Slf4j
public class MultiSchemaFlywayRunner {

    public record SchemaMigrationSpec(String schema, String... locations) {
        public static SchemaMigrationSpec of(String schema, String... locations) {
            return new SchemaMigrationSpec(schema, locations);
        }
    }

    /**
     * Executes Flyway migrations sequentially for each provided schema specification.
     *
     * @param dataSource the shared datasource
     * @param specs      the list of schema and location specifications to migrate
     */
    public static void runMigrations(DataSource dataSource, List<SchemaMigrationSpec> specs) {
        log.info("Starting Multi-Schema Flyway migrations for {} schema(s)...", specs.size());

        for (SchemaMigrationSpec spec : specs) {
            try {
                log.info("? Running Flyway migration for schema '{}' from location(s): {}",
                        spec.schema(), String.join(", ", spec.locations()));

                Flyway flyway = Flyway.configure()
                        .dataSource(dataSource)
                        .schemas(spec.schema())
                        .defaultSchema(spec.schema())
                        .table("flyway_schema_history")
                        .locations(spec.locations())
                        .baselineOnMigrate(true)
                        .validateOnMigrate(false)
                        .load();

                int appliedMigrations = flyway.migrate().migrationsExecuted;
                log.info("? Flyway migration for schema '{}' completed successfully ({} migrations applied).",
                        spec.schema(), appliedMigrations);
            } catch (Exception e) {
                log.error("? Failed to execute Flyway migration for schema '{}': {}", spec.schema(), e.getMessage(), e);
                throw new RuntimeException("Flyway multi-schema migration failed for schema: " + spec.schema(), e);
            }
        }

        log.info("? All Multi-Schema Flyway migrations completed successfully.");
    }
}
