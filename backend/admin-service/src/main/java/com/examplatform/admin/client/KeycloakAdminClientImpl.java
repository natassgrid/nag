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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.\
 */

package com.examplatform.admin.client;

import com.examplatform.admin.config.KeycloakProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

/**
 * Production implementation of KeycloakAdminClient using Keycloak Admin REST API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakAdminClientImpl implements KeycloakAdminClient {

    private final KeycloakProperties keycloakProperties;
    private final RestClient restClient = RestClient.create();

    @Override
    public void disableUser(UUID userId, String tenantId) {
        log.info("Disabling user {} in Keycloak for tenant {}", userId, tenantId);

        String adminToken;
        try {
            adminToken = getAdminToken();
        } catch (Exception e) {
            log.error("Failed to obtain Keycloak admin token when attempting to disable user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Keycloak admin service unavailable: " + e.getMessage(), e);
        }

        String realm = (tenantId != null && !tenantId.isBlank() && !tenantId.equals("default"))
                ? tenantId
                : keycloakProperties.getRealm();

        String userUrl = keycloakProperties.getServerUrl() + "/admin/realms/" + realm + "/users/" + userId;
        String logoutUrl = userUrl + "/logout";

        try {
            // 1. Disable user account
            restClient.put()
                    .uri(userUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("enabled", false))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully disabled user {} in Keycloak realm {}", userId, realm);

            // 2. Revoke active Keycloak sessions
            restClient.post()
                    .uri(logoutUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully revoked all Keycloak sessions for user {} in realm {}", userId, realm);
        } catch (Exception e) {
            log.error("Failed to disable user {} or revoke sessions in Keycloak realm {}: {}", userId, realm, e.getMessage());
            throw new RuntimeException("Failed to update user status in Keycloak: " + e.getMessage(), e);
        }
    }

    private String getAdminToken() {
        String tokenUrl = keycloakProperties.getServerUrl()
                + "/realms/master/protocol/openid-connect/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        if (keycloakProperties.getAdminClientSecret() != null && !keycloakProperties.getAdminClientSecret().isBlank()) {
            form.add("grant_type", "client_credentials");
            form.add("client_id", keycloakProperties.getAdminClientId());
            form.add("client_secret", keycloakProperties.getAdminClientSecret());
        } else {
            form.add("grant_type", "password");
            form.add("client_id", keycloakProperties.getAdminClientId());
            form.add("username", keycloakProperties.getAdminUsername());
            form.add("password", keycloakProperties.getAdminPassword());
        }

        try {
            Map<?, ?> response = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("access_token")) {
                return (String) response.get("access_token");
            }
            throw new IllegalStateException("Missing access_token in Keycloak admin token response");
        } catch (Exception e) {
            log.error("Error obtaining Keycloak admin token: {}", e.getMessage());
            throw new RuntimeException("Keycloak admin token request failed", e);
        }
    }
}
