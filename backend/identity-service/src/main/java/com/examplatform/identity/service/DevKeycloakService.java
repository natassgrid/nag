package com.examplatform.identity.service;

import com.examplatform.identity.dto.AuthTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.UUID;

/**
 * Dev-mode stub that bypasses Keycloak and issues dummy JWT tokens.
 * Active only when 'docker' or 'dev' profile is set.
 * Accepts any password — DO NOT use in production.
 */
@Slf4j
@Service
@Primary
@Profile({"dev", "docker"})
public class DevKeycloakService extends KeycloakService {

    public DevKeycloakService() {
        super(null);
    }

    @Override
    public AuthTokenResponse getTokens(String username, String password) {
        log.info("[DEV] Issuing dev token for user: {}", username);

        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"" + UUID.randomUUID() + "\","
                        + "\"preferred_username\":\"" + username + "\","
                        + "\"iss\":\"dev-issuer\","
                        + "\"iat\":" + System.currentTimeMillis() / 1000 + ","
                        + "\"exp\":" + (System.currentTimeMillis() / 1000 + 3600) + "}")
                        .getBytes());
        String devToken = header + "." + payload + ".";

        return AuthTokenResponse.builder()
                .accessToken(devToken)
                .refreshToken(UUID.randomUUID().toString())
                .expiresIn(3600L)
                .tokenType("Bearer")
                .build();
    }

    @Override
    public void activateUser(String keycloakUserId) {
        log.info("[DEV] Skipping Keycloak user activation for: {}", keycloakUserId);
    }
}
