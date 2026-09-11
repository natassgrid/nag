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

package com.examplatform.audit.controller;

import com.examplatform.audit.service.AuditIngestionService;
import com.examplatform.audit.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuditEventController REST Endpoints E2E Tests (MockMvc)")
class AuditEventControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private AuditIngestionService auditIngestionService;

    private static final String TENANT_ID = "tenant-test";
    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Nested
    @DisplayName("PUT /api/v1/audit/events/{id}")
    class RejectUpdateEndpoint {

        @Test
        @DisplayName("+ve/-ve: Immutability enforcement - authenticated PUT rejected with 403 Forbidden and tamper attempt logged")
        void authenticatedPutRejectedWithForbidden() throws Exception {
            mockMvc.perform(put("/api/v1/audit/events/{id}", EVENT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject("actor-123").claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());

            verify(auditIngestionService).recordTamperAttempt(eq(EVENT_ID), eq("actor-123"), anyString());
        }

        @Test
        @DisplayName("-ve: Unauthenticated PUT rejected with 401 Unauthorized")
        void unauthenticatedPutRejected() throws Exception {
            mockMvc.perform(put("/api/v1/audit/events/{id}", EVENT_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/audit/events/{id}")
    class RejectDeleteEndpoint {

        @Test
        @DisplayName("+ve/-ve: Immutability enforcement - authenticated DELETE rejected with 403 Forbidden and tamper attempt logged")
        void authenticatedDeleteRejectedWithForbidden() throws Exception {
            mockMvc.perform(delete("/api/v1/audit/events/{id}", EVENT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject("actor-456").claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());

            verify(auditIngestionService).recordTamperAttempt(eq(EVENT_ID), eq("actor-456"), anyString());
        }

        @Test
        @DisplayName("-ve: Unauthenticated DELETE rejected with 401 Unauthorized")
        void unauthenticatedDeleteRejected() throws Exception {
            mockMvc.perform(delete("/api/v1/audit/events/{id}", EVENT_ID))
                    .andExpect(status().isUnauthorized());
        }
    }
}
