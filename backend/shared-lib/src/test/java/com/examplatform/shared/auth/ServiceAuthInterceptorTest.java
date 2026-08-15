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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ServiceAuthInterceptor}.
 * Validates that the interceptor attaches a Bearer token to outgoing requests.
 */
@ExtendWith(MockitoExtension.class)
class ServiceAuthInterceptorTest {

    @Mock
    private ServiceAccountTokenProvider tokenProvider;

    @Mock
    private HttpRequest request;

    @Mock
    private ClientHttpRequestExecution execution;

    @Mock
    private ClientHttpResponse response;

    @InjectMocks
    private ServiceAuthInterceptor interceptor;

    @Test
    void intercept_addsAuthorizationHeader() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(tokenProvider.getServiceToken("platform-service")).thenReturn("service-jwt-token");
        byte[] body = new byte[0];
        when(execution.execute(request, body)).thenReturn(response);

        interceptor.intercept(request, body, execution);

        verify(tokenProvider).getServiceToken("platform-service");
        verify(execution).execute(request, body);
        assert headers.getFirst(HttpHeaders.AUTHORIZATION).equals("Bearer service-jwt-token");
    }
}
