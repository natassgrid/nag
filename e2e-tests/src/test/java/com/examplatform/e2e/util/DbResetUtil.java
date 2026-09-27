/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) — Open Digital Public Infrastructure (DPI) Platform
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
 */
package com.examplatform.e2e.util;

import com.examplatform.e2e.config.E2ETestConfig;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;

/**
 * Utility class that performs a full state reset between E2E test runs.
 *
 * <p>All four cleanup operations ({@linkplain #resetPostgres() Postgres},
 * {@linkplain #flushRedis() Redis}, {@linkplain #resetWireMock() WireMock},
 * {@linkplain #deleteMailhogMessages() MailHog}) are executed independently
 * inside try-catch blocks so that one failure does not abort the others.
 *
 * <p>Entry point: {@link #reset()}.
 */
public final class DbResetUtil {

    private static final Logger LOG = LoggerFactory.getLogger(DbResetUtil.class);

    /**
     * Schemas and tables to truncate before each E2E test.
     * Tables are listed in dependency order so CASCADE handles FK chains.
     */
    private static final String TRUNCATE_SQL =
            "TRUNCATE "
            + "response_service.responses, "
            + "delivery_service.exam_sessions, "
            + "result_service.results, "
            + "audit_service.audit_logs "
            + "RESTART IDENTITY CASCADE";

    private static final String REDIS_HOST = "localhost";
    private static final int    REDIS_PORT  = 6379;
    private static final String REDIS_KEY_PATTERN = "e2e:*";

    private DbResetUtil() {
        // utility class — no instances
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Performs a full E2E state reset:
     * <ol>
     *   <li>Truncates transactional tables in PostgreSQL</li>
     *   <li>Flushes {@code e2e:*} keys from Redis</li>
     *   <li>Resets all WireMock scenario states</li>
     *   <li>Deletes all messages from MailHog</li>
     * </ol>
     * Errors are logged at WARN level and do not propagate.
     */
    public static void reset() {
        resetPostgres();
        flushRedis();
        resetWireMock();
        deleteMailhogMessages();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Truncates the E2E transactional tables via JDBC.
     * Catches {@link Exception} to handle the case where the schema/tables do
     * not exist yet in the current environment (e.g. a freshly provisioned DB).
     */
    private static void resetPostgres() {
        try (Connection conn = DriverManager.getConnection(
                E2ETestConfig.getDbUrl(),
                E2ETestConfig.getDbUsername(),
                E2ETestConfig.getDbPassword());
             Statement stmt = conn.createStatement()) {

            stmt.execute(TRUNCATE_SQL);
            LOG.info("[DbResetUtil] PostgreSQL tables truncated successfully.");

        } catch (Exception ex) {
            LOG.warn("[DbResetUtil] PostgreSQL reset skipped or failed (tables may not exist yet): {}",
                    ex.getMessage());
        }
    }

    /**
     * Deletes all Redis keys matching {@value #REDIS_KEY_PATTERN} using Lettuce.
     */
    private static void flushRedis() {
        RedisClient client = null;
        try {
            RedisURI uri = RedisURI.builder()
                    .withHost(REDIS_HOST)
                    .withPort(REDIS_PORT)
                    .withTimeout(Duration.ofSeconds(5))
                    .build();
            client = RedisClient.create(uri);

            try (StatefulRedisConnection<String, String> conn = client.connect()) {
                RedisCommands<String, String> commands = conn.sync();
                List<String> keys = commands.keys(REDIS_KEY_PATTERN);
                if (!keys.isEmpty()) {
                    commands.del(keys.toArray(new String[0]));
                    LOG.info("[DbResetUtil] Flushed {} Redis key(s) matching '{}'.",
                            keys.size(), REDIS_KEY_PATTERN);
                } else {
                    LOG.debug("[DbResetUtil] No Redis keys matching '{}' found.", REDIS_KEY_PATTERN);
                }
            }
        } catch (Exception ex) {
            LOG.warn("[DbResetUtil] Redis flush failed (Redis may not be running): {}",
                    ex.getMessage());
        } finally {
            if (client != null) {
                try {
                    client.shutdown();
                } catch (Exception ignore) {
                    // best-effort shutdown
                }
            }
        }
    }

    /**
     * Resets all WireMock scenario states via the admin API.
     * Endpoint: {@code POST /__admin/scenarios/reset}
     */
    private static void resetWireMock() {
        try {
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(E2ETestConfig.getWiremockUrl() + "/__admin/scenarios/reset"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString());
            LOG.info("[DbResetUtil] WireMock scenarios reset. HTTP status: {}",
                    response.statusCode());
        } catch (Exception ex) {
            LOG.warn("[DbResetUtil] WireMock reset failed (WireMock may not be running): {}",
                    ex.getMessage());
        }
    }

    /**
     * Deletes all messages stored in MailHog.
     * Endpoint: {@code DELETE /api/v1/messages}
     */
    private static void deleteMailhogMessages() {
        try {
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(E2ETestConfig.getMailhogUrl() + "/api/v1/messages"))
                    .DELETE()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString());
            LOG.info("[DbResetUtil] MailHog messages deleted. HTTP status: {}",
                    response.statusCode());
        } catch (Exception ex) {
            LOG.warn("[DbResetUtil] MailHog delete failed (MailHog may not be running): {}",
                    ex.getMessage());
        }
    }
}
