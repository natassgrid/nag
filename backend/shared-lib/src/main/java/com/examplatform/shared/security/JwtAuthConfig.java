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

package com.examplatform.shared.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Shared JWT authentication converter that extracts roles and authorities from
 * Keycloak realm_access.roles, resource_access, top-level roles, and authorities claims.
 */
@AutoConfiguration
@ConditionalOnClass(JwtAuthenticationConverter.class)
public class JwtAuthConfig {

    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<GrantedAuthority> authorities = new HashSet<>();

            // 1. Keycloak realm_access.roles: {"realm_access": {"roles": ["..."]}}
            Object realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof Map<?, ?> realmMap) {
                Object roles = realmMap.get("roles");
                if (roles instanceof Collection<?> roleList) {
                    for (Object role : roleList) {
                        if (role != null) {
                            String roleStr = role.toString().trim();
                            if (!roleStr.isBlank()) {
                                authorities.add(new SimpleGrantedAuthority(
                                        roleStr.startsWith("ROLE_") ? roleStr : "ROLE_" + roleStr));
                            }
                        }
                    }
                }
            }

            // 2. Direct top-level "roles" claim: ["CANDIDATE", "EVALUATOR"]
            Object rolesClaim = jwt.getClaim("roles");
            if (rolesClaim instanceof Collection<?> roleList) {
                for (Object role : roleList) {
                    if (role != null) {
                        String roleStr = role.toString().trim();
                        if (!roleStr.isBlank()) {
                            authorities.add(new SimpleGrantedAuthority(
                                    roleStr.startsWith("ROLE_") ? roleStr : "ROLE_" + roleStr));
                        }
                    }
                }
            } else if (rolesClaim instanceof String roleStr && !roleStr.isBlank()) {
                authorities.add(new SimpleGrantedAuthority(
                        roleStr.startsWith("ROLE_") ? roleStr : "ROLE_" + roleStr));
            }

            // 3. Direct top-level "role" claim
            Object roleClaim = jwt.getClaim("role");
            if (roleClaim instanceof String singleRole && !singleRole.isBlank()) {
                authorities.add(new SimpleGrantedAuthority(
                        singleRole.startsWith("ROLE_") ? singleRole : "ROLE_" + singleRole));
            }

            // 4. Direct "authorities" claim
            Object authClaim = jwt.getClaim("authorities");
            if (authClaim instanceof Collection<?> authList) {
                for (Object auth : authList) {
                    if (auth != null) {
                        String authStr = auth.toString().trim();
                        if (!authStr.isBlank()) {
                            authorities.add(new SimpleGrantedAuthority(authStr));
                        }
                    }
                }
            }

            // 5. Direct "scp" or "scope" claim (preserve standard OAuth2 scopes)
            Object scopeClaim = jwt.getClaim("scope");
            if (scopeClaim == null) {
                scopeClaim = jwt.getClaim("scp");
            }
            if (scopeClaim instanceof String scopes) {
                for (String sc : scopes.split(" ")) {
                    if (!sc.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority("SCOPE_" + sc));
                    }
                }
            } else if (scopeClaim instanceof Collection<?> scopeList) {
                for (Object sc : scopeList) {
                    if (sc != null) {
                        String scStr = sc.toString().trim();
                        if (!scStr.isBlank()) {
                            authorities.add(new SimpleGrantedAuthority("SCOPE_" + scStr));
                        }
                    }
                }
            }

            return authorities;
        });
        return converter;
    }
}
