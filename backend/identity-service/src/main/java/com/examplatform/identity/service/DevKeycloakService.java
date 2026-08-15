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

package com.examplatform.identity.service;

import com.examplatform.identity.dto.AuthTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Dev-mode service that issues properly signed JWT tokens using a shared HMAC secret.
 * All downstream services validate these tokens using the same secret.
 * Active only when 'docker' or 'dev' profile is set.
 */
@Slf4j
@Service
@Primary
@Profile({"dev", "docker"})
public class DevKeycloakService extends KeycloakService {

    private final String jwtSecret;

    public DevKeycloakService(@Value("${app.jwt.secret:dev-jwt-secret-key-for-local-testing-minimum-32-chars}") String jwtSecret) {
        super(null);
        this.jwtSecret = jwtSecret;
    }

    @Override
    public AuthTokenResponse getTokens(String username, String password) {
        return getTokens(username, password, null);
    }

    @Override
    public AuthTokenResponse getTokens(String username, String password, String userId) {
        log.info("[DEV] Issuing signed dev token for user: {} (id: {})", username, userId);

        long now = System.currentTimeMillis() / 1000;
        long exp = now + 3600;
        String sub = userId != null ? userId : username;

        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url("{" +
                "\"sub\":\"" + sub + "\"," +
                "\"preferred_username\":\"" + username + "\"," +
                "\"iss\":\"exam-platform-dev\"," +
                "\"aud\":\"exam-backend\"," +
                "\"iat\":" + now + "," +
                "\"exp\":" + exp + "," +
                "\"realm_access\":{\"roles\":[\"SUPER_ADMIN\",\"CANDIDATE\",\"QUESTION_AUTHOR\",\"REVIEWER\",\"EXAM_CONTROLLER\"]}" +
                "}");

        String signingInput = header + "." + payload;
        String signature = hmacSha256(signingInput);
        String accessToken = signingInput + "." + signature;

        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(UUID.randomUUID().toString())
                .expiresIn(3600L)
                .tokenType("Bearer")
                .build();
    }

    @Override
    public void activateUser(String keycloakUserId) {
        log.info("[DEV] Skipping Keycloak user activation for: {}", keycloakUserId);
    }

    private String base64Url(String input) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign JWT", e);
        }
    }
}
