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

package com.examplatform.delivery.controller;

import com.examplatform.delivery.service.OfflineDeliveryService;
import com.examplatform.delivery.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("OfflineDeliveryController REST Endpoints E2E Tests (MockMvc)")
class OfflineDeliveryControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private OfflineDeliveryService offlineDeliveryService;

    private static final String TENANT_ID = "tenant-test";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESSION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String CENTER_ID = "CENTER-001";

    @Nested
    @DisplayName("POST /api/v1/sessions/{sessionId}/offline/preload")
    class PreloadEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER preloads exam package - returns 200 OK")
        void examControllerCanPreloadPackage() throws Exception {
            when(offlineDeliveryService.preloadExamPackage(eq(SESSION_ID), eq(CENTER_ID), eq(TENANT_ID)))
                    .thenReturn("{\"questions\":[{\"id\":\"q1\"}]}");

            mockMvc.perform(post("/api/v1/sessions/{sessionId}/offline/preload", SESSION_ID)
                            .param("centerId", CENTER_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()))
                    .andExpect(jsonPath("$.status").value("PRELOADED"))
                    .andExpect(jsonPath("$.content").value("{\"questions\":[{\"id\":\"q1\"}]}"));
        }

        @Test
        @DisplayName("+ve: CENTER_ADMIN preloads exam package - returns 200 OK")
        void centerAdminCanPreloadPackage() throws Exception {
            when(offlineDeliveryService.preloadExamPackage(eq(SESSION_ID), eq(CENTER_ID), eq(TENANT_ID)))
                    .thenReturn("{\"questions\":[]}");

            mockMvc.perform(post("/api/v1/sessions/{sessionId}/offline/preload", SESSION_ID)
                            .param("centerId", CENTER_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CENTER_ADMIN"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PRELOADED"));
        }

        @Test
        @DisplayName("-ve: Missing centerId parameter returns 400 Bad Request")
        void missingCenterIdReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/sessions/{sessionId}/offline/preload", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("-ve: CANDIDATE role forbidden from preloading - returns 403 Forbidden")
        void candidateForbiddenFromPreloading() throws Exception {
            mockMvc.perform(post("/api/v1/sessions/{sessionId}/offline/preload", SESSION_ID)
                            .param("centerId", CENTER_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(post("/api/v1/sessions/{sessionId}/offline/preload", SESSION_ID)
                            .param("centerId", CENTER_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/sessions/{sessionId}/offline/reconcile")
    class ReconcileEndpoint {

        @Test
        @DisplayName("+ve: CENTER_ADMIN reconciles offline data on reconnect - returns 200 OK")
        void centerAdminCanReconcile() throws Exception {
            doNothing().when(offlineDeliveryService).reconcileOnReconnect(eq(SESSION_ID), eq(TENANT_ID));

            mockMvc.perform(post("/api/v1/sessions/{sessionId}/offline/reconcile", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CENTER_ADMIN"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()))
                    .andExpect(jsonPath("$.status").value("RECONCILED"));
        }

        @Test
        @DisplayName("-ve: CANDIDATE role forbidden from reconciliation - returns 403 Forbidden")
        void candidateForbiddenFromReconciling() throws Exception {
            mockMvc.perform(post("/api/v1/sessions/{sessionId}/offline/reconcile", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(post("/api/v1/sessions/{sessionId}/offline/reconcile", SESSION_ID))
                    .andExpect(status().isUnauthorized());
        }
    }
}
