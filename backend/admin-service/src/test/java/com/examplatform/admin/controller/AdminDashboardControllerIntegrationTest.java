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

package com.examplatform.admin.controller;

import com.examplatform.admin.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AdminDashboardController REST Endpoints E2E Tests (MockMvc)")
class AdminDashboardControllerIntegrationTest extends AbstractIntegrationTest {

    @Nested
    @DisplayName("GET /api/v1/admin/dashboard/summary")
    class GetDashboardSummaryEndpoint {

        @Test
        @DisplayName("+ve: SUPER_ADMIN retrieves aggregated dashboard metrics - returns 200 OK")
        void superAdminCanGetSummary() throws Exception {
            mockMvc.perform(get("/api/v1/admin/dashboard/summary")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.subject("11111111-1111-1111-1111-111111111111").claim("tenant_id", "default")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tenantId").value("default"))
                    .andExpect(jsonPath("$.kpis").exists())
                    .andExpect(jsonPath("$.kpis.totalQuestions").isNumber())
                    .andExpect(jsonPath("$.examBreakdown").exists())
                    .andExpect(jsonPath("$.questionBreakdown").exists())
                    .andExpect(jsonPath("$.systemServices").isArray())
                    .andExpect(jsonPath("$.recentAuditEvents").isArray());
        }

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER retrieves aggregated dashboard metrics - returns 200 OK")
        void examControllerCanGetSummary() throws Exception {
            mockMvc.perform(get("/api/v1/admin/dashboard/summary")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject("22222222-2222-2222-2222-222222222222").claim("tenant_id", "nta-exam")))
                            .param("tenantId", "nta-exam")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tenantId").value("nta-exam"))
                    .andExpect(jsonPath("$.kpis").exists());
        }

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR retrieves dashboard metrics - returns 200 OK")
        void questionAuthorCanGetSummary() throws Exception {
            mockMvc.perform(get("/api/v1/admin/dashboard/summary")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject("33333333-3333-3333-3333-333333333333").claim("tenant_id", "default")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.questionBreakdown").exists());
        }

        @Test
        @DisplayName("+ve: EVALUATOR retrieves dashboard metrics - returns 200 OK")
        void evaluatorCanGetSummary() throws Exception {
            mockMvc.perform(get("/api/v1/admin/dashboard/summary")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EVALUATOR"))
                                    .jwt(j -> j.subject("44444444-4444-4444-4444-444444444444").claim("tenant_id", "default")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.evaluationBreakdown").exists());
        }

        @Test
        @DisplayName("-ve: ROLE_CANDIDATE is forbidden from accessing admin dashboard - returns 403 Forbidden")
        void candidateForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/admin/dashboard/summary")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject("55555555-5555-5555-5555-555555555555").claim("tenant_id", "default")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/admin/dashboard/summary")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }
}
