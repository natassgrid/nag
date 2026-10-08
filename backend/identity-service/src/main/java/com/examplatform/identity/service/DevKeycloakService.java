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

import com.examplatform.identity.domain.UserAccount;
import com.examplatform.identity.dto.AuthTokenResponse;
import com.examplatform.identity.exception.AuthenticationException;
import com.examplatform.identity.repository.UserAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    private final UserAccountRepository userAccountRepository;
    private final Map<String, DevSessionData> devRefreshTokenStore = new ConcurrentHashMap<>();

    private record DevSessionData(String username, String userId, String preferredLanguage) {}

    @Autowired
    public DevKeycloakService(
            @Value("${app.jwt.secret:dev-jwt-secret-key-for-local-testing-minimum-32-chars}") String jwtSecret,
            UserAccountRepository userAccountRepository) {
        super(null);
        this.jwtSecret = jwtSecret;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public AuthTokenResponse getTokens(String username, String password) {
        return getTokens(username, password, null);
    }

    @Override
    public AuthTokenResponse getTokens(String username, String password, String userId) {
        String lang = "en";
        if (userAccountRepository != null && userId != null) {
            try {
                lang = userAccountRepository.findById(UUID.fromString(userId))
                        .map(UserAccount::getPreferredLanguage)
                        .filter(l -> l != null && !l.isBlank())
                        .orElse("en");
            } catch (Exception ignored) {
            }
        }
        return getTokens(username, password, userId, lang);
    }

    @Override
    public AuthTokenResponse getTokens(String username, String password, String userId, String preferredLanguage) {
        log.info("[DEV] Issuing signed dev token for user: {} (id: {}, preferredLanguage: {})", username, userId, preferredLanguage);

        long now = System.currentTimeMillis() / 1000;
        long exp = now + 3600;
        String sub = userId != null ? userId : username;
        String lang = (preferredLanguage != null && !preferredLanguage.isBlank()) ? preferredLanguage : "en";

        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url("{" +
                "\"sub\":\"" + sub + "\"," +
                "\"preferred_username\":\"" + username + "\"," +
                "\"preferred_language\":\"" + lang + "\"," +
                "\"tenant_id\":\"default\"," +
                "\"name\":\"" + username + "\"," +
                "\"iss\":\"exam-platform-dev\"," +
                "\"aud\":\"exam-backend\"," +
                "\"iat\":" + now + "," +
                "\"exp\":" + exp + "," +
                "\"realm_access\":{\"roles\":[\"SUPER_ADMIN\",\"CANDIDATE\",\"QUESTION_AUTHOR\",\"REVIEWER\",\"EXAM_CONTROLLER\"]}" +
                "}");

        String signingInput = header + "." + payload;
        String signature = hmacSha256(signingInput);
        String accessToken = signingInput + "." + signature;
        String refreshToken = "dev-rt-" + UUID.randomUUID();

        devRefreshTokenStore.put(refreshToken, new DevSessionData(username, sub, lang));

        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600L)
                .tokenType("Bearer")
                .userId(sub)
                .build();
    }

    @Override
    public AuthTokenResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthenticationException("Refresh token is required");
        }

        DevSessionData sessionData = devRefreshTokenStore.remove(refreshToken);
        if (sessionData == null) {
            log.warn("[DEV] Invalid or expired refresh token: {}", refreshToken);
            throw new AuthenticationException("Invalid or expired refresh token");
        }

        log.info("[DEV] Refreshing token for user: {} (id: {}, lang: {})",
                sessionData.username(), sessionData.userId(), sessionData.preferredLanguage());
        return getTokens(sessionData.username(), null, sessionData.userId(), sessionData.preferredLanguage());
    }

    @Override
    public void activateUser(String keycloakUserId) {
        log.info("[DEV] Skipping Keycloak user activation for: {}", keycloakUserId);
    }

    @Override
    public void changePassword(String username, String currentPassword, String newPassword, String keycloakUserId) {
        log.info("[DEV] Password changed for user: {}", username);
    }

    @Override
    public void revokeUserSessions(String keycloakUserId) {
        log.info("[DEV] User sessions revoked for: {}", keycloakUserId);
        devRefreshTokenStore.entrySet().removeIf(entry -> entry.getValue().userId().equals(keycloakUserId));
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
