// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.controller;

import com.examplatform.recommendation.domain.PerformanceBracket;
import com.examplatform.recommendation.dto.*;
import com.examplatform.recommendation.service.LearnerProfileService;
import com.examplatform.recommendation.service.LearningPathGeneratorService;
import com.examplatform.recommendation.service.MilestoneProgressionService;
import com.examplatform.recommendation.service.RecommendationService;
import com.examplatform.recommendation.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("LearningPathController REST Endpoints E2E Tests (MockMvc)")
class LearningPathControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private LearningPathGeneratorService learningPathGeneratorService;

    @MockitoBean
    private MilestoneProgressionService milestoneProgressionService;

    @MockitoBean
    private RecommendationService recommendationService;

    @MockitoBean
    private LearnerProfileService learnerProfileService;

    private static final UUID CANDIDATE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID DLP_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private DynamicLearningPathResponse buildSampleDlpResponse() {
        MilestoneItemDto item1 = new MilestoneItemDto(
                "P1-1",
                "Rotational Mechanics",
                "Mechanics",
                "Physics",
                PerformanceBracket.CONCEPTUAL_GAP,
                "MICRO_LESSON",
                "Remedial Micro-Lesson: Rotational Mechanics",
                "Focus on moment of inertia derivations",
                20,
                false
        );

        MilestonePhaseDto phase1 = new MilestonePhaseDto(
                1,
                "Phase 1: Remediation (Micro-Lessons)",
                "Review core concept derivations",
                "IN_PROGRESS",
                List.of(item1)
        );

        SubtopicClassificationResult gap = new SubtopicClassificationResult(
                1L, "Physics", 10L, "Mechanics", 100L, "Rotational Mechanics",
                PerformanceBracket.CONCEPTUAL_GAP, 45.0, 1.1, 10.0, false,
                "CORE_CONCEPTUAL_MICRO_LESSONS", "Focus on moment of inertia derivations"
        );

        MilestoneGraphDto graph = new MilestoneGraphDto(
                List.of(phase1),
                List.of(gap),
                List.of(),
                List.of(),
                1,
                0.0
        );

        return new DynamicLearningPathResponse(
                DLP_ID,
                CANDIDATE_ID,
                UUID.randomUUID(),
                "JEE_MAIN",
                "ACTIVE",
                1,
                0,
                0,
                graph,
                Instant.now(),
                Instant.now()
        );
    }

    @Nested
    @DisplayName("GET /api/learning-path/current")
    class GetCurrentEndpoint {

        @Test
        @DisplayName("200 OK - Candidate retrieves their active Dynamic Learning Path")
        void candidateCanGetCurrentPath() throws Exception {
            DynamicLearningPathResponse resp = buildSampleDlpResponse();
            when(learningPathGeneratorService.getCurrentActivePath(eq(CANDIDATE_ID))).thenReturn(Optional.of(resp));

            mockMvc.perform(get("/api/learning-path/current")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(DLP_ID.toString()))
                    .andExpect(jsonPath("$.candidateId").value(CANDIDATE_ID.toString()))
                    .andExpect(jsonPath("$.examTarget").value("JEE_MAIN"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.conceptualGapsCount").value(1))
                    .andExpect(jsonPath("$.milestoneGraph.phases[0].phaseName").value("Phase 1: Remediation (Micro-Lessons)"));
        }

        @Test
        @DisplayName("401 UNAUTHORIZED - When unauthenticated")
        void unauthenticatedReturns401() throws Exception {
            mockMvc.perform(get("/api/learning-path/current"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/learning-path/recalculate")
    class RecalculateEndpoint {

        @Test
        @DisplayName("200 OK - Candidate manually recalculates their DLP")
        void candidateCanRecalculate() throws Exception {
            DynamicLearningPathResponse resp = buildSampleDlpResponse();
            when(learningPathGeneratorService.generateOrRecalculate(eq(CANDIDATE_ID), any(), eq("JEE_MAIN")))
                    .thenReturn(null);
            when(learningPathGeneratorService.toResponse(any())).thenReturn(resp);

            RecalculateLearningPathRequest req = new RecalculateLearningPathRequest(UUID.randomUUID(), "JEE_MAIN");

            mockMvc.perform(post("/api/learning-path/recalculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(DLP_ID.toString()))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(recommendationService).generateOrRefresh(eq(CANDIDATE_ID), eq("JEE_MAIN"));
        }

        @Test
        @DisplayName("401 UNAUTHORIZED - When recalculate unauthenticated")
        void unauthenticatedRecalculateReturns401() throws Exception {
            RecalculateLearningPathRequest req = new RecalculateLearningPathRequest(UUID.randomUUID(), "JEE_MAIN");

            mockMvc.perform(post("/api/learning-path/recalculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Single Canonical Route Enforcement (No Legacy Endpoints)")
    class CanonicalRouteEnforcement {

        @Test
        @DisplayName("404 NOT FOUND - Root GET /api/learning-path is rejected (only /current allowed)")
        void legacyGetRootReturns404() throws Exception {
            mockMvc.perform(get("/api/learning-path")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("404 NOT FOUND - POST /api/learning-path/generate is rejected (only /recalculate allowed)")
        void legacyPostGenerateReturns404() throws Exception {
            mockMvc.perform(post("/api/learning-path/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/learning-path/summary")
    class SummaryEndpoint {

        @Test
        @DisplayName("200 OK - Candidate retrieves mastery summary across subjects")
        void candidateCanGetSummary() throws Exception {
            SubjectMasterySummaryDto subjectSummary = new SubjectMasterySummaryDto(
                    "Physics", 5, 2, 2, 1, 62.5
            );

            LearningPathSummaryResponse summary = new LearningPathSummaryResponse(
                    CANDIDATE_ID,
                    DLP_ID,
                    "JEE_MAIN",
                    "ACTIVE",
                    5,
                    2,
                    2,
                    1,
                    40.0,
                    Map.of("Physics", subjectSummary)
            );

            when(learningPathGeneratorService.getSummary(eq(CANDIDATE_ID))).thenReturn(summary);

            mockMvc.perform(get("/api/learning-path/summary")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.candidateId").value(CANDIDATE_ID.toString()))
                    .andExpect(jsonPath("$.totalSubtopics").value(5))
                    .andExpect(jsonPath("$.conceptualGapsCount").value(2))
                    .andExpect(jsonPath("$.subjectSummaries.Physics.avgAccuracyPercent").value(62.5));
        }
    }

    @Nested
    @DisplayName("Milestone & Gatekeeper Stat Plan Endpoints")
    class MilestoneProgressionEndpoints {

        @Test
        @DisplayName("200 OK - Candidate completes Phase 1 milestone")
        void testCompleteMilestone() throws Exception {
            DynamicLearningPathResponse resp = buildSampleDlpResponse();
            when(milestoneProgressionService.completeMilestone(eq(CANDIDATE_ID), eq("P1-1"))).thenReturn(resp);

            mockMvc.perform(post("/api/learning-path/milestones/{milestoneId}/complete", "P1-1")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(DLP_ID.toString()));
        }

        @Test
        @DisplayName("200 OK - Candidate starts Phase 2 adaptive quiz")
        void testStartAdaptiveQuiz() throws Exception {
            UUID quizSessionId = UUID.randomUUID();
            StartAdaptiveQuizResponse quizResp = new StartAdaptiveQuizResponse(
                    quizSessionId, "PHASE_2_ADAPTIVE_QUIZ", 70.0, 15,
                    List.of("Rotational Mechanics"), "Quiz initialized"
            );
            when(milestoneProgressionService.startAdaptiveQuiz(eq(CANDIDATE_ID))).thenReturn(quizResp);

            mockMvc.perform(post("/api/learning-path/adaptive-quiz/start")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quizSessionId").value(quizSessionId.toString()))
                    .andExpect(jsonPath("$.targetAccuracy").value(70.0));
        }

        @Test
        @DisplayName("200 OK - Candidate submits Phase 2 adaptive quiz score")
        void testSubmitAdaptiveQuiz() throws Exception {
            DynamicLearningPathResponse resp = buildSampleDlpResponse();
            when(milestoneProgressionService.submitAdaptiveQuiz(eq(CANDIDATE_ID), any())).thenReturn(resp);

            SubmitQuizScoreRequest req = new SubmitQuizScoreRequest(UUID.randomUUID(), 75.0);

            mockMvc.perform(post("/api/learning-path/adaptive-quiz/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(DLP_ID.toString()));
        }

        @Test
        @DisplayName("200 OK - Candidate starts Phase 3 Gatekeeper test")
        void testStartGatekeeperTest() throws Exception {
            UUID gatekeeperSessionId = UUID.randomUUID();
            StartGatekeeperResponse gatekeeperResp = new StartGatekeeperResponse(
                    gatekeeperSessionId, "PHASE_3_GATEKEEPER", 75.0, 20,
                    List.of("Rotational Mechanics"), "Gatekeeper initialized"
            );
            when(milestoneProgressionService.startGatekeeperTest(eq(CANDIDATE_ID))).thenReturn(gatekeeperResp);

            mockMvc.perform(post("/api/learning-path/gatekeeper/start")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.gatekeeperSessionId").value(gatekeeperSessionId.toString()))
                    .andExpect(jsonPath("$.passThreshold").value(75.0));
        }

        @Test
        @DisplayName("200 OK - Candidate checks Gatekeeper status")
        void testGetGatekeeperStatus() throws Exception {
            GatekeeperStatusResponse statusResp = new GatekeeperStatusResponse(
                    true, false, "PHASE_1_REMEDIATION", null, 1, List.of("Rotational Mechanics")
            );
            when(milestoneProgressionService.getGatekeeperStatus(eq(CANDIDATE_ID))).thenReturn(statusResp);

            mockMvc.perform(get("/api/learning-path/gatekeeper/status")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.gatekeeperRequired").value(true))
                    .andExpect(jsonPath("$.clearedForFullMock").value(false))
                    .andExpect(jsonPath("$.remainingMilestonesCount").value(1));
        }
    }
}
