/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
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

package com.examplatform.response.controller;

import com.examplatform.response.domain.Response;
import com.examplatform.response.dto.BulkSaveRequest;
import com.examplatform.response.dto.SaveResponseRequest;
import com.examplatform.response.dto.SaveResponseResponse;
import com.examplatform.response.service.BulkSaveService;
import com.examplatform.response.service.ResponseHistoryService;
import com.examplatform.response.service.ResponseSaveService;
import com.examplatform.response.service.SessionFinalizationService;
import com.examplatform.response.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ResponseController REST Endpoints E2E Tests (MockMvc)")
class ResponseControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private ResponseSaveService responseSaveService;

    @MockitoBean
    private BulkSaveService bulkSaveService;

    @MockitoBean
    private ResponseHistoryService responseHistoryService;

    @MockitoBean
    private SessionFinalizationService sessionFinalizationService;

    private static final String TENANT_ID = "tenant-test";
    private static final UUID CANDIDATE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESSION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID QUESTION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID RESPONSE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Nested
    @DisplayName("POST /api/v1/responses/{sessionId}/save")
    class SaveResponseEndpoint {

        @Test
        @DisplayName("+ve: CANDIDATE saves response successfully - returns 200 OK")
        void candidateCanSaveResponse() throws Exception {
            SaveResponseRequest request = SaveResponseRequest.builder()
                    .questionId(QUESTION_ID)
                    .selectedOptionIds("[\"opt-1\"]")
                    .enteredValue(null)
                    .timestamp(Instant.now())
                    .cumulativeTimeSpentMs(45000)
                    .saveSource("MANUAL")
                    .build();

            SaveResponseResponse responseDto = SaveResponseResponse.builder()
                    .responseId(RESPONSE_ID)
                    .sessionId(SESSION_ID)
                    .questionId(QUESTION_ID)
                    .revisionSequence(1)
                    .saveSource("MANUAL")
                    .savedAt(Instant.now())
                    .build();

            when(responseSaveService.saveResponse(eq(SESSION_ID), any(SaveResponseRequest.class), eq(CANDIDATE_ID), eq(TENANT_ID)))
                    .thenReturn(responseDto);

            mockMvc.perform(post("/api/v1/responses/{sessionId}/save", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("Response saved successfully"))
                    .andExpect(jsonPath("$.data.responseId").value(RESPONSE_ID.toString()))
                    .andExpect(jsonPath("$.data.revisionSequence").value(1));
        }

        @Test
        @DisplayName("-ve: Missing questionId returns 400 Bad Request")
        void missingQuestionIdReturnsBadRequest() throws Exception {
            SaveResponseRequest request = SaveResponseRequest.builder()
                    .questionId(null)
                    .timestamp(Instant.now())
                    .saveSource("MANUAL")
                    .build();

            mockMvc.perform(post("/api/v1/responses/{sessionId}/save", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"));
        }

        @Test
        @DisplayName("-ve: Invalid saveSource returns 400 Bad Request")
        void invalidSaveSourceReturnsBadRequest() throws Exception {
            SaveResponseRequest request = SaveResponseRequest.builder()
                    .questionId(QUESTION_ID)
                    .timestamp(Instant.now())
                    .saveSource("INVALID_SOURCE")
                    .build();

            mockMvc.perform(post("/api/v1/responses/{sessionId}/save", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("-ve: Negative cumulativeTimeSpentMs returns 400 Bad Request")
        void negativeTimeSpentReturnsBadRequest() throws Exception {
            SaveResponseRequest request = SaveResponseRequest.builder()
                    .questionId(QUESTION_ID)
                    .timestamp(Instant.now())
                    .saveSource("AUTO")
                    .cumulativeTimeSpentMs(-100)
                    .build();

            mockMvc.perform(post("/api/v1/responses/{sessionId}/save", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("-ve: Non-CANDIDATE role returns 403 Forbidden")
        void nonCandidateForbiddenFromSaving() throws Exception {
            SaveResponseRequest request = SaveResponseRequest.builder()
                    .questionId(QUESTION_ID)
                    .timestamp(Instant.now())
                    .saveSource("AUTO")
                    .build();

            mockMvc.perform(post("/api/v1/responses/{sessionId}/save", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EVALUATOR"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            SaveResponseRequest request = SaveResponseRequest.builder()
                    .questionId(QUESTION_ID)
                    .timestamp(Instant.now())
                    .saveSource("AUTO")
                    .build();

            mockMvc.perform(post("/api/v1/responses/{sessionId}/save", SESSION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/responses/{sessionId}/bulk-save")
    class BulkSaveEndpoint {

        @Test
        @DisplayName("+ve: CANDIDATE bulk saves buffered responses - returns 200 OK")
        void candidateCanBulkSave() throws Exception {
            SaveResponseRequest item = SaveResponseRequest.builder()
                    .questionId(QUESTION_ID)
                    .timestamp(Instant.now())
                    .saveSource("OFFLINE")
                    .revisionSequence(1)
                    .build();

            BulkSaveRequest request = BulkSaveRequest.builder()
                    .responses(List.of(item))
                    .build();

            SaveResponseResponse responseDto = SaveResponseResponse.builder()
                    .responseId(RESPONSE_ID)
                    .sessionId(SESSION_ID)
                    .questionId(QUESTION_ID)
                    .revisionSequence(1)
                    .saveSource("OFFLINE")
                    .savedAt(Instant.now())
                    .build();

            when(bulkSaveService.bulkSave(eq(SESSION_ID), any(BulkSaveRequest.class), eq(CANDIDATE_ID), eq(TENANT_ID)))
                    .thenReturn(List.of(responseDto));

            mockMvc.perform(post("/api/v1/responses/{sessionId}/bulk-save", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].responseId").value(RESPONSE_ID.toString()));
        }

        @Test
        @DisplayName("-ve: Empty responses list returns 400 Bad Request")
        void emptyResponsesReturnsBadRequest() throws Exception {
            BulkSaveRequest request = BulkSaveRequest.builder()
                    .responses(Collections.emptyList())
                    .build();

            mockMvc.perform(post("/api/v1/responses/{sessionId}/bulk-save", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("-ve: Non-CANDIDATE role returns 403 Forbidden")
        void nonCandidateForbiddenFromBulkSave() throws Exception {
            SaveResponseRequest item = SaveResponseRequest.builder()
                    .questionId(QUESTION_ID)
                    .timestamp(Instant.now())
                    .saveSource("OFFLINE")
                    .build();

            BulkSaveRequest request = BulkSaveRequest.builder()
                    .responses(List.of(item))
                    .build();

            mockMvc.perform(post("/api/v1/responses/{sessionId}/bulk-save", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROCTOR"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/responses/{sessionId}/submit")
    class SubmitSessionEndpoint {

        @Test
        @DisplayName("+ve: CANDIDATE submits exam session - returns 200 OK")
        void candidateCanSubmitSession() throws Exception {
            doNothing().when(sessionFinalizationService).submitSession(eq(SESSION_ID), eq(CANDIDATE_ID), eq(TENANT_ID));

            mockMvc.perform(post("/api/v1/responses/{sessionId}/submit", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .header("X-Tenant-Id", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("Session submitted successfully"));

            verify(sessionFinalizationService).submitSession(eq(SESSION_ID), eq(CANDIDATE_ID), eq(TENANT_ID));
        }

        @Test
        @DisplayName("-ve: Non-CANDIDATE role returns 403 Forbidden")
        void nonCandidateForbiddenFromSubmit() throws Exception {
            mockMvc.perform(post("/api/v1/responses/{sessionId}/submit", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(post("/api/v1/responses/{sessionId}/submit", SESSION_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/responses/{sessionId}/responses")
    class GetSessionResponsesEndpoint {

        @Test
        @DisplayName("+ve: CANDIDATE retrieves session responses - returns 200 OK")
        void candidateCanRetrieveResponses() throws Exception {
            Response resp = Response.builder()
                    .sessionId(SESSION_ID)
                    .questionId(QUESTION_ID)
                    .candidateId(CANDIDATE_ID)
                    .revisionSequence(1)
                    .saveSource("MANUAL")
                    .timestamp(Instant.now())
                    .cumulativeTimeSpentMs(30000L)
                    .isFinal(false)
                    .build();
            ReflectionTestUtils.setField(resp, "id", RESPONSE_ID);
            resp.setTenantId(TENANT_ID);

            when(responseHistoryService.getSessionResponses(eq(SESSION_ID), eq(TENANT_ID)))
                    .thenReturn(List.of(resp));

            mockMvc.perform(get("/api/v1/responses/{sessionId}/responses", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .header("X-Tenant-Id", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].id").value(RESPONSE_ID.toString()));
        }

        @Test
        @DisplayName("+ve: EVALUATOR retrieves session responses - returns 200 OK")
        void evaluatorCanRetrieveResponses() throws Exception {
            when(responseHistoryService.getSessionResponses(eq(SESSION_ID), eq(TENANT_ID)))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/responses/{sessionId}/responses", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EVALUATOR"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID)))
                            .header("X-Tenant-Id", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"));
        }

        @Test
        @DisplayName("+ve: AUDITOR retrieves session responses - returns 200 OK")
        void auditorCanRetrieveResponses() throws Exception {
            when(responseHistoryService.getSessionResponses(eq(SESSION_ID), eq(TENANT_ID)))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/responses/{sessionId}/responses", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID)))
                            .header("X-Tenant-Id", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"));
        }

        @Test
        @DisplayName("-ve: Role without permission (ROLE_PROCTOR) returns 403 Forbidden")
        void proctorForbiddenFromFetchingResponses() throws Exception {
            mockMvc.perform(get("/api/v1/responses/{sessionId}/responses", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROCTOR"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/responses/{sessionId}/responses", SESSION_ID))
                    .andExpect(status().isUnauthorized());
        }
    }
}
