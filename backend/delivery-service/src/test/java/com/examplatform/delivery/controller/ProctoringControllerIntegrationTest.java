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

import com.examplatform.delivery.service.ProctoringService;
import com.examplatform.delivery.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ProctoringController REST Endpoints E2E Tests (MockMvc)")
class ProctoringControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private ProctoringService proctoringService;

    private static final String TENANT_ID = "tenant-test";
    private static final UUID CANDIDATE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESSION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Nested
    @DisplayName("POST /api/v1/sessions/{sessionId}/proctoring/snapshot")
    class SnapshotEndpoint {

        @Test
        @DisplayName("+ve: CANDIDATE captures webcam snapshot - returns 200 OK")
        void candidateCanCaptureSnapshot() throws Exception {
            byte[] mockImageData = new byte[]{1, 2, 3, 4, 5};
            doNothing().when(proctoringService).captureSnapshot(eq(SESSION_ID), any(byte[].class), eq(TENANT_ID));

            mockMvc.perform(post("/api/v1/sessions/{sessionId}/proctoring/snapshot", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .content(mockImageData))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("Snapshot captured successfully"));

            verify(proctoringService).captureSnapshot(eq(SESSION_ID), any(byte[].class), eq(TENANT_ID));
        }

        @Test
        @DisplayName("-ve: Missing X-Tenant-Id header returns 400 Bad Request")
        void missingTenantIdHeaderReturnsBadRequest() throws Exception {
            byte[] mockImageData = new byte[]{1, 2, 3};

            mockMvc.perform(post("/api/v1/sessions/{sessionId}/proctoring/snapshot", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .content(mockImageData))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("-ve: Non-CANDIDATE role returns 403 Forbidden")
        void nonCandidateForbiddenFromSnapshot() throws Exception {
            mockMvc.perform(post("/api/v1/sessions/{sessionId}/proctoring/snapshot", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .header("X-Tenant-Id", TENANT_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(post("/api/v1/sessions/{sessionId}/proctoring/snapshot", SESSION_ID)
                            .header("X-Tenant-Id", TENANT_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/sessions/{sessionId}/proctoring/fullscreen-exit")
    class FullScreenExitEndpoint {

        @Test
        @DisplayName("+ve: CANDIDATE records full-screen exit - returns 200 OK")
        void candidateCanRecordFullScreenExit() throws Exception {
            doNothing().when(proctoringService).recordFullScreenExit(SESSION_ID);

            mockMvc.perform(post("/api/v1/sessions/{sessionId}/proctoring/fullscreen-exit", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("Full-screen exit recorded"));

            verify(proctoringService).recordFullScreenExit(SESSION_ID);
        }

        @Test
        @DisplayName("-ve: Non-CANDIDATE role returns 403 Forbidden")
        void nonCandidateForbiddenFromRecordingExit() throws Exception {
            mockMvc.perform(post("/api/v1/sessions/{sessionId}/proctoring/fullscreen-exit", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(post("/api/v1/sessions/{sessionId}/proctoring/fullscreen-exit", SESSION_ID))
                    .andExpect(status().isUnauthorized());
        }
    }
}
