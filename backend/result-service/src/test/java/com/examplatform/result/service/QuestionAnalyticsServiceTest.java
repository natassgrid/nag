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

package com.examplatform.result.service;

import com.examplatform.result.client.EvaluationClient;
import com.examplatform.result.client.QuestionBankClient;
import com.examplatform.result.dto.CandidateExamResponseDto;
import com.examplatform.result.dto.QuestionAnalyticsResult;
import com.examplatform.result.dto.QuestionDetailDto;
import com.examplatform.result.dto.ReviewOptionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for QuestionAnalyticsService.
 * Validates: Requirements 26.1, 26.5, Issue #111
 */
@ExtendWith(MockitoExtension.class)
class QuestionAnalyticsServiceTest {

    @Mock
    private EvaluationClient evaluationClient;

    @Mock
    private QuestionBankClient questionBankClient;

    @InjectMocks
    private QuestionAnalyticsService analyticsService;

    private static final String TENANT_ID = "tenant-test";
    private UUID examId;
    private UUID questionId1;
    private UUID questionId2;

    @BeforeEach
    void setUp() {
        examId = UUID.randomUUID();
        questionId1 = UUID.randomUUID();
        questionId2 = UUID.randomUUID();
        analyticsService.clearCache();
    }

    @Test
    @DisplayName("computeAnalytics with 10 candidates computes accurate difficulty, discrimination, and response distribution")
    void computeAnalytics_multiCandidateExam() {
        // Setup 10 candidates with ranked total scores 100 down to 10
        // Top 27% (k=3): candidates 1, 2, 3
        // Bottom 27% (k=3): candidates 8, 9, 10
        List<CandidateExamResponseDto> responses = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            UUID cId = UUID.randomUUID();
            double examTotalScore = (11 - i) * 10.0;

            // Q1: Top 3 get it right (score=1.0, opt-a), Middle 4 (2 get right, 2 get wrong), Bottom 3 all wrong (score=0.0, opt-b)
            boolean q1Correct = (i <= 3) || (i == 4 || i == 5);
            responses.add(CandidateExamResponseDto.builder()
                    .candidateId(cId)
                    .questionId(questionId1)
                    .examTotalScore(examTotalScore)
                    .questionScore(q1Correct ? 1.0 : 0.0)
                    .maxMarks(1.0)
                    .selectedOptionIds(List.of(q1Correct ? "opt-a" : "opt-b"))
                    .timeSpentMs(60000L)
                    .isCorrect(q1Correct)
                    .build());

            // Q2: All 10 candidates get it right
            responses.add(CandidateExamResponseDto.builder()
                    .candidateId(cId)
                    .questionId(questionId2)
                    .examTotalScore(examTotalScore)
                    .questionScore(1.0)
                    .maxMarks(1.0)
                    .selectedOptionIds(List.of("opt-1"))
                    .timeSpentMs(40000L)
                    .isCorrect(true)
                    .build());
        }

        QuestionDetailDto q1Detail = QuestionDetailDto.builder()
                .id(questionId1)
                .subject("Physics")
                .topic("Thermodynamics")
                .difficulty("MEDIUM")
                .cognitiveLevel("APPLY")
                .explanation("Explanation for Q1")
                .options(List.of(
                        ReviewOptionDto.builder().id("opt-a").text("Option A").isCorrect(true).build(),
                        ReviewOptionDto.builder().id("opt-b").text("Option B").isCorrect(false).build(),
                        ReviewOptionDto.builder().id("opt-c").text("Option C").isCorrect(false).build()
                ))
                .build();

        QuestionDetailDto q2Detail = QuestionDetailDto.builder()
                .id(questionId2)
                .subject("Math")
                .topic("Algebra")
                .difficulty("EASY")
                .cognitiveLevel("REMEMBER")
                .explanation("Explanation for Q2")
                .options(List.of(
                        ReviewOptionDto.builder().id("opt-1").text("Option 1").isCorrect(true).build(),
                        ReviewOptionDto.builder().id("opt-2").text("Option 2").isCorrect(false).build()
                ))
                .build();

