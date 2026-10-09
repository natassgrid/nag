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

package com.examplatform.gateway.config;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Shared security helper and route definitions for API Gateway security configurations.
 */
public final class GatewaySecurityConfigSupport {

    private GatewaySecurityConfigSupport() {
    }

    public static final String[] PUBLIC_AUTH_PATH_PATTERNS = {
            "/api/v1/identity/register",
            "/api/v1/identity/auth/**",
            "/api/v1/identity/otp/**",
            "/api/v1/identity/verify-otp",
            "/api/v1/identity/verify/**",
            "/api/v1/identity/resend/**",
            "/api/v1/identity/verification-status",
            "/api/v1/identity/admin/invite/**",
            "/api/v1/examinations/public/**",
            "/api/v1/papers/public/**",
            "/api/v1/sessions/paper/**",
            "/api/v1/delivery/paper/**"
    };

    public static final String[] PUBLIC_GENERAL_PATH_PATTERNS = {
            "/api/v1/geo/**",
            "/api/v1/public/**"
    };

    public static final String[] ACTUATOR_PATH_PATTERNS = {
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus"
    };

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/v1/identity/auth/",
            "/api/v1/identity/register",
            "/api/v1/identity/otp/",
            "/api/v1/identity/verify-otp",
            "/api/v1/identity/verify/",
            "/api/v1/identity/resend/",
            "/api/v1/identity/verification-status",
            "/api/v1/identity/admin/invite/",
            "/api/v1/examinations/public/",
            "/api/v1/papers/public/",
            "/api/v1/sessions/paper/",
            "/api/v1/delivery/paper/",
            "/api/v1/geo/",
            "/api/v1/public/"
    );

    /**
     * Creates the token converter that skips public endpoints and extracts tokens from headers or query parameters.
     */
    public static ServerAuthenticationConverter createTokenConverter() {
        return exchange -> {
            // Don't extract Bearer token for public auth endpoints
            String path = exchange.getRequest().getPath().value();
            if (isPublicPath(exchange.getRequest().getMethod(), path)) {
                return Mono.empty();
            }

            // 1. Check Authorization header first
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
                String token = authHeader.substring(7).trim();
                if (!token.isBlank()) {
                    return Mono.just(new BearerTokenAuthenticationToken(token));
                }
            }

            // 2. Check query parameters ?token= or ?access_token= (for SSE streams)
            String tokenParam = exchange.getRequest().getQueryParams().getFirst("token");
            if (tokenParam == null || tokenParam.isBlank()) {
                tokenParam = exchange.getRequest().getQueryParams().getFirst("access_token");
            }
            if (tokenParam != null && !tokenParam.isBlank()) {
                return Mono.just(new BearerTokenAuthenticationToken(tokenParam));
            }

            return Mono.empty();
        };
    }

    /**
     * Checks if the given path is a public endpoint that doesn't require authentication extraction.
     */
    public static boolean isPublicPath(HttpMethod method, String path) {
        for (String prefix : PUBLIC_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return method == HttpMethod.GET
                && path.startsWith("/api/v1/assets/")
                && (path.endsWith("/download") || path.endsWith("/url"));
    }

    /**
     * Configures common CSRF and exchange authorization rules.
     */
    public static ServerHttpSecurity configureCommonSecurity(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(ACTUATOR_PATH_PATTERNS).permitAll()
                        .pathMatchers(PUBLIC_AUTH_PATH_PATTERNS).permitAll()
                        .pathMatchers(PUBLIC_GENERAL_PATH_PATTERNS).permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/assets/*/download", "/api/v1/assets/*/url").permitAll()
                        .anyExchange().authenticated()
                );
    }
}
