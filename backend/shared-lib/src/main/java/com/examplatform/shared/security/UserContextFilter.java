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

import com.examplatform.shared.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Filter that automatically extracts identity and tenant claims from the active
 * Spring Security authentication and populates {@link UserContext} and {@link TenantContext}.
 *
 * Guarantees thread cleanup in a {@code finally} block upon completion of the request.
 */
public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                UUID userId = null;
                String email = null;
                String tenantId = null;

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
                } else if (auth.getName() != null) {
                    try {
                        userId = UUID.fromString(auth.getName());
                    } catch (IllegalArgumentException ignored) {
                    }
                    email = auth.getName();
                }

                if (tenantId == null || tenantId.isBlank()) {
                    tenantId = request.getHeader("X-Tenant-Id");
                }
                if (tenantId != null && !tenantId.isBlank()) {
                    TenantContext.set(tenantId);
                }

                Set<String> roles = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toUnmodifiableSet());

                UserContext.set(new UserContext.CurrentUser(
                        userId,
                        email,
                        tenantId != null && !tenantId.isBlank() ? tenantId : TenantContext.get(),
                        roles
                ));
            } else {
                String headerTenant = request.getHeader("X-Tenant-Id");
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
