package com.examplatform.result.service;

import com.examplatform.result.dto.QuestionAnalyticsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for QuestionAnalyticsService.
 * Validates: Requirements 26.1, 26.5
 */
class QuestionAnalyticsServiceTest {

    private QuestionAnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new QuestionAnalyticsService();
    }

    @Test
    @DisplayName("computeAnalytics returns results for all questions")
    void computeAnalytics_returnsAllQuestions() {
        UUID examId = UUID.randomUUID();
        String tenantId = "tenant-test";

        List<QuestionAnalyticsResult> results = analyticsService.computeAnalytics(examId, tenantId);

        assertThat(results).hasSize(5);
        assertThat(results).allSatisfy(r -> {
            assertThat(r.getQuestionId()).isNotNull();
            assertThat(r.getResponseDistribution()).isNotEmpty();
        });
    }

    @Test
    @DisplayName("difficulty index is computed as correct/total")
    void computeAnalytics_difficultyIndex_isCorrectOverTotal() {
        UUID examId = UUID.randomUUID();

        List<QuestionAnalyticsResult> results = analyticsService.computeAnalytics(examId, "tenant-test");

        // First question: 75 correct out of 100 → difficulty = 0.75
        assertThat(results.get(0).getDifficultyIndex()).isEqualTo(0.75);
        // Second question: 40 correct out of 100 → difficulty = 0.40
        assertThat(results.get(1).getDifficultyIndex()).isEqualTo(0.40);
        // Third question: 20 correct out of 100 → difficulty = 0.20
        assertThat(results.get(2).getDifficultyIndex()).isEqualTo(0.20);
    }

    @Test
    @DisplayName("discrimination index is computed as top27% - bottom27%")
    void computeAnalytics_discriminationIndex_isTopMinusBottom() {
        UUID examId = UUID.randomUUID();

        List<QuestionAnalyticsResult> results = analyticsService.computeAnalytics(examId, "tenant-test");

        // First question: 0.92 - 0.55 = 0.37
        assertThat(results.get(0).getDiscriminationIndex()).isCloseTo(0.37, org.assertj.core.data.Offset.offset(0.001));
        // Second question: 0.70 - 0.15 = 0.55
        assertThat(results.get(1).getDiscriminationIndex()).isCloseTo(0.55, org.assertj.core.data.Offset.offset(0.001));
        // Fifth question: 0.80 - 0.30 = 0.50
        assertThat(results.get(4).getDiscriminationIndex()).isCloseTo(0.50, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    @DisplayName("response distribution sums to total responses")
    void computeAnalytics_responseDistribution_sumsToTotal() {
        UUID examId = UUID.randomUUID();

        List<QuestionAnalyticsResult> results = analyticsService.computeAnalytics(examId, "tenant-test");

        for (QuestionAnalyticsResult result : results) {
            int totalResponses = result.getResponseDistribution().values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
            assertThat(totalResponses).isEqualTo(100);
        }
    }

    @Test
    @DisplayName("difficulty index is between 0 and 1")
    void computeAnalytics_difficultyIndex_inValidRange() {
        UUID examId = UUID.randomUUID();

        List<QuestionAnalyticsResult> results = analyticsService.computeAnalytics(examId, "tenant-test");

        assertThat(results).allSatisfy(r ->
                assertThat(r.getDifficultyIndex()).isBetween(0.0, 1.0));
    }

    @Test
    @DisplayName("discrimination index is between -1 and 1")
    void computeAnalytics_discriminationIndex_inValidRange() {
        UUID examId = UUID.randomUUID();

        List<QuestionAnalyticsResult> results = analyticsService.computeAnalytics(examId, "tenant-test");

        assertThat(results).allSatisfy(r ->
                assertThat(r.getDiscriminationIndex()).isBetween(-1.0, 1.0));
    }
}
