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

import com.examplatform.audit.domain.AuditEvent;
import com.examplatform.audit.service.AuditQueryService;
import com.examplatform.audit.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuditQueryController REST Endpoints E2E Tests (MockMvc)")
class AuditQueryControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private AuditQueryService auditQueryService;

    private static final String TENANT_ID = "tenant-test";
    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ACTOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Nested
    @DisplayName("GET /api/v1/audit/events")
    class QueryAuditEventsEndpoint {

        @Test
        @DisplayName("+ve: AUDITOR queries audit events successfully - returns 200 OK with Page")
        void auditorCanQueryEvents() throws Exception {
            AuditEvent event = AuditEvent.builder()
                    .id(EVENT_ID)
                    .eventType("SESSION_START")
                    .actorId(ACTOR_ID)
                    .resource("/api/v1/sessions/start")
                    .tenantId(TENANT_ID)
                    .occurredAt(Instant.now())
                    .payloadHash("abcd1234hash")
                    .build();

            Page<AuditEvent> page = new PageImpl<>(List.of(event));

            when(auditQueryService.queryEvents(any(), any(), any(), any(), any(), anyString(), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/audit/events")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(EVENT_ID.toString()))
                    .andExpect(jsonPath("$.content[0].eventType").value("SESSION_START"))
                    .andExpect(jsonPath("$.content[0].actorId").value(ACTOR_ID.toString()));
        }

        @Test
        @DisplayName("+ve: AUDITOR queries with filters - returns 200 OK")
        void auditorCanQueryEventsWithFilters() throws Exception {
            Page<AuditEvent> page = new PageImpl<>(List.of());

            when(auditQueryService.queryEvents(any(), any(), any(), any(), any(), anyString(), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/audit/events")
                            .param("userId", ACTOR_ID.toString())
                            .param("examId", "exam-999")
                            .param("actionType", "SESSION_START")
                            .param("from", "2026-01-01T00:00:00Z")
                            .param("to", "2026-12-31T23:59:59Z")
                            .param("page", "0")
                            .param("size", "10")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_CANDIDATE) returns 403 Forbidden")
        void candidateForbiddenFromQueryingAudit() throws Exception {
            mockMvc.perform(get("/api/v1/audit/events")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_PROCTOR) returns 403 Forbidden")
        void proctorForbiddenFromQueryingAudit() throws Exception {
            mockMvc.perform(get("/api/v1/audit/events")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROCTOR"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/audit/events"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
