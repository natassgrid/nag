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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import reactor.core.publisher.Mono;

/**
 * Spring Cloud Gateway security configuration.
 *
 * <p>All external traffic must pass JWT validation at the gateway level.
 * Public endpoints (login, registration, actuator health) are explicitly permitted.
 * All other routes require a valid Bearer token issued by Keycloak.</p>
 *
 * <p>In production (Kubernetes/Istio), inter-service mTLS is enforced via Istio
 * PeerAuthentication policies. This ensures transport-layer identity verification
 * using SPIFFE identities, complementing the application-layer JWT validation
 * performed here. See {@link ZeroTrustConfig} for the full Zero Trust architecture.</p>
 */
@Configuration
@EnableWebFluxSecurity
@Profile("!dev & !docker")
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        ServerAuthenticationConverter tokenConverter = exchange -> {
            String path = exchange.getRequest().getPath().value();
            if (path.startsWith("/api/v1/identity/auth/") ||
                path.startsWith("/api/v1/identity/register") ||
                path.startsWith("/api/v1/identity/otp/") ||
                path.startsWith("/api/v1/identity/verify-otp") ||
                path.startsWith("/api/v1/identity/verify/") ||
                path.startsWith("/api/v1/identity/resend/") ||
                path.startsWith("/api/v1/identity/verification-status") ||
                path.startsWith("/api/v1/identity/admin/invite/") ||
                path.startsWith("/api/v1/examinations/public/") ||
                path.startsWith("/api/v1/geo/") ||
                path.startsWith("/api/v1/public/") ||
                (exchange.getRequest().getMethod() == HttpMethod.GET && path.startsWith("/api/v1/assets/") && (path.endsWith("/download") || path.endsWith("/url")))) {
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

        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                .pathMatchers(
                    "/api/v1/identity/register",
                    "/api/v1/identity/auth/**",
                    "/api/v1/identity/otp/**",
                    "/api/v1/identity/verify-otp",
                    "/api/v1/identity/verify/**",
                    "/api/v1/identity/resend/**",
                    "/api/v1/identity/verification-status",
                    "/api/v1/identity/admin/invite/**",
                    "/api/v1/examinations/public/**"
                ).permitAll()
                .pathMatchers("/api/v1/geo/**", "/api/v1/public/**").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/v1/assets/*/download", "/api/v1/assets/*/url", "/api/v1/assets/**/download", "/api/v1/assets/**/url").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})
                .bearerTokenConverter(tokenConverter)
            );
        return http.build();
    }
}
