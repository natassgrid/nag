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
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.result.controller;

import com.examplatform.result.domain.Result;
import com.examplatform.result.dto.CandidateScoreInput;
import com.examplatform.result.dto.ComputeResultsRequest;
import com.examplatform.result.service.ResultComputationService;
import com.examplatform.result.service.ResultPublicationService;
import com.examplatform.result.support.AbstractIntegrationTest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ResultController REST Endpoints E2E Tests (MockMvc)")
class ResultControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private ResultComputationService resultComputationService;

    @MockitoBean
    private ResultPublicationService resultPublicationService;

    private static final String TENANT_ID = "tenant-test";
    private static final UUID RESULT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CANDIDATE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID EXAM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Nested
    @DisplayName("GET /api/v1/results/{candidateId}")
    class GetResultEndpoint {

        @Test
        @DisplayName("+ve: CANDIDATE retrieves result - returns 200 OK")
        void candidateCanRetrieveResult() throws Exception {
            Result result = Result.builder()
                    .candidateId(CANDIDATE_ID)
                    .examId(EXAM_ID)
                    .totalScore(BigDecimal.valueOf(88.50))
                    .overallRank(12)
                    .overallPercentile(BigDecimal.valueOf(95.45))
                    .build();
            ReflectionTestUtils.setField(result, "id", RESULT_ID);

            when(resultComputationService.getResult(eq(CANDIDATE_ID), eq(EXAM_ID), anyString()))
                    .thenReturn(result);

            mockMvc.perform(get("/api/v1/results/{candidateId}", CANDIDATE_ID)
                            .param("examId", EXAM_ID.toString())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(RESULT_ID.toString()))
                    .andExpect(jsonPath("$.candidateId").value(CANDIDATE_ID.toString()))
                    .andExpect(jsonPath("$.totalScore").value(88.50))
                    .andExpect(jsonPath("$.overallRank").value(12));
        }

        @Test
        @DisplayName("+ve: SUPER_ADMIN retrieves result - returns 200 OK")
        void superAdminCanRetrieveResult() throws Exception {
            Result result = Result.builder()
                    .candidateId(CANDIDATE_ID)
                    .examId(EXAM_ID)
                    .totalScore(BigDecimal.valueOf(88.50))
                    .build();
            ReflectionTestUtils.setField(result, "id", RESULT_ID);

            when(resultComputationService.getResult(eq(CANDIDATE_ID), eq(EXAM_ID), anyString()))
                    .thenReturn(result);

            mockMvc.perform(get("/api/v1/results/{candidateId}", CANDIDATE_ID)
                            .param("examId", EXAM_ID.toString())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(RESULT_ID.toString()));
        }

        @Test
        @DisplayName("-ve: Missing examId param returns 400 Bad Request")
        void missingExamIdReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/v1/results/{candidateId}", CANDIDATE_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("-ve: Nonexistent result returns 404 Not Found")
        void notFoundReturns404() throws Exception {
            when(resultComputationService.getResult(eq(CANDIDATE_ID), eq(EXAM_ID), anyString()))
                    .thenThrow(new EntityNotFoundException("Result not found"));

            mockMvc.perform(get("/api/v1/results/{candidateId}", CANDIDATE_ID)
                            .param("examId", EXAM_ID.toString())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_PROCTOR) returns 403 Forbidden")
        void proctorForbiddenFromFetchingResult() throws Exception {
            mockMvc.perform(get("/api/v1/results/{candidateId}", CANDIDATE_ID)
                            .param("examId", EXAM_ID.toString())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROCTOR"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/results/{candidateId}", CANDIDATE_ID)
                            .param("examId", EXAM_ID.toString()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/results/compute")
    class ComputeResultsEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER triggers computation - returns 201 Created")
        void examControllerCanComputeResults() throws Exception {
            CandidateScoreInput input = CandidateScoreInput.builder()
                    .candidateId(CANDIDATE_ID)
                    .examId(EXAM_ID)
                    .totalRawScore(85.0)
                    .sectionScores(Map.of("Section-A", 45.0))
                    .shiftId("shift-1")
                    .build();

            ComputeResultsRequest request = ComputeResultsRequest.builder()
                    .examId(EXAM_ID)
                    .candidateScores(List.of(input))
                    .normalizeShifts(true)
                    .build();

            Result result = Result.builder()
                    .candidateId(CANDIDATE_ID)
                    .examId(EXAM_ID)
                    .totalScore(BigDecimal.valueOf(85.0))
                    .build();
            ReflectionTestUtils.setField(result, "id", RESULT_ID);

            when(resultComputationService.computeResults(eq(EXAM_ID), any(), anyBoolean(), anyString()))
                    .thenReturn(List.of(result));

            mockMvc.perform(post("/api/v1/results/compute")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(RESULT_ID.toString()));
        }

        @Test
        @DisplayName("-ve: Missing examId in request body returns 400 Bad Request")
        void missingExamIdReturnsBadRequest() throws Exception {
            ComputeResultsRequest request = ComputeResultsRequest.builder()
                    .examId(null)
                    .candidateScores(List.of(CandidateScoreInput.builder().candidateId(CANDIDATE_ID).build()))
                    .build();

            mockMvc.perform(post("/api/v1/results/compute")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Bad Request"));
        }

        @Test
        @DisplayName("-ve: Empty candidateScores returns 400 Bad Request")
        void emptyScoresReturnsBadRequest() throws Exception {
            ComputeResultsRequest request = ComputeResultsRequest.builder()
                    .examId(EXAM_ID)
                    .candidateScores(Collections.emptyList())
                    .build();

            mockMvc.perform(post("/api/v1/results/compute")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_CANDIDATE) returns 403 Forbidden")
        void candidateForbiddenFromComputing() throws Exception {
            CandidateScoreInput input = CandidateScoreInput.builder()
                    .candidateId(CANDIDATE_ID)
                    .examId(EXAM_ID)
                    .totalRawScore(85.0)
                    .sectionScores(Map.of("Section-A", 45.0))
                    .shiftId("shift-1")
                    .build();

            ComputeResultsRequest request = ComputeResultsRequest.builder()
                    .examId(EXAM_ID)
                    .candidateScores(List.of(input))
                    .build();

            mockMvc.perform(post("/api/v1/results/compute")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            CandidateScoreInput input = CandidateScoreInput.builder()
                    .candidateId(CANDIDATE_ID)
                    .examId(EXAM_ID)
                    .totalRawScore(85.0)
                    .sectionScores(Map.of("Section-A", 45.0))
                    .shiftId("shift-1")
                    .build();

            ComputeResultsRequest request = ComputeResultsRequest.builder()
                    .examId(EXAM_ID)
                    .candidateScores(List.of(input))
                    .build();

            mockMvc.perform(post("/api/v1/results/compute")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/results/{candidateId}/publish")
    class PublishResultEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER publishes result - returns 200 OK")
        void examControllerCanPublishResult() throws Exception {
            Result result = Result.builder()
                    .candidateId(CANDIDATE_ID)
                    .examId(EXAM_ID)
                    .totalScore(BigDecimal.valueOf(90.0))
                    .digiLockerPushed(true)
                    .build();
            ReflectionTestUtils.setField(result, "id", RESULT_ID);

            when(resultPublicationService.publishResult(eq(CANDIDATE_ID), eq(EXAM_ID), anyString()))
                    .thenReturn(result);

            mockMvc.perform(post("/api/v1/results/{candidateId}/publish", CANDIDATE_ID)
                            .param("examId", EXAM_ID.toString())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(RESULT_ID.toString()))
                    .andExpect(jsonPath("$.digiLockerPushed").value(true));
        }

        @Test
        @DisplayName("-ve: Missing examId param returns 400 Bad Request")
        void missingExamIdReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/results/{candidateId}/publish", CANDIDATE_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_CANDIDATE) returns 403 Forbidden")
        void candidateForbiddenFromPublishing() throws Exception {
            mockMvc.perform(post("/api/v1/results/{candidateId}/publish", CANDIDATE_ID)
                            .param("examId", EXAM_ID.toString())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(post("/api/v1/results/{candidateId}/publish", CANDIDATE_ID)
                            .param("examId", EXAM_ID.toString()))
                    .andExpect(status().isUnauthorized());
        }
    }
}
