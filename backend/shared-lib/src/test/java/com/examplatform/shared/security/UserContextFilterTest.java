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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserContextFilter Unit Tests")
class UserContextFilterTest {

    private UserContextFilter filter;

    @BeforeEach
    void setUp() {
        filter = new UserContextFilter();
        SecurityContextHolder.clearContext();
        UserContext.clear();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        UserContext.clear();
        TenantContext.clear();
    }

    @Test
    @DisplayName("Populates UserContext and TenantContext from authenticated JWT claims")
    void populatesFromJwt() throws ServletException, IOException {
        UUID userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("mock-jwt-token")
                .header("alg", "HS256")
                .subject(userId.toString())
                .claim("email", "evaluator@exam.gov.in")
                .claim("tenant_id", "upsc-exam")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                jwt,
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_EVALUATOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean filterChainExecuted = new AtomicBoolean(false);
        FilterChain filterChain = (req, res) -> {
            filterChainExecuted.set(true);
            assertThat(UserContext.isAuthenticated()).isTrue();
            assertThat(UserContext.getUserId()).isEqualTo(userId);
            assertThat(UserContext.getEmail()).isEqualTo("evaluator@exam.gov.in");
            assertThat(UserContext.getTenantId()).isEqualTo("upsc-exam");
            assertThat(UserContext.hasRole("EVALUATOR")).isTrue();
            assertThat(TenantContext.getTenantId()).isEqualTo("upsc-exam");
        };

        filter.doFilter(request, response, filterChain);

        assertThat(filterChainExecuted.get()).isTrue();
        // Verifies cleanup in finally block
        assertThat(UserContext.get()).isNull();
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Extracts tenant from request header when JWT tenant claim is absent")
    void fallbackToHeaderTenant() throws ServletException, IOException {
        UUID userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("mock-jwt-token")
                .header("alg", "HS256")
                .subject(userId.toString())
                .claim("preferred_username", "candidate@exam.gov.in")
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                jwt,
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "nta-exam");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (req, res) -> {
            assertThat(UserContext.getUserId()).isEqualTo(userId);
            assertThat(UserContext.getEmail()).isEqualTo("candidate@exam.gov.in");
            assertThat(UserContext.getTenantId()).isEqualTo("nta-exam");
            assertThat(TenantContext.getTenantId()).isEqualTo("nta-exam");
        };

        filter.doFilter(request, response, filterChain);
    }
}
