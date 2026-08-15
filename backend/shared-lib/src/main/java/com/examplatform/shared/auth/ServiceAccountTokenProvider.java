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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that provides short-lived JWT tokens for inter-service communication
 * using the OAuth2 client_credentials grant via Keycloak.
 * <p>
 * Tokens are cached until near expiry (with a 30-second safety margin)
 * to avoid unnecessary token requests on every inter-service call.
 * <p>
 * This is part of the Zero Trust architecture: every inter-service request
 * is authenticated via service-account JWTs (no shared secrets or API keys).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceAccountTokenProvider {

    private final ServiceAccountTokenClient tokenClient;

    private final ConcurrentHashMap<String, CachedToken> tokenCache = new ConcurrentHashMap<>();

    /** Safety margin before token expiry to trigger refresh (30 seconds). */
    private static final long EXPIRY_MARGIN_SECONDS = 30;

    /**
     * Retrieves a valid service-account JWT for the specified service.
     * Returns a cached token if still valid, otherwise requests a new one
     * from Keycloak using the client_credentials grant.
     *
     * @param serviceId the service identifier (maps to a Keycloak client ID)
     * @return a valid JWT access token string
     */
    public String getServiceToken(String serviceId) {
        CachedToken cached = tokenCache.get(serviceId);
        if (cached != null && !cached.isExpired()) {
            return cached.accessToken();
        }

        log.debug("Requesting new service token for serviceId: {}", serviceId);
        TokenResponse response = tokenClient.requestToken(serviceId);
        CachedToken newToken = new CachedToken(
                response.getAccessToken(),
                Instant.now().plusSeconds(response.getExpiresIn() - EXPIRY_MARGIN_SECONDS)
        );
        tokenCache.put(serviceId, newToken);
        return newToken.accessToken();
    }

    /**
     * Internal record holding the cached token and its effective expiry.
     */
    private record CachedToken(String accessToken, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
