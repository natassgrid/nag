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

package com.examplatform.evaluation.controller;

import com.examplatform.evaluation.domain.Evaluation;
import com.examplatform.evaluation.dto.ScoreRequest;
import com.examplatform.evaluation.service.ManualEvaluationService;
import com.examplatform.evaluation.service.ScoreAggregationService;
import com.examplatform.evaluation.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("EvaluationController REST Endpoints E2E Tests (MockMvc)")
class EvaluationControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private ManualEvaluationService manualEvaluationService;

    @MockitoBean
    private ScoreAggregationService scoreAggregationService;

    private static final String TENANT_ID = "tenant-test";
    private static final UUID EVALUATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EVALUATOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SESSION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CANDIDATE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Nested
    @DisplayName("POST /api/v1/evaluations/{id}/score")
    class RecordScoreEndpoint {

        @Test
        @DisplayName("+ve: EVALUATOR records score - returns 200 OK")
        void evaluatorCanRecordScore() throws Exception {
            ScoreRequest request = new ScoreRequest(EVALUATOR_ID, 8.5, "Good reasoning");

            Evaluation eval = Evaluation.builder()
                    .sessionId(SESSION_ID)
                    .questionId(UUID.randomUUID())
                    .candidateId(CANDIDATE_ID)
                    .evaluationType(Evaluation.EvaluationType.MANUAL)
                    .evaluatorId(EVALUATOR_ID)
                    .score(BigDecimal.valueOf(8.5))
                    .maxMarks(BigDecimal.valueOf(10.0))
                    .negativeMarks(BigDecimal.ZERO)
                    .status(Evaluation.EvaluationStatus.MANUAL_EVALUATED)
                    .build();
            ReflectionTestUtils.setField(eval, "id", EVALUATION_ID);

            when(manualEvaluationService.recordScore(eq(EVALUATION_ID), eq(EVALUATOR_ID), eq(8.5), eq("Good reasoning")))
                    .thenReturn(eval);

            mockMvc.perform(post("/api/v1/evaluations/{id}/score", EVALUATION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EVALUATOR"))
                                    .jwt(j -> j.subject(EVALUATOR_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.evaluationId").value(EVALUATION_ID.toString()))
                    .andExpect(jsonPath("$.status").value("MANUAL_EVALUATED"))
                    .andExpect(jsonPath("$.score").value(8.5))
                    .andExpect(jsonPath("$.message").value("Score recorded successfully"));
        }

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER records score - returns 200 OK")
        void examControllerCanRecordScore() throws Exception {
            ScoreRequest request = new ScoreRequest(EVALUATOR_ID, 10.0, "Excellent");

            Evaluation eval = Evaluation.builder()
                    .sessionId(SESSION_ID)
                    .questionId(UUID.randomUUID())
                    .candidateId(CANDIDATE_ID)
                    .evaluationType(Evaluation.EvaluationType.MANUAL)
                    .evaluatorId(EVALUATOR_ID)
                    .score(BigDecimal.valueOf(10.0))
                    .maxMarks(BigDecimal.valueOf(10.0))
                    .negativeMarks(BigDecimal.ZERO)
                    .status(Evaluation.EvaluationStatus.MANUAL_EVALUATED)
                    .build();
            ReflectionTestUtils.setField(eval, "id", EVALUATION_ID);

            when(manualEvaluationService.recordScore(eq(EVALUATION_ID), eq(EVALUATOR_ID), eq(10.0), eq("Excellent")))
                    .thenReturn(eval);

            mockMvc.perform(post("/api/v1/evaluations/{id}/score", EVALUATION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(EVALUATOR_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("MANUAL_EVALUATED"));
        }

        @Test
        @DisplayName("-ve: Missing evaluatorId returns 400 Bad Request")
        void missingEvaluatorIdReturnsBadRequest() throws Exception {
            ScoreRequest request = new ScoreRequest(null, 5.0, "Comments");

            mockMvc.perform(post("/api/v1/evaluations/{id}/score", EVALUATION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EVALUATOR"))
                                    .jwt(j -> j.subject(EVALUATOR_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"));
        }

        @Test
        @DisplayName("-ve: Negative score returns 400 Bad Request")
        void negativeScoreReturnsBadRequest() throws Exception {
            ScoreRequest request = new ScoreRequest(EVALUATOR_ID, -2.5, "Comments");

            mockMvc.perform(post("/api/v1/evaluations/{id}/score", EVALUATION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EVALUATOR"))
                                    .jwt(j -> j.subject(EVALUATOR_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("-ve: Nonexistent evaluation returns 404 Not Found")
        void notFoundEvaluationReturns404() throws Exception {
            ScoreRequest request = new ScoreRequest(EVALUATOR_ID, 5.0, "Comments");

            when(manualEvaluationService.recordScore(eq(EVALUATION_ID), any(), anyDouble(), anyString()))
                    .thenThrow(new IllegalArgumentException("Evaluation not found: " + EVALUATION_ID));

            mockMvc.perform(post("/api/v1/evaluations/{id}/score", EVALUATION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EVALUATOR"))
                                    .jwt(j -> j.subject(EVALUATOR_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("-ve: Evaluation not in scorable state returns 409 Conflict")
        void invalidStateReturns409() throws Exception {
            ScoreRequest request = new ScoreRequest(EVALUATOR_ID, 5.0, "Comments");

            when(manualEvaluationService.recordScore(eq(EVALUATION_ID), any(), anyDouble(), anyString()))
                    .thenThrow(new IllegalStateException("Evaluation is not in a scorable state"));

            mockMvc.perform(post("/api/v1/evaluations/{id}/score", EVALUATION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EVALUATOR"))
                                    .jwt(j -> j.subject(EVALUATOR_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_CANDIDATE) returns 403 Forbidden")
        void candidateForbiddenFromScoring() throws Exception {
            ScoreRequest request = new ScoreRequest(EVALUATOR_ID, 5.0, "Comments");

            mockMvc.perform(post("/api/v1/evaluations/{id}/score", EVALUATION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            ScoreRequest request = new ScoreRequest(EVALUATOR_ID, 5.0, "Comments");

            mockMvc.perform(post("/api/v1/evaluations/{id}/score", EVALUATION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/evaluations/aggregate")
    class AggregateScoresEndpoint {

        @Test
        @DisplayName("+ve: EVALUATOR aggregates scores - returns 200 OK")
        void evaluatorCanAggregateScores() throws Exception {
            Map<String, String> body = Map.of(
                    "sessionId", SESSION_ID.toString(),
                    "candidateId", CANDIDATE_ID.toString()
            );

            Map<String, Object> aggregationResult = Map.of(
                    "sessionId", SESSION_ID.toString(),
                    "candidateId", CANDIDATE_ID.toString(),
                    "totalScore", 45.5,
                    "totalMaxMarks", 50.0,
                    "allEvaluated", true
            );

            when(scoreAggregationService.aggregateScores(eq(SESSION_ID), eq(CANDIDATE_ID), any(), any()))
                    .thenReturn(aggregationResult);

            mockMvc.perform(post("/api/v1/evaluations/aggregate")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EVALUATOR"))
                                    .jwt(j -> j.subject(EVALUATOR_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()))
                    .andExpect(jsonPath("$.totalScore").value(45.5))
                    .andExpect(jsonPath("$.allEvaluated").value(true));
        }

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER aggregates scores - returns 200 OK")
        void examControllerCanAggregateScores() throws Exception {
            Map<String, String> body = Map.of(
                    "sessionId", SESSION_ID.toString(),
                    "candidateId", CANDIDATE_ID.toString()
            );

            when(scoreAggregationService.aggregateScores(eq(SESSION_ID), eq(CANDIDATE_ID), any(), any()))
                    .thenReturn(Map.of("allEvaluated", true));

            mockMvc.perform(post("/api/v1/evaluations/aggregate")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(EVALUATOR_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allEvaluated").value(true));
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_CANDIDATE) returns 403 Forbidden")
        void candidateForbiddenFromAggregating() throws Exception {
            Map<String, String> body = Map.of(
                    "sessionId", SESSION_ID.toString(),
                    "candidateId", CANDIDATE_ID.toString()
            );

            mockMvc.perform(post("/api/v1/evaluations/aggregate")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            Map<String, String> body = Map.of(
                    "sessionId", SESSION_ID.toString(),
                    "candidateId", CANDIDATE_ID.toString()
            );

            mockMvc.perform(post("/api/v1/evaluations/aggregate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
