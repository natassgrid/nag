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

package com.examplatform.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * Global filter that strips dangerous headers and validates Content-Type.
 * Protects against X-Forwarded-Host abuse, header injection, and unexpected content types.
 */
@Component
public class RequestSanitizationFilter implements GlobalFilter, Ordered {

    /**
     * Headers that should be stripped from incoming requests to prevent abuse.
     */
    private static final Set<String> DANGEROUS_HEADERS = Set.of(
            "X-Forwarded-Host",
            "X-Forwarded-Server",
            "X-Original-URL",
            "X-Rewrite-URL",
            "X-HTTP-Method-Override",
            "X-Method-Override",
            "X-Override-Method"
    );

    /**
     * Allowed content types for request bodies.
     */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/json",
            "application/xml",
            "multipart/form-data",
            "application/x-www-form-urlencoded",
            "application/octet-stream",
            "text/plain"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Strip dangerous headers
        ServerHttpRequest.Builder requestBuilder = request.mutate();
        for (String dangerousHeader : DANGEROUS_HEADERS) {
            if (request.getHeaders().containsHeader(dangerousHeader)) {
                requestBuilder.headers(headers -> headers.remove(dangerousHeader));
            }
        }

        // Validate Content-Type for requests with body
        String method = request.getMethod().name();
        if (List.of("POST", "PUT", "PATCH").contains(method)) {
            MediaType contentType = request.getHeaders().getContentType();
            if (contentType != null && !isAllowedContentType(contentType)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                return exchange.getResponse().setComplete();
            }
        }

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(requestBuilder.build())
                .build();
        return chain.filter(mutatedExchange);
    }

    private boolean isAllowedContentType(MediaType contentType) {
        String baseType = contentType.getType() + "/" + contentType.getSubtype();
        return ALLOWED_CONTENT_TYPES.contains(baseType);
    }

    @Override
    public int getOrder() {
        // Execute after RequestIdFilter but before other filters
        return -90;
    }
}
