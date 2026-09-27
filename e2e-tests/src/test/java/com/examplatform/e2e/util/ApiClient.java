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
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Typed REST client wrapper that manages a Keycloak-issued Bearer token.
 *
 * <p>Usage:
 * <pre>{@code
 * ApiClient admin = ApiClient.getAdminClient();
 * admin.given()
 *      .body(payload)
 *      .post("/api/v1/examinations")
 *      .then().statusCode(201);
 * }</pre>
 *
 * <p>Tokens are obtained via the Keycloak Resource Owner Password Credentials
 * (ROPC) grant, which must be enabled on the {@code exam-backend} client.
 */
public class ApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(ApiClient.class);

    private String accessToken;

    // ── Factory methods ───────────────────────────────────────────────────────

    /**
     * Returns an {@link ApiClient} authenticated as the superadmin user.
     *
     * @return authenticated admin client
     */
    public static ApiClient getAdminClient() {
        ApiClient client = new ApiClient();
        client.authenticate(
                E2ETestConfig.getAdminUsername(),
                E2ETestConfig.getAdminPassword());
        return client;
    }

    /**
     * Returns an {@link ApiClient} authenticated as the default candidate user.
     *
     * @return authenticated candidate client
     */
    public static ApiClient getCandidateClient() {
        ApiClient client = new ApiClient();
        client.authenticate(
                E2ETestConfig.getCandidateUsername(),
                E2ETestConfig.getCandidatePassword());
        return client;
    }

    // ── Authentication ────────────────────────────────────────────────────────

    /**
     * Authenticates with Keycloak using the Resource Owner Password Credentials grant
     * and stores the resulting {@code access_token} for subsequent requests.
     *
     * <p>Token endpoint:
     * {@code {keycloakUrl}/realms/{realm}/protocol/openid-connect/token}
     *
     * @param username Keycloak username
     * @param password Keycloak password
     * @throws AssertionError if the token response status is not 200
     */
    public void authenticate(String username, String password) {
        String tokenEndpoint = E2ETestConfig.getKeycloakUrl()
                + "/realms/" + E2ETestConfig.getKeycloakRealm()
                + "/protocol/openid-connect/token";

        LOG.debug("[ApiClient] Obtaining token for user '{}' from {}", username, tokenEndpoint);

        Response tokenResponse = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "password")
                .formParam("client_id", E2ETestConfig.getKeycloakClientId())
                .formParam("username", username)
                .formParam("password", password)
                .post(tokenEndpoint);

        if (tokenResponse.statusCode() != 200) {
            LOG.error("[ApiClient] Token request failed. Status={} Body={}",
                    tokenResponse.statusCode(), tokenResponse.asString());
        }

        this.accessToken = tokenResponse.jsonPath().getString("access_token");

        if (this.accessToken == null || this.accessToken.isBlank()) {
            LOG.warn("[ApiClient] Received null/blank access_token for user '{}'. "
                    + "Keycloak may not be running or credentials are invalid.", username);
        } else {
            LOG.debug("[ApiClient] Token obtained successfully for user '{}'.", username);
        }
    }

    // ── Request builder ───────────────────────────────────────────────────────

    /**
     * Returns a pre-configured REST Assured {@link RequestSpecification} with:
     * <ul>
     *   <li>{@code Authorization: Bearer <token>} header</li>
     *   <li>{@code Content-Type: application/json} header</li>
     * </ul>
     *
     * @return pre-authenticated request specification
     */
    public RequestSpecification given() {
        return RestAssured.given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json");
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /**
     * Returns the raw Bearer access token (JWT).
     *
     * @return access token string, or {@code null} if not yet authenticated
     */
    public String getAccessToken() {
        return accessToken;
    }
}
