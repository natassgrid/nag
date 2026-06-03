package com.examplatform.identity.service;

import com.examplatform.identity.config.KeycloakProperties;
import com.examplatform.identity.dto.AuthTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {

    private final KeycloakProperties keycloakProperties;
    private final RestClient restClient = RestClient.create();

    /**
     * Exchange username + password for JWT tokens via Keycloak token endpoint.
     * POST {serverUrl}/realms/{realm}/protocol/openid-connect/token
     */
    public AuthTokenResponse getTokens(String username, String password) {
        String tokenUrl = keycloakProperties.getServerUrl()
            + "/realms/" + keycloakProperties.getRealm()
            + "/protocol/openid-connect/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", keycloakProperties.getClientId());
        form.add("client_secret", keycloakProperties.getClientSecret());
        form.add("username", username);
        form.add("password", password);

        try {
            Map<?, ?> response = restClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

            if (response == null) {
                throw new IllegalStateException("Empty response from Keycloak token endpoint");
            }

            long expiresIn = response.containsKey("expires_in")
                ? Long.parseLong(response.get("expires_in").toString()) : 900L;

            return AuthTokenResponse.builder()
                .accessToken((String) response.get("access_token"))
                .refreshToken((String) response.get("refresh_token"))
                .expiresIn(expiresIn)
                .tokenType("Bearer")
                .build();
        } catch (Exception e) {
            log.error("Failed to obtain tokens from Keycloak for user {}: {}", username, e.getMessage());
            throw new RuntimeException("Authentication service unavailable. Please try again.", e);
        }
    }

    /**
     * Enable a user in Keycloak by keycloakUserId.
     * PUT {serverUrl}/admin/realms/{realm}/users/{id}
     */
    public void activateUser(String keycloakUserId) {
        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            log.warn("Skipping Keycloak activation — keycloakUserId is null/blank");
            return;
        }

        String adminToken = getAdminToken();
        String userUrl = keycloakProperties.getServerUrl()
            + "/admin/realms/" + keycloakProperties.getRealm()
            + "/users/" + keycloakUserId;

        try {
            restClient.put()
                .uri(userUrl)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("enabled", true))
                .retrieve()
                .toBodilessEntity();
            log.info("Keycloak user {} activated", keycloakUserId);
        } catch (Exception e) {
            log.error("Failed to activate Keycloak user {}: {}", keycloakUserId, e.getMessage());
            // Non-fatal: local account is already activated; Keycloak sync can be retried
        }
    }

    /**
     * Obtain a short-lived admin access token using admin-cli credentials.
     */
    private String getAdminToken() {
        String tokenUrl = keycloakProperties.getServerUrl()
            + "/realms/master/protocol/openid-connect/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", keycloakProperties.getAdminClientId());
        form.add("username", keycloakProperties.getAdminUsername());
        form.add("password", keycloakProperties.getAdminPassword());

        Map<?, ?> response = restClient.post()
            .uri(tokenUrl)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(Map.class);

        if (response == null || !response.containsKey("access_token")) {
            throw new IllegalStateException("Could not obtain Keycloak admin token");
        }
        return (String) response.get("access_token");
    }
}
