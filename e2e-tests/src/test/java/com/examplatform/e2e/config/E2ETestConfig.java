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
package com.examplatform.e2e.config;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Central configuration loader for the E2E test suite.
 *
 * <p>Reads {@code e2e-application.properties} from the classpath (no Spring context).
 * Call {@link #configureRestAssured()} once in a {@code @BeforeAll} hook before
 * issuing any REST Assured requests.
 */
public final class E2ETestConfig {

    private static final String PROPERTIES_FILE = "e2e-application.properties";
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream is = E2ETestConfig.class
                .getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {
            if (is == null) {
                throw new IllegalStateException(
                        "Cannot find '" + PROPERTIES_FILE + "' on classpath. "
                        + "Ensure src/test/resources is included in the test source set.");
            }
            PROPS.load(is);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Failed to load E2E configuration from '" + PROPERTIES_FILE + "'", ex);
        }
    }

    private E2ETestConfig() {
        // utility class — no instances
    }

    // ── Property accessors ────────────────────────────────────────────────────

    /** Base URL of the API Gateway / reverse proxy under test (e.g. http://localhost:9000). */
    public static String getBaseUrl() {
        return PROPS.getProperty("e2e.base.url");
    }

    /** Keycloak server URL (e.g. http://localhost:8081). */
    public static String getKeycloakUrl() {
        return PROPS.getProperty("e2e.keycloak.url");
    }

    /** Keycloak realm name. */
    public static String getKeycloakRealm() {
        return PROPS.getProperty("e2e.keycloak.realm");
    }

    /** Keycloak OAuth2 client-id used for password-grant token requests. */
    public static String getKeycloakClientId() {
        return PROPS.getProperty("e2e.keycloak.client.id");
    }

    /** JDBC URL for the platform's primary PostgreSQL database. */
    public static String getDbUrl() {
        return PROPS.getProperty("e2e.db.url");
    }

    /** PostgreSQL username. */
    public static String getDbUsername() {
        return PROPS.getProperty("e2e.db.username");
    }

    /** PostgreSQL password. */
    public static String getDbPassword() {
        return PROPS.getProperty("e2e.db.password");
    }

    /** MailHog API base URL (e.g. http://localhost:8025). */
    public static String getMailhogUrl() {
        return PROPS.getProperty("e2e.mailhog.url");
    }

    /** WireMock admin base URL (e.g. http://localhost:8089). */
    public static String getWiremockUrl() {
        return PROPS.getProperty("e2e.wiremock.url");
    }

    /** Username of the superadmin account seeded via seed-data.sql. */
    public static String getAdminUsername() {
        return PROPS.getProperty("e2e.admin.username");
    }

    /** Password for the superadmin account. */
    public static String getAdminPassword() {
        return PROPS.getProperty("e2e.admin.password");
    }

    /** Username of the candidate account seeded via seed-data.sql. */
    public static String getCandidateUsername() {
        return PROPS.getProperty("e2e.candidate.username");
    }

    /** Password for the candidate account. */
    public static String getCandidatePassword() {
        return PROPS.getProperty("e2e.candidate.password");
    }

    /**
     * Shared secret token used to authorise the {@code /e2e/reset} endpoint
     * (if implemented). Must never be deployed to production environments.
     */
    public static String getResetToken() {
        return PROPS.getProperty("e2e.reset.token");
    }

    // ── REST Assured bootstrap ────────────────────────────────────────────────

    /**
     * Configures global REST Assured defaults.
     *
     * <ul>
     *   <li>Sets {@link RestAssured#baseURI} to {@link #getBaseUrl()}</li>
     *   <li>Enables request/response logging on validation failures</li>
     * </ul>
     *
     * <p>Call once from a {@code @BeforeAll} method.
     */
    public static void configureRestAssured() {
        RestAssured.baseURI = getBaseUrl();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}
