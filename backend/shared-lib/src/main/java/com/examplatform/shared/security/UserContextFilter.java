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

package com.examplatform.shared.security;

import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * Filter that automatically extracts identity and tenant claims from the active
 * Spring Security authentication and populates {@link UserContext} and {@link TenantContext}.
 *
 * Guarantees thread cleanup in a {@code finally} block upon completion of the request.
 */
public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                UUID userId = null;
                String email = null;
                String tenantId = null;
                String preferredLanguage = null;

                if (auth.getPrincipal() instanceof Jwt jwt) {
                    if (jwt.getSubject() != null) {
                        try {
                            userId = UUID.fromString(jwt.getSubject());
                        } catch (IllegalArgumentException ignored) {
                        }
                    }

                    email = jwt.getClaimAsString("email");
                    if (email == null || email.isBlank()) {
                        email = jwt.getClaimAsString("preferred_username");
                    }

                    tenantId = jwt.getClaimAsString("tenant_id");
                    preferredLanguage = jwt.getClaimAsString("preferred_language");
                } else if (auth.getName() != null) {
                    try {
                        userId = UUID.fromString(auth.getName());
                    } catch (IllegalArgumentException ignored) {
                    }
                    email = auth.getName();
                }

                if (tenantId == null || tenantId.isBlank()) {
                    tenantId = request.getHeader(SharedConstants.HEADER_TENANT_ID);
                }
                if (tenantId != null && !tenantId.isBlank()) {
                    TenantContext.set(tenantId);
                }

                Set<String> roles = new HashSet<>();
                if (auth.getAuthorities() != null) {
                    for (GrantedAuthority ga : auth.getAuthorities()) {
                        if (ga != null && ga.getAuthority() != null) {
                            String authStr = ga.getAuthority();
                            roles.add(authStr);
                            roles.add(authStr.replaceFirst("^ROLE_", ""));
                            roles.add("ROLE_" + authStr.replaceFirst("^ROLE_", ""));
                        }
                    }
                }

                if (auth.getPrincipal() instanceof Jwt jwt) {
                    Object realmAccess = jwt.getClaim("realm_access");
                    if (realmAccess instanceof Map<?, ?> realmMap) {
                        Object r = realmMap.get("roles");
                        if (r instanceof Collection<?> roleList) {
                            for (Object role : roleList) {
                                if (role != null && !role.toString().isBlank()) {
                                    roles.add(role.toString());
                                    roles.add("ROLE_" + role.toString().replaceFirst("^ROLE_", ""));
                                }
                            }
                        }
                    }
                    Object rolesClaim = jwt.getClaim("roles");
                    if (rolesClaim instanceof Collection<?> roleList) {
                        for (Object role : roleList) {
                            if (role != null && !role.toString().isBlank()) {
                                roles.add(role.toString());
                                roles.add("ROLE_" + role.toString().replaceFirst("^ROLE_", ""));
                            }
                        }
                    }
                }

                UserContext.set(new UserContext.CurrentUser(
                        userId,
                        email,
                        tenantId != null && !tenantId.isBlank() ? tenantId : TenantContext.get(),
                        Collections.unmodifiableSet(roles),
                        preferredLanguage
                ));
            } else {
                String headerTenant = request.getHeader(SharedConstants.HEADER_TENANT_ID);
                if (headerTenant != null && !headerTenant.isBlank()) {
                    TenantContext.set(headerTenant);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
            TenantContext.clear();
        }
    }
}
