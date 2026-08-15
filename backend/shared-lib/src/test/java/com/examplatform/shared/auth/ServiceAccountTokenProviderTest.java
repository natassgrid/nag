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

package com.examplatform.shared.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ServiceAccountTokenProvider}.
 * Validates token caching behavior and Keycloak client_credentials grant flow.
 */
@ExtendWith(MockitoExtension.class)
class ServiceAccountTokenProviderTest {

    @Mock
    private ServiceAccountTokenClient tokenClient;

    private ServiceAccountTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new ServiceAccountTokenProvider(tokenClient);
    }

    @Test
    void getServiceToken_requestsNewTokenWhenNoCacheExists() {
        TokenResponse response = TokenResponse.builder()
                .accessToken("jwt-token-123")
                .expiresIn(300)
                .tokenType("Bearer")
                .build();
        when(tokenClient.requestToken("delivery-service")).thenReturn(response);

        String token = tokenProvider.getServiceToken("delivery-service");

        assertThat(token).isEqualTo("jwt-token-123");
        verify(tokenClient).requestToken("delivery-service");
    }

    @Test
    void getServiceToken_returnsCachedTokenOnSubsequentCalls() {
        TokenResponse response = TokenResponse.builder()
                .accessToken("jwt-token-456")
                .expiresIn(300)
                .tokenType("Bearer")
                .build();
        when(tokenClient.requestToken("identity-service")).thenReturn(response);

        // First call — fetches from Keycloak
        String firstToken = tokenProvider.getServiceToken("identity-service");
        // Second call — should use cache
        String secondToken = tokenProvider.getServiceToken("identity-service");

        assertThat(firstToken).isEqualTo("jwt-token-456");
        assertThat(secondToken).isEqualTo("jwt-token-456");
        // Token client should only be called once (cached on second call)
        verify(tokenClient, times(1)).requestToken("identity-service");
    }

    @Test
    void getServiceToken_refreshesExpiredToken() {
        // Token with very short expiry (expires almost immediately considering 30s margin)
        TokenResponse expiredResponse = TokenResponse.builder()
                .accessToken("expired-token")
                .expiresIn(1) // 1 second - with 30s margin, already expired
                .tokenType("Bearer")
                .build();
        TokenResponse freshResponse = TokenResponse.builder()
                .accessToken("fresh-token")
                .expiresIn(300)
                .tokenType("Bearer")
                .build();

        when(tokenClient.requestToken("exam-service"))
                .thenReturn(expiredResponse)
                .thenReturn(freshResponse);

        // First call — gets "expired" token (which is immediately stale)
        String firstToken = tokenProvider.getServiceToken("exam-service");
        assertThat(firstToken).isEqualTo("expired-token");

        // Second call — token is expired (expiresIn=1, margin=30), should refresh
        String secondToken = tokenProvider.getServiceToken("exam-service");
        assertThat(secondToken).isEqualTo("fresh-token");

        verify(tokenClient, times(2)).requestToken("exam-service");
    }

    @Test
    void getServiceToken_cachesSeparateTokensPerService() {
        TokenResponse deliveryToken = TokenResponse.builder()
                .accessToken("delivery-jwt")
                .expiresIn(300)
                .tokenType("Bearer")
                .build();
        TokenResponse identityToken = TokenResponse.builder()
                .accessToken("identity-jwt")
                .expiresIn(300)
                .tokenType("Bearer")
                .build();

        when(tokenClient.requestToken("delivery-service")).thenReturn(deliveryToken);
        when(tokenClient.requestToken("identity-service")).thenReturn(identityToken);

        String token1 = tokenProvider.getServiceToken("delivery-service");
        String token2 = tokenProvider.getServiceToken("identity-service");

        assertThat(token1).isEqualTo("delivery-jwt");
        assertThat(token2).isEqualTo("identity-jwt");
    }
}