        when(evaluationClient.getResponsesForExam(eq(examId), eq(TENANT_ID)))
                .thenReturn(responses);
        when(questionBankClient.findQuestionsByIds(any(), eq(TENANT_ID)))
                .thenReturn(List.of(q1Detail, q2Detail));

        List<QuestionAnalyticsResult> results = analyticsService.computeAnalytics(examId, TENANT_ID);

        assertThat(results).hasSize(2);

        // Verify Question 1
        QuestionAnalyticsResult r1 = results.stream().filter(r -> r.getQuestionId().equals(questionId1)).findFirst().orElseThrow();
        assertThat(r1.getTotalAttempted()).isEqualTo(10);
        assertThat(r1.getTotalCorrect()).isEqualTo(5);
        // Difficulty index = 5 / 10 = 0.50
        assertThat(r1.getDifficultyIndex()).isEqualTo(0.50);
        // Top 27% correct rate = 3 / 3 = 1.0; Bottom 27% correct rate = 0 / 3 = 0.0 => Discrimination = 1.0
        assertThat(r1.getDiscriminationIndex()).isEqualTo(1.0);
        assertThat(r1.getAvgTimeSpentMs()).isEqualTo(60000.0);
        assertThat(r1.getResponseDistribution()).containsEntry("opt-a", 5);
        assertThat(r1.getResponseDistribution()).containsEntry("opt-b", 5);
        assertThat(r1.getResponseDistribution()).containsEntry("opt-c", 0);
        assertThat(r1.getSolutionExplanation()).isEqualTo("Explanation for Q1");
        assertThat(r1.getCorrectAnswer()).isEqualTo("opt-a");

