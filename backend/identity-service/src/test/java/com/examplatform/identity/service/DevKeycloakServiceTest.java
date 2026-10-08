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
import com.examplatform.identity.repository.UserAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DevKeycloakService — Preferred Language & JWT Claims (Issue #277)")
class DevKeycloakServiceTest {

    private static final String SECRET = "01234567890123456789012345678901"; // 32 chars
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private UserAccountRepository userAccountRepository;

    private DevKeycloakService devKeycloakService;

    @BeforeEach
    void setUp() {
        devKeycloakService = new DevKeycloakService(SECRET, userAccountRepository);
    }

    private JsonNode extractPayload(String token) throws Exception {
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        return MAPPER.readTree(payloadJson);
    }

    @Test
    @DisplayName("Should inject explicit preferred_language claim into JWT")
    void shouldInjectExplicitPreferredLanguage() throws Exception {
        AuthTokenResponse tokens = devKeycloakService.getTokens("candidate_01", "pass", "user-uuid-1", "hi");

        assertThat(tokens).isNotNull();
        JsonNode payload = extractPayload(tokens.getAccessToken());

        assertThat(payload.get("sub").asText()).isEqualTo("user-uuid-1");
        assertThat(payload.get("preferred_username").asText()).isEqualTo("candidate_01");
        assertThat(payload.get("preferred_language").asText()).isEqualTo("hi");
        assertThat(payload.get("tenant_id").asText()).isEqualTo("default");
    }

    @Test
    @DisplayName("Should resolve preferred_language from UserAccountRepository when using 3-arg getTokens")
    void shouldResolveLanguageFromRepository() throws Exception {
        UUID userId = UUID.randomUUID();
        UserAccount account = UserAccount.builder()
                .preferredLanguage("ta")
                .build();
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(account));

        AuthTokenResponse tokens = devKeycloakService.getTokens("tamil_user", "pass", userId.toString());

        JsonNode payload = extractPayload(tokens.getAccessToken());
        assertThat(payload.get("preferred_language").asText()).isEqualTo("ta");
    }

    @Test
    @DisplayName("Should preserve preferred_language across token refresh")
    void shouldPreservePreferredLanguageAcrossRefresh() throws Exception {
        AuthTokenResponse initialTokens = devKeycloakService.getTokens("user_mr", "pass", "uuid-mr", "mr");
        JsonNode initialPayload = extractPayload(initialTokens.getAccessToken());
        assertThat(initialPayload.get("preferred_language").asText()).isEqualTo("mr");

        AuthTokenResponse refreshedTokens = devKeycloakService.refreshToken(initialTokens.getRefreshToken());
        JsonNode refreshedPayload = extractPayload(refreshedTokens.getAccessToken());
        assertThat(refreshedPayload.get("preferred_language").asText()).isEqualTo("mr");
        assertThat(refreshedPayload.get("sub").asText()).isEqualTo("uuid-mr");
    }

    @Test
    @DisplayName("Should default preferred_language to 'en' when null or empty")
    void shouldDefaultToEnglish() throws Exception {
        AuthTokenResponse tokens = devKeycloakService.getTokens("guest_user", "pass", "uuid-guest", "");

        JsonNode payload = extractPayload(tokens.getAccessToken());
        assertThat(payload.get("preferred_language").asText()).isEqualTo("en");
    }
}
