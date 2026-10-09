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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.\n */

package com.examplatform.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;

/**
 * Standard security configuration for API Gateway.
 * Validates JWT tokens against Keycloak (production mode).
 * Active when neither 'dev' nor 'docker' profile is active.
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
        ServerAuthenticationConverter tokenConverter = GatewaySecurityConfigSupport.createTokenConverter();

        return GatewaySecurityConfigSupport.configureCommonSecurity(http)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {})
                        .bearerTokenConverter(tokenConverter)
                )
                .build();
    }
}
