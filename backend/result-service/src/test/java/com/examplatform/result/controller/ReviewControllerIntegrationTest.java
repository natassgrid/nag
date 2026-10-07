/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.result.controller;

import com.examplatform.result.client.EvaluationClient;
import com.examplatform.result.client.QuestionBankClient;
import com.examplatform.result.domain.Result;
import com.examplatform.result.dto.CandidateEvaluationItemDto;
import com.examplatform.result.dto.QuestionDetailDto;
import com.examplatform.result.dto.ReviewOptionDto;
import com.examplatform.result.repository.ResultRepository;
import com.examplatform.result.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ReviewController REST Endpoints E2E Tests (MockMvc)")
class ReviewControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ResultRepository resultRepository;

    @MockitoBean
    private EvaluationClient evaluationClient;

    @MockitoBean
    private QuestionBankClient questionBankClient;

    private static final String TENANT_ID = "tenant-test";
    private static final UUID CANDIDATE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_CANDIDATE_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID EXAM_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID QUESTION_ID_1 = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID QUESTION_ID_2 = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Nested
    @DisplayName("GET /api/v1/results/{candidateId}/review - Candidate Exam Review")
    class GetExamReviewEndpoint {

        @Test
        @DisplayName("200 OK - CANDIDATE retrieves full post-exam review with real cross-service questions and evaluations")
        void candidateCanGetExamReview() throws Exception {
            CandidateEvaluationItemDto eval1 = CandidateEvaluationItemDto.builder()
                    .evaluationId(UUID.randomUUID())
                    .questionId(QUESTION_ID_1)
                    .candidateId(CANDIDATE_ID)
                    .score(-0.25)
                    .maxMarks(1.0)
                    .negativeMarks(0.25)
                    .status("AUTO_EVALUATED")
                    .candidateSelectedOptionIds(List.of("opt-b"))
                    .timeSpentMs(83000L)
                    .build();

            CandidateEvaluationItemDto eval2 = CandidateEvaluationItemDto.builder()
                    .evaluationId(UUID.randomUUID())
                    .questionId(QUESTION_ID_2)
                    .candidateId(CANDIDATE_ID)
                    .score(1.0)
                    .maxMarks(1.0)
                    .negativeMarks(0.25)
                    .status("AUTO_EVALUATED")
                    .candidateSelectedOptionIds(List.of("opt-1"))
                    .timeSpentMs(45000L)
                    .build();

            QuestionDetailDto q1 = QuestionDetailDto.builder()
                    .id(QUESTION_ID_1)
                    .subject("Physics")
                    .topic("Thermodynamics")
                    .difficulty("MEDIUM")
                    .cognitiveLevel("APPLY")
                    .content("Sample question content with $$\\frac{a}{b}$$ formula.")
                    .explanation("The correct answer is A because $$\\frac{a}{b}$$ represents the ratio.")
                    .options(List.of(
                            ReviewOptionDto.builder().id("opt-a").text("Option A").isCorrect(true).build(),
                            ReviewOptionDto.builder().id("opt-b").text("Option B").isCorrect(false).build(),
                            ReviewOptionDto.builder().id("opt-c").text("Option C").isCorrect(false).build(),
                            ReviewOptionDto.builder().id("opt-d").text("Option D").isCorrect(false).build()
                    ))
                    .build();

            QuestionDetailDto q2 = QuestionDetailDto.builder()
                    .id(QUESTION_ID_2)
                    .subject("Mathematics")
                    .topic("Calculus")
                    .difficulty("EASY")
                    .cognitiveLevel("UNDERSTAND")
                    .content("What is the derivative of sin(x)?")
                    .explanation("The derivative of sin(x) is cos(x).")
                    .options(List.of(
                            ReviewOptionDto.builder().id("opt-1").text("cos(x)").isCorrect(true).build(),
                            ReviewOptionDto.builder().id("opt-2").text("-cos(x)").isCorrect(false).build()
                    ))
                    .build();

            when(evaluationClient.getEvaluationsForCandidate(eq(CANDIDATE_ID), eq(EXAM_ID), eq(TENANT_ID)))
                    .thenReturn(List.of(eval1, eval2));
            when(questionBankClient.findQuestionsByIds(eq(List.of(QUESTION_ID_1, QUESTION_ID_2)), eq(TENANT_ID)))
                    .thenReturn(List.of(q1, q2));

            mockMvc.perform(get("/api/v1/results/{candidateId}/review", CANDIDATE_ID)
                            .param("examId", EXAM_ID.toString())
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.candidateId").value(CANDIDATE_ID.toString()))
                    .andExpect(jsonPath("$.examId").value(EXAM_ID.toString()))
                    .andExpect(jsonPath("$.questions.length()").value(2))
                    .andExpect(jsonPath("$.questions[0].questionNumber").value(1))
                    .andExpect(jsonPath("$.questions[0].questionId").value(QUESTION_ID_1.toString()))
                    .andExpect(jsonPath("$.questions[0].subject").value("Physics"))
                    .andExpect(jsonPath("$.questions[0].topic").value("Thermodynamics"))
                    .andExpect(jsonPath("$.questions[0].marksAwarded").value(-0.25))
                    .andExpect(jsonPath("$.questions[0].isCorrect").value(false))
                    .andExpect(jsonPath("$.questions[0].timeSpentMs").value(83000))
                    .andExpect(jsonPath("$.questions[0].options.length()").value(4))
                    .andExpect(jsonPath("$.questions[1].questionNumber").value(2))
                    .andExpect(jsonPath("$.questions[1].questionId").value(QUESTION_ID_2.toString()))
                    .andExpect(jsonPath("$.questions[1].subject").value("Mathematics"))
                    .andExpect(jsonPath("$.questions[1].isCorrect").value(true))
                    .andExpect(jsonPath("$.questions[1].marksAwarded").value(1.0));
        }

        @Test
        @DisplayName("403 FORBIDDEN - CANDIDATE cannot view another candidate's exam review")
        void candidateCannotViewOtherCandidateReview() throws Exception {
            mockMvc.perform(get("/api/v1/results/{candidateId}/review", OTHER_CANDIDATE_ID)
                            .param("examId", EXAM_ID.toString())
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("200 OK - EXAM_CONTROLLER retrieves post-exam review for any candidate")
        void examControllerCanGetExamReview() throws Exception {
            when(evaluationClient.getEvaluationsForCandidate(eq(CANDIDATE_ID), eq(EXAM_ID), eq(TENANT_ID)))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/v1/results/{candidateId}/review", CANDIDATE_ID)
                            .param("examId", EXAM_ID.toString())
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.candidateId").value(CANDIDATE_ID.toString()))
                    .andExpect(jsonPath("$.examId").value(EXAM_ID.toString()));
        }

        @Test
        @DisplayName("200 OK - SUPER_ADMIN retrieves post-exam review for any candidate")
        void superAdminCanGetExamReview() throws Exception {
            when(evaluationClient.getEvaluationsForCandidate(eq(CANDIDATE_ID), eq(EXAM_ID), eq(TENANT_ID)))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/v1/results/{candidateId}/review", CANDIDATE_ID)
                            .param("examId", EXAM_ID.toString())
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.candidateId").value(CANDIDATE_ID.toString()))
                    .andExpect(jsonPath("$.examId").value(EXAM_ID.toString()));
        }

        @Test
        @DisplayName("403 FORBIDDEN - ROLE_PROCTOR cannot access exam review")
        void proctorForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/results/{candidateId}/review", CANDIDATE_ID)
                            .param("examId", EXAM_ID.toString())
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROCTOR"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("401 UNAUTHORIZED - Unauthenticated request returns 401")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/results/{candidateId}/review", CANDIDATE_ID)
                            .param("examId", EXAM_ID.toString()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("200 OK - Exam review properly evaluates multi-correct options, unattempted questions, and LaTeX math")
        void examReviewEvaluationEdgeCasesAndLatexMath() throws Exception {
            UUID qMulti = UUID.randomUUID();
            UUID qSkipped = UUID.randomUUID();

            CandidateEvaluationItemDto evalMulti = CandidateEvaluationItemDto.builder()
                    .evaluationId(UUID.randomUUID())
                    .questionId(qMulti)
                    .candidateId(CANDIDATE_ID)
                    .score(0.0) // raw score was 0, but options match
                    .maxMarks(2.0)
                    .negativeMarks(0.5)
                    .status("AUTO_EVALUATED")
                    .candidateSelectedOptionIds(List.of("opt-a", "opt-c"))
                    .timeSpentMs(32000L)
                    .build();

            CandidateEvaluationItemDto evalSkipped = CandidateEvaluationItemDto.builder()
                    .evaluationId(UUID.randomUUID())
                    .questionId(qSkipped)
                    .candidateId(CANDIDATE_ID)
                    .score(0.0)
                    .maxMarks(2.0)
                    .negativeMarks(0.5)
                    .status("AUTO_EVALUATED")
                    .candidateSelectedOptionIds(List.of())
                    .timeSpentMs(0L)
                    .build();

            QuestionDetailDto qDetailMulti = QuestionDetailDto.builder()
                    .id(qMulti)
                    .subject("Mathematics")
                    .topic("Calculus")
                    .difficulty("HARD")
                    .cognitiveLevel("ANALYZE")
                    .content("Evaluate the integral $$\\int_0^1 x^2 dx$$ and select equivalent forms:")
                    .explanation("The integral evaluates to $$\\frac{1}{3}$$. Both options A and C represent this value.")
                    .options(List.of(
                            ReviewOptionDto.builder().id("opt-a").text("$$\\frac{1}{3}$$").isCorrect(true).build(),
                            ReviewOptionDto.builder().id("opt-b").text("$$\\frac{1}{2}$$").isCorrect(false).build(),
                            ReviewOptionDto.builder().id("opt-c").text("$$3^{-1}$$").isCorrect(true).build()
                    ))
                    .build();

            QuestionDetailDto qDetailSkipped = QuestionDetailDto.builder()
                    .id(qSkipped)
                    .subject("Physics")
                    .topic("Quantum Mechanics")
                    .difficulty("HARD")
                    .cognitiveLevel("EVALUATE")
                    .content("What is the Planck relation $$E = h\\nu$$?")
                    .explanation("Photon energy is directly proportional to frequency via $$E = h\\nu$$.")
                    .options(List.of(
                            ReviewOptionDto.builder().id("opt-1").text("$$E = h\\nu$$").isCorrect(true).build()
                    ))
                    .build();

            when(evaluationClient.getEvaluationsForCandidate(eq(CANDIDATE_ID), eq(EXAM_ID), eq(TENANT_ID)))
                    .thenReturn(List.of(evalMulti, evalSkipped));
            when(questionBankClient.findQuestionsByIds(eq(List.of(qMulti, qSkipped)), eq(TENANT_ID)))
                    .thenReturn(List.of(qDetailMulti, qDetailSkipped));

            mockMvc.perform(get("/api/v1/results/{candidateId}/review", CANDIDATE_ID)
                            .param("examId", EXAM_ID.toString())
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.questions.length()").value(2))
                    .andExpect(jsonPath("$.questions[0].questionId").value(qMulti.toString()))
                    .andExpect(jsonPath("$.questions[0].isCorrect").value(true))
                    .andExpect(jsonPath("$.questions[0].candidateSelectedOptionIds.length()").value(2))
                    .andExpect(jsonPath("$.questions[0].content").value("Evaluate the integral $$\\int_0^1 x^2 dx$$ and select equivalent forms:"))
                    .andExpect(jsonPath("$.questions[0].explanation").value("The integral evaluates to $$\\frac{1}{3}$$. Both options A and C represent this value."))
                    .andExpect(jsonPath("$.questions[1].questionId").value(qSkipped.toString()))
                    .andExpect(jsonPath("$.questions[1].isCorrect").value(false))
                    .andExpect(jsonPath("$.questions[1].candidateSelectedOptionIds.length()").value(0))
                    .andExpect(jsonPath("$.questions[1].marksAwarded").value(0.0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/results/verify - QR Code Scorecard Verification")
    class VerifyScorecardEndpoint {

        @Test
        @DisplayName("200 OK - Public QR verification returns redacted scorecard data when valid")
        void publicVerifyEndpointSuccess() throws Exception {
            String qrCode = "qr-token-valid-" + UUID.randomUUID();

            Result result = Result.builder()
                    .candidateId(CANDIDATE_ID)
                    .examId(EXAM_ID)
                    .totalScore(BigDecimal.valueOf(87.50))
                    .overallRank(5)
                    .overallPercentile(BigDecimal.valueOf(98.750))
                    .accuracyRate(BigDecimal.valueOf(92.00))
                    .categoryRank(2)
                    .qrVerificationCode(qrCode)
                    .digiLockerPushed(false)
                    .build();
            result.setTenantId(TENANT_ID);
            resultRepository.save(result);

            mockMvc.perform(get("/api/v1/results/verify")
                            .param("code", qrCode))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true))
                    .andExpect(jsonPath("$.code").value(qrCode))
                    .andExpect(jsonPath("$.candidateId").value(CANDIDATE_ID.toString()))
                    .andExpect(jsonPath("$.examId").value(EXAM_ID.toString()))
                    .andExpect(jsonPath("$.totalScore").value(87.5))
                    .andExpect(jsonPath("$.overallRank").value(5))
                    .andExpect(jsonPath("$.overallPercentile").value(98.75))
                    .andExpect(jsonPath("$.status").value("VALID"))
                    .andExpect(jsonPath("$.message").value("Scorecard verified successfully"));
        }

        @Test
        @DisplayName("404 NOT_FOUND - QR verification returns 404 when code does not exist")
        void publicVerifyEndpointNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/results/verify")
                            .param("code", "non-existent-code-999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.valid").value(false))
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("410 GONE - QR verification returns 410 when result has been invalidated")
        void publicVerifyEndpointInvalidated() throws Exception {
            String qrCode = "qr-token-invalidated-" + UUID.randomUUID();

            Result result = Result.builder()
                    .candidateId(CANDIDATE_ID)
                    .examId(EXAM_ID)
                    .totalScore(BigDecimal.valueOf(40.00))
                    .overallRank(100)
                    .sectionalStatusJson("{\"status\":\"INVALIDATED\"}")
                    .qrVerificationCode(qrCode)
                    .digiLockerPushed(false)
                    .build();
            result.setTenantId(TENANT_ID);
            resultRepository.save(result);

            mockMvc.perform(get("/api/v1/results/verify")
                            .param("code", qrCode))
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.valid").value(false))
                    .andExpect(jsonPath("$.error").value("Scorecard has been invalidated"));
        }
    }
}
