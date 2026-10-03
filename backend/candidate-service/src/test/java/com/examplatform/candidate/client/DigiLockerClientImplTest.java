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

package com.examplatform.candidate.client;

import com.examplatform.candidate.dto.DigiLockerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DigiLockerClientImpl Unit Tests (candidate-service)")
class DigiLockerClientImplTest {

    private DigiLockerClientImpl client;

    @BeforeEach
    void setUp() {
        client = new DigiLockerClientImpl();
        ReflectionTestUtils.setField(client, "digiLockerBaseUrl", "http://localhost:8099/digilocker");
        ReflectionTestUtils.setField(client, "clientId", "nag-exam-platform");
        ReflectionTestUtils.setField(client, "clientSecret", "mock-secret");
        ReflectionTestUtils.setField(client, "defaultRedirectUri", "http://localhost:8082/api/v1/candidates/digilocker/callback");
    }

    @Test
    @DisplayName("getAuthorizationUrl generates properly formatted OAuth2 URL with state and params")
    void getAuthorizationUrl_generatesValidUrl() {
        String authUrl = client.getAuthorizationUrl("mock_state_123", "http://localhost:8082/custom/callback");

        assertThat(authUrl).contains("response_type=code");
        assertThat(authUrl).contains("client_id=nag-exam-platform");
        assertThat(authUrl).contains("state=mock_state_123");
        assertThat(authUrl).contains("redirect_uri=http://localhost:8082/custom/callback");
        assertThat(authUrl).contains("/oauth/authorize");
    }

    @Test
    @DisplayName("exchangeCodeForToken returns fallback token when endpoint unreachable")
    void exchangeCodeForToken_unreachableEndpoint_returnsFallback() {
        ReflectionTestUtils.setField(client, "digiLockerTokenUrl", "http://localhost:19999/unreachable/token");

        Map<String, Object> tokenResponse = client.exchangeCodeForToken("test_code_123", null);

        assertThat(tokenResponse).isNotNull();
        assertThat(tokenResponse).containsKey("access_token");
        assertThat(tokenResponse.get("access_token").toString()).contains("test_code_123");
    }

    @Test
    @DisplayName("getUserInfo returns demographic fallback when endpoint unreachable")
    void getUserInfo_unreachableEndpoint_returnsFallback() {
        ReflectionTestUtils.setField(client, "digiLockerUserinfoUrl", "http://localhost:19999/unreachable/userinfo");

        Map<String, Object> userInfo = client.getUserInfo("mock_token");

        assertThat(userInfo).isNotNull();
        assertThat(userInfo).containsKey("name");
        assertThat(userInfo).containsKey("dob");
    }

    @Test
    @DisplayName("fetchDocument returns fallback response when endpoint unreachable")
    void fetchDocument_unreachableEndpoint_returnsFallback() {
        ReflectionTestUtils.setField(client, "digiLockerApiUrl", "http://localhost:19999/unreachable/verify");

        DigiLockerResponse response = client.fetchDocument("mock_token", "AADHAAR");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getDocumentData()).isNotEmpty();
    }
}
