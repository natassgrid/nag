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

import com.examplatform.delivery.domain.ExamSession;
import com.examplatform.delivery.domain.ExamSession.ExamSessionStatus;
import com.examplatform.delivery.service.SessionTimerService;
import com.examplatform.delivery.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("SessionStatusController REST Endpoints E2E Tests (MockMvc)")
class SessionStatusControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private SessionTimerService sessionTimerService;

    private static final String TENANT_ID = "tenant-test";
    private static final UUID CANDIDATE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESSION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Nested
    @DisplayName("GET /api/v1/sessions/{sessionId}/status")
    class GetSessionStatusEndpoint {

        @Test
        @DisplayName("+ve: CANDIDATE retrieves active session status - returns 200 OK")
        void candidateCanGetSessionStatus() throws Exception {
            ExamSession session = ExamSession.builder()
                    .sessionId(SESSION_ID)
                    .candidateId(CANDIDATE_ID)
                    .examId(UUID.randomUUID())
                    .shiftId(UUID.randomUUID())
                    .paperId(UUID.randomUUID())
                    .status(ExamSessionStatus.ACTIVE)
                    .startedAt(Instant.now().minusSeconds(600))
                    .scheduledEndAt(Instant.now().plusSeconds(3000))
                    .currentQuestionIndex(5)
                    .languageCode("en")
                    .fullScreenExitCount(0)
                    .build();

            when(sessionTimerService.getSession(eq(SESSION_ID))).thenReturn(session);
            when(sessionTimerService.getTimeRemaining(eq(SESSION_ID))).thenReturn(Duration.ofSeconds(3000));

            mockMvc.perform(get("/api/v1/sessions/{sessionId}/status", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.timeRemainingSeconds").value(3000))
                    .andExpect(jsonPath("$.fullScreenLocked").value(false));
        }

        @Test
        @DisplayName("+ve: CANDIDATE retrieves session status with fullScreenLocked = true after exit")
        void candidateSeesFullScreenLockedWhenExitCountGreaterThanZero() throws Exception {
            ExamSession session = ExamSession.builder()
                    .sessionId(SESSION_ID)
                    .candidateId(CANDIDATE_ID)
                    .examId(UUID.randomUUID())
                    .shiftId(UUID.randomUUID())
                    .paperId(UUID.randomUUID())
                    .status(ExamSessionStatus.ACTIVE)
                    .startedAt(Instant.now().minusSeconds(600))
                    .scheduledEndAt(Instant.now().plusSeconds(1200))
                    .currentQuestionIndex(10)
                    .languageCode("en")
                    .fullScreenExitCount(2)
                    .build();

            when(sessionTimerService.getSession(eq(SESSION_ID))).thenReturn(session);
            when(sessionTimerService.getTimeRemaining(eq(SESSION_ID))).thenReturn(Duration.ofSeconds(1200));

            mockMvc.perform(get("/api/v1/sessions/{sessionId}/status", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.timeRemainingSeconds").value(1200))
                    .andExpect(jsonPath("$.fullScreenLocked").value(true));
        }

        @Test
        @DisplayName("-ve: Non-existent session returns 404 Not Found")
        void nonExistentSessionReturnsNotFound() throws Exception {
            when(sessionTimerService.getSession(eq(SESSION_ID)))
                    .thenThrow(new NoSuchElementException("Session not found: " + SESSION_ID));

            mockMvc.perform(get("/api/v1/sessions/{sessionId}/status", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("-ve: Non-CANDIDATE role returns 403 Forbidden")
        void nonCandidateForbiddenFromCheckingStatus() throws Exception {
            mockMvc.perform(get("/api/v1/sessions/{sessionId}/status", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/sessions/{sessionId}/status", SESSION_ID))
                    .andExpect(status().isUnauthorized());
        }
    }
}
