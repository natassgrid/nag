/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 Open Digital Public Infrastructure (DPI) Platform Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */

package com.examplatform.shared.db;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.util.List;

/**
 * Reusable Flyway migration runner capable of running isolated schema migrations
 * for multiple bounded contexts in a shared physical PostgreSQL database.
 */
@Slf4j
public final class MultiSchemaFlywayRunner {

    private MultiSchemaFlywayRunner() {
        // Utility class
    }

    public record SchemaMigrationSpec(String schema, String locations) {
        public static SchemaMigrationSpec of(String schema, String... locations) {
            return new SchemaMigrationSpec(schema, String.join(",", locations));
        }
    }

    /**
     * Executes Flyway migrations sequentially across the specified schemas.
     *
     * @param dataSource Target PostgreSQL DataSource
     * @param specs List of schema names and their corresponding classpath migration locations
     */
    public static void runMigrations(DataSource dataSource, List<SchemaMigrationSpec> specs) {
        log.info("🚀 Initiating Multi-Schema Flyway Migration for {} schemas...", specs.size());

        for (SchemaMigrationSpec spec : specs) {
            log.info("▶ Running Flyway migrations for schema '{}' from location '{}'...",
                    spec.schema(), spec.locations());

            try {
                // Ensure schema exists before running Flyway
                try (var connection = dataSource.getConnection();
                     var statement = connection.createStatement()) {
                    statement.execute("CREATE SCHEMA IF NOT EXISTS " + spec.schema());
                }

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
                log.info("✔ Flyway migration for schema '{}' completed successfully ({} migrations applied).",
                        spec.schema(), appliedMigrations);
            } catch (Exception e) {
                log.error("❌ Failed to execute Flyway migration for schema '{}': {}", spec.schema(), e.getMessage(), e);
                throw new IllegalStateException("Flyway multi-schema migration failed for schema: " + spec.schema(), e);
            }
        }

        log.info("🎉 All Multi-Schema Flyway migrations completed successfully.");
    }
}
