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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Spring RestClient/RestTemplate interceptor that adds a service-account JWT
 * to outgoing inter-service HTTP requests.
 * <p>
 * This interceptor is part of the Zero Trust inter-service authentication pattern:
 * all downstream service calls are authenticated via short-lived service-account JWTs
 * obtained from Keycloak (no shared secrets, no API keys).
 * <p>
 * Usage: Register this interceptor with any RestClient or RestTemplate used for
 * inter-service communication.
 */
@Component
@RequiredArgsConstructor
public class ServiceAuthInterceptor implements ClientHttpRequestInterceptor {

    private final ServiceAccountTokenProvider tokenProvider;

    /** The default service ID used when no specific service is configured. */
    private static final String DEFAULT_SERVICE_ID = "platform-service";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        String token = tokenProvider.getServiceToken(DEFAULT_SERVICE_ID);
        request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return execution.execute(request, body);
    }
}
