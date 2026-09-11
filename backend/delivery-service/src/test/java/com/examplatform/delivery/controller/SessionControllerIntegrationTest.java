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

import com.examplatform.delivery.dto.QuestionDeliveryDto;
import com.examplatform.delivery.dto.SessionStartRequest;
import com.examplatform.delivery.dto.SessionStartResponse;
import com.examplatform.delivery.exception.ConcurrentSessionException;
import com.examplatform.delivery.service.ExamQuestionDeliveryService;
import com.examplatform.delivery.service.SessionStartService;
import com.examplatform.delivery.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("SessionController REST Endpoints E2E Tests (MockMvc)")
class SessionControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private SessionStartService sessionStartService;

    @MockitoBean
    private ExamQuestionDeliveryService examQuestionDeliveryService;

    private static final String TENANT_ID = "tenant-test";
    private static final UUID CANDIDATE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EXAM_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SHIFT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SESSION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Nested
    @DisplayName("POST /api/v1/sessions/start")
    class StartSessionEndpoint {

        @Test
        @DisplayName("+ve: CANDIDATE starts new exam session - returns 200 OK")
        void candidateCanStartSession() throws Exception {
            SessionStartRequest request = SessionStartRequest.builder()
                    .examId(EXAM_ID)
                    .shiftId(SHIFT_ID)
                    .languageCode("en")
                    .build();

            SessionStartResponse response = SessionStartResponse.builder()
                    .sessionId(SESSION_ID)
                    .examId(EXAM_ID)
                    .examTitle("National Assessment Test")
                    .shiftId(SHIFT_ID)
                    .candidateId(CANDIDATE_ID)
                    .startedAt(Instant.now())
                    .durationSeconds(3600)
                    .totalQuestions(50)
                    .navigationMode("FLEXIBLE")
                    .build();

            when(sessionStartService.startSession(any(SessionStartRequest.class), eq(CANDIDATE_ID), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/sessions/start")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()))
                    .andExpect(jsonPath("$.examTitle").value("National Assessment Test"))
                    .andExpect(jsonPath("$.navigationMode").value("FLEXIBLE"))
                    .andExpect(jsonPath("$.totalQuestions").value(50));
        }

        @Test
        @DisplayName("-ve: Missing examId in request body returns 400 Bad Request")
        void missingExamIdReturnsBadRequest() throws Exception {
            SessionStartRequest invalidRequest = SessionStartRequest.builder()
                    .examId(null) // Required field
                    .shiftId(SHIFT_ID)
                    .build();

            mockMvc.perform(post("/api/v1/sessions/start")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("-ve: Concurrent session active returns 409 Conflict")
        void concurrentSessionReturnsConflict() throws Exception {
            SessionStartRequest request = SessionStartRequest.builder()
                    .examId(EXAM_ID)
                    .shiftId(SHIFT_ID)
                    .build();

            when(sessionStartService.startSession(any(SessionStartRequest.class), eq(CANDIDATE_ID), eq(TENANT_ID)))
                    .thenThrow(new ConcurrentSessionException("Active session already exists for candidate: " + CANDIDATE_ID));

            mockMvc.perform(post("/api/v1/sessions/start")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.error.code").value("CONCURRENT_SESSION"));
        }

        @Test
        @DisplayName("-ve: Non-CANDIDATE role (EXAM_CONTROLLER) returns 403 Forbidden")
        void examControllerForbiddenFromStartingSession() throws Exception {
            SessionStartRequest request = SessionStartRequest.builder()
                    .examId(EXAM_ID)
                    .build();

            mockMvc.perform(post("/api/v1/sessions/start")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            SessionStartRequest request = SessionStartRequest.builder()
                    .examId(EXAM_ID)
                    .build();

            mockMvc.perform(post("/api/v1/sessions/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/sessions/{sessionId}/resume")
    class ResumeSessionEndpoint {

        @Test
        @DisplayName("+ve: CANDIDATE resumes active session - returns 200 OK")
        void candidateCanResumeSession() throws Exception {
            SessionStartResponse response = SessionStartResponse.builder()
                    .sessionId(SESSION_ID)
                    .examId(EXAM_ID)
                    .candidateId(CANDIDATE_ID)
                    .durationSeconds(1800)
                    .totalQuestions(50)
                    .build();

            when(sessionStartService.resumeSessionById(eq(SESSION_ID), eq(CANDIDATE_ID), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/sessions/{sessionId}/resume", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()))
                    .andExpect(jsonPath("$.candidateId").value(CANDIDATE_ID.toString()));
        }

        @Test
        @DisplayName("-ve: Session not found returns 404 Not Found")
        void nonExistentSessionReturnsNotFound() throws Exception {
            when(sessionStartService.resumeSessionById(eq(SESSION_ID), eq(CANDIDATE_ID), eq(TENANT_ID)))
                    .thenThrow(new NoSuchElementException("Session not found: " + SESSION_ID));

            mockMvc.perform(post("/api/v1/sessions/{sessionId}/resume", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("-ve: Non-CANDIDATE role returns 403 Forbidden")
        void nonCandidateForbiddenFromResume() throws Exception {
            mockMvc.perform(post("/api/v1/sessions/{sessionId}/resume", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/sessions/{sessionId}/questions")
    class GetSessionQuestionsEndpoint {

        @Test
        @DisplayName("+ve: CANDIDATE retrieves session questions - returns 200 OK")
        void candidateCanRetrieveQuestions() throws Exception {
            QuestionDeliveryDto q1 = QuestionDeliveryDto.builder()
                    .id("q1")
                    .text("What is the capital of France?")
                    .build();

            when(examQuestionDeliveryService.getQuestionsForSession(eq(SESSION_ID), eq(TENANT_ID)))
                    .thenReturn(List.of(q1));

            mockMvc.perform(get("/api/v1/sessions/{sessionId}/questions", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].text").value("What is the capital of France?"));
        }

        @Test
        @DisplayName("-ve: Non-CANDIDATE role returns 403 Forbidden")
        void nonCandidateForbiddenFromFetchingQuestions() throws Exception {
            mockMvc.perform(get("/api/v1/sessions/{sessionId}/questions", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROCTOR"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }
    }
}
