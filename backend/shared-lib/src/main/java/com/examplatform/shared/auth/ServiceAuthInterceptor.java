/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Spring RestClient/RestTemplate interceptor that automatically attaches
 * authorization tokens (current request JWT or service account) and propagates
 * the active {@link TenantContext} to outgoing inter-service HTTP requests.
 * <p>
 * This interceptor is part of the Zero Trust inter-service authentication pattern:
 * downstream calls propagate the caller identity and tenancy or authenticate
 * via short-lived service-account JWTs.
 */
@Component
@RequiredArgsConstructor
public class ServiceAuthInterceptor implements ClientHttpRequestInterceptor {

    private final ServiceAccountTokenProvider tokenProvider;

    /** The default service ID used when no specific service is configured. */
    private static final String DEFAULT_SERVICE_ID = "platform-service";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte @NonNull [] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        if (request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION) == null) {
            String token = ClientAuthTokenResolver.resolveAuthToken(DEFAULT_SERVICE_ID, tokenProvider, null);
            if (token != null && !token.isBlank()) {
                request.getHeaders().set(HttpHeaders.AUTHORIZATION, token);
            }
        }
        if (request.getHeaders().getFirst(SharedConstants.HEADER_TENANT_ID) == null) {
            String tenantId = TenantContext.getTenantId();
            if (tenantId != null && !tenantId.isBlank()) {
                request.getHeaders().set(SharedConstants.HEADER_TENANT_ID, tenantId);
            }
        }
        return execution.execute(request, body);
    }
}