        // Verify Question 2 (All correct)
        QuestionAnalyticsResult r2 = results.stream().filter(r -> r.getQuestionId().equals(questionId2)).findFirst().orElseThrow();
        assertThat(r2.getTotalAttempted()).isEqualTo(10);
        assertThat(r2.getTotalCorrect()).isEqualTo(10);
        assertThat(r2.getDifficultyIndex()).isEqualTo(1.0);
        // Top 27% correct rate = 1.0; Bottom 27% correct rate = 1.0 => Discrimination = 0.0
        assertThat(r2.getDiscriminationIndex()).isEqualTo(0.0);
        assertThat(r2.getResponseDistribution()).containsEntry("opt-1", 10);
        assertThat(r2.getResponseDistribution()).containsEntry("opt-2", 0);
    }

    @Test
    @DisplayName("Edge Case: Zero responses for an exam returns empty list")
    void computeAnalytics_zeroResponses_returnsEmptyList() {
        when(evaluationClient.getResponsesForExam(eq(examId), eq(TENANT_ID)))
                .thenReturn(Collections.emptyList());

        List<QuestionAnalyticsResult> results = analyticsService.computeAnalytics(examId, TENANT_ID);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Edge Case: Single candidate exam yields difficulty index but 0.0 discrimination")
    void computeAnalytics_singleCandidateExam() {
        UUID cId = UUID.randomUUID();
        CandidateExamResponseDto resp = CandidateExamResponseDto.builder()
                .candidateId(cId)
                .questionId(questionId1)
                .examTotalScore(50.0)
                .questionScore(1.0)
                .maxMarks(1.0)
                .selectedOptionIds(List.of("opt-a"))
                .timeSpentMs(45000L)
                .isCorrect(true)
                .build();

        QuestionDetailDto qDetail = QuestionDetailDto.builder()
                .id(questionId1)
                .options(List.of(ReviewOptionDto.builder().id("opt-a").isCorrect(true).build()))
                .build();

        when(evaluationClient.getResponsesForExam(eq(examId), eq(TENANT_ID)))
                .thenReturn(List.of(resp));
        when(questionBankClient.findQuestionsByIds(any(), eq(TENANT_ID)))
                .thenReturn(List.of(qDetail));

        List<QuestionAnalyticsResult> results = analyticsService.computeAnalytics(examId, TENANT_ID);

        assertThat(results).hasSize(1);
        QuestionAnalyticsResult r = results.get(0);
        assertThat(r.getTotalAttempted()).isEqualTo(1);
        assertThat(r.getTotalCorrect()).isEqualTo(1);
        assertThat(r.getDifficultyIndex()).isEqualTo(1.0);
        assertThat(r.getDiscriminationIndex()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Edge Case: All candidates answer incorrectly")
    void computeAnalytics_allIncorrect() {
        List<CandidateExamResponseDto> responses = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            responses.add(CandidateExamResponseDto.builder()
                    .candidateId(UUID.randomUUID())
                    .questionId(questionId1)
                    .examTotalScore((7 - i) * 10.0)
                    .questionScore(0.0)
                    .maxMarks(1.0)
                    .selectedOptionIds(List.of("opt-b"))
                    .timeSpentMs(30000L)
                    .isCorrect(false)
                    .build());
        }

        QuestionDetailDto qDetail = QuestionDetailDto.builder()
                .id(questionId1)
                .options(List.of(
                        ReviewOptionDto.builder().id("opt-a").isCorrect(true).build(),
                        ReviewOptionDto.builder().id("opt-b").isCorrect(false).build()
                ))
                .build();

        when(evaluationClient.getResponsesForExam(eq(examId), eq(TENANT_ID)))
                .thenReturn(responses);
        when(questionBankClient.findQuestionsByIds(any(), eq(TENANT_ID)))
                .thenReturn(List.of(qDetail));

        List<QuestionAnalyticsResult> results = analyticsService.computeAnalytics(examId, TENANT_ID);

        assertThat(results).hasSize(1);
        QuestionAnalyticsResult r = results.get(0);
        assertThat(r.getTotalAttempted()).isEqualTo(6);
        assertThat(r.getTotalCorrect()).isEqualTo(0);
        assertThat(r.getDifficultyIndex()).isEqualTo(0.0);
        assertThat(r.getDiscriminationIndex()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Caching: Subsequent calls use cache within 1 hour TTL and cache can be invalidated")
    void computeAnalytics_cachingAndInvalidation() {
        UUID cId = UUID.randomUUID();
        CandidateExamResponseDto resp = CandidateExamResponseDto.builder()
                .candidateId(cId)
                .questionId(questionId1)
                .examTotalScore(50.0)
                .questionScore(1.0)
                .maxMarks(1.0)
                .selectedOptionIds(List.of("opt-a"))
                .timeSpentMs(45000L)
                .isCorrect(true)
                .build();

        QuestionDetailDto qDetail = QuestionDetailDto.builder()
                .id(questionId1)
                .options(List.of(ReviewOptionDto.builder().id("opt-a").isCorrect(true).build()))
                .build();

        when(evaluationClient.getResponsesForExam(eq(examId), eq(TENANT_ID)))
                .thenReturn(List.of(resp));
        when(questionBankClient.findQuestionsByIds(any(), eq(TENANT_ID)))
                .thenReturn(List.of(qDetail));

        // First call - should call client
        List<QuestionAnalyticsResult> results1 = analyticsService.computeAnalytics(examId, TENANT_ID);
        assertThat(results1).hasSize(1);
        verify(evaluationClient, times(1)).getResponsesForExam(eq(examId), eq(TENANT_ID));

        // Second call - should return cached result without calling client
        List<QuestionAnalyticsResult> results2 = analyticsService.computeAnalytics(examId, TENANT_ID);
        assertThat(results2).hasSize(1);
        verify(evaluationClient, times(1)).getResponsesForExam(eq(examId), eq(TENANT_ID));

        // Invalidate cache for exam
        analyticsService.invalidateCache(examId);

        // Third call after invalidation - should call client again
        List<QuestionAnalyticsResult> results3 = analyticsService.computeAnalytics(examId, TENANT_ID);
        assertThat(results3).hasSize(1);
        verify(evaluationClient, times(2)).getResponsesForExam(eq(examId), eq(TENANT_ID));
    }
}
