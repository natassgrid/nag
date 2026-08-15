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
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Reactive security configuration for the API Gateway.
 * Uses OAuth2 Resource Server with JWT validation via Keycloak JWKS endpoint.
 * Permits actuator endpoints and requires authentication for all API routes.
 *
 * <p><strong>mTLS via Istio (production):</strong> In production Kubernetes deployments,
 * Istio service mesh provides mutual TLS (mTLS) between all services via
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
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                .pathMatchers("/api/v1/identity/register", "/api/v1/identity/auth/**", "/api/v1/identity/otp/**").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})
                .bearerTokenConverter(exchange -> {
                    String path = exchange.getRequest().getPath().value();
                    if (path.startsWith("/api/v1/identity/auth/") ||
                        path.startsWith("/api/v1/identity/register") ||
                        path.startsWith("/api/v1/identity/otp/")) {
                        return reactor.core.publisher.Mono.empty();
                    }
                    return new org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter()
                            .convert(exchange);
                })
            );
        return http.build();
    }
}
