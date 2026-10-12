// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.service;

import com.examplatform.recommendation.domain.LearnerSubtopicPerformance;
import com.examplatform.recommendation.domain.PerformanceBracket;
import com.examplatform.recommendation.dto.SubtopicClassificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SubtopicClassifierService 3-Tier Rule Engine Tests")
class SubtopicClassifierServiceTest {

    private SubtopicClassifierService classifierService;

    @BeforeEach
    void setUp() {
        classifierService = new SubtopicClassifierService();
    }

    @Nested
    @DisplayName("Classification Boundary Tests")
    class BoundaryTests {

        @Test
        @DisplayName("Accuracy 49.9% is classified as CONCEPTUAL_GAP (< 50.0%)")
        void testBoundaryBelow50Percent() {
            SubtopicClassificationResult result = classifierService.classify(
                    1L, "Physics",
                    10L, "Mechanics",
                    100L, "Rotational Dynamics",
                    49.9, 1.0, 10.0
            );

            assertThat(result.bracket()).isEqualTo(PerformanceBracket.CONCEPTUAL_GAP);
            assertThat(result.pedagogicalAction()).isEqualTo(SubtopicClassifierService.ACTION_CONCEPTUAL);
            assertThat(result.slowPace()).isFalse();
            assertThat(result.recommendationNote()).contains("Low accuracy");
        }

        @Test
        @DisplayName("Accuracy 50.0% is classified as APPLICATION_GAP (exact lower threshold)")
        void testBoundaryExact50Percent() {
            SubtopicClassificationResult result = classifierService.classify(
                    1L, "Physics",
                    10L, "Mechanics",
                    100L, "Rotational Dynamics",
                    50.0, 1.0, 15.0
            );

            assertThat(result.bracket()).isEqualTo(PerformanceBracket.APPLICATION_GAP);
            assertThat(result.pedagogicalAction()).isEqualTo(SubtopicClassifierService.ACTION_APPLICATION);
            assertThat(result.slowPace()).isFalse();
        }

        @Test
        @DisplayName("Accuracy 75.0% is classified as APPLICATION_GAP (exact upper threshold)")
        void testBoundaryExact75Percent() {
            SubtopicClassificationResult result = classifierService.classify(
                    1L, "Physics",
                    10L, "Mechanics",
                    100L, "Rotational Dynamics",
                    75.0, 1.0, 15.0
            );

            assertThat(result.bracket()).isEqualTo(PerformanceBracket.APPLICATION_GAP);
            assertThat(result.pedagogicalAction()).isEqualTo(SubtopicClassifierService.ACTION_APPLICATION);
        }

        @Test
        @DisplayName("Accuracy 75.1% is classified as PROFICIENT (> 75.0%)")
        void testBoundaryAbove75Percent() {
            SubtopicClassificationResult result = classifierService.classify(
                    1L, "Physics",
                    10L, "Mechanics",
                    100L, "Rotational Dynamics",
                    75.1, 1.0, 5.0
            );

            assertThat(result.bracket()).isEqualTo(PerformanceBracket.PROFICIENT);
            assertThat(result.pedagogicalAction()).isEqualTo(SubtopicClassifierService.ACTION_ADVANCED_TIER);
        }
    }

    @Nested
    @DisplayName("High Skip Rate Override Tests")
    class SkipRateTests {

        @Test
        @DisplayName("Candidates with Skip Rate >= 40% are classified as CONCEPTUAL_GAP regardless of 100% accuracy")
        void testHighSkipRateOverridesPerfectAccuracy() {
            SubtopicClassificationResult result = classifierService.classify(
                    2L, "Mathematics",
                    20L, "Calculus",
                    200L, "Definite Integrals",
                    100.0, 0.9, 40.0
            );

            assertThat(result.bracket()).isEqualTo(PerformanceBracket.CONCEPTUAL_GAP);
            assertThat(result.pedagogicalAction()).isEqualTo(SubtopicClassifierService.ACTION_CONCEPTUAL);
            assertThat(result.recommendationNote()).contains("High skip rate");
        }

        @Test
        @DisplayName("Skip Rate 39.9% with 85% accuracy remains PROFICIENT")
        void testSkipRateJustBelowThreshold() {
            SubtopicClassificationResult result = classifierService.classify(
                    2L, "Mathematics",
                    20L, "Calculus",
                    200L, "Definite Integrals",
                    85.0, 1.0, 39.9
            );

            assertThat(result.bracket()).isEqualTo(PerformanceBracket.PROFICIENT);
            assertThat(result.pedagogicalAction()).isEqualTo(SubtopicClassifierService.ACTION_ADVANCED_TIER);
        }

        @ParameterizedTest(name = "Skip rate {0}% forces CONCEPTUAL_GAP")
        @CsvSource({"40.0", "45.5", "70.0", "100.0"})
        void testSkipRatesAboveThreshold(double skipRate) {
            SubtopicClassificationResult result = classifierService.classify(
                    2L, "Mathematics",
                    20L, "Calculus",
                    200L, "Definite Integrals",
                    90.0, 1.1, skipRate
            );

            assertThat(result.bracket()).isEqualTo(PerformanceBracket.CONCEPTUAL_GAP);
        }
    }

    @Nested
    @DisplayName("Pace Ratio Modifier Tests")
    class PaceRatioTests {

        @Test
        @DisplayName("Proficient candidate with Pace Ratio > 1.25 is assigned TIMED_SPRINT_DRILLS")
        void testProficientWithSlowPace() {
            SubtopicClassificationResult result = classifierService.classify(
                    3L, "Chemistry",
                    30L, "Organic",
                    300L, "Aldehydes & Ketones",
                    88.0, 1.45, 10.0
            );

            assertThat(result.bracket()).isEqualTo(PerformanceBracket.PROFICIENT);
            assertThat(result.slowPace()).isTrue();
            assertThat(result.pedagogicalAction()).isEqualTo(SubtopicClassifierService.ACTION_SPEED_SPRINT);
            assertThat(result.recommendationNote()).contains("timed sprint drills");
        }

        @Test
        @DisplayName("Proficient candidate with Pace Ratio <= 1.25 is assigned ADVANCED_TIER_QUESTIONS")
        void testProficientWithOptimalPace() {
            SubtopicClassificationResult result = classifierService.classify(
                    3L, "Chemistry",
                    30L, "Organic",
                    300L, "Aldehydes & Ketones",
                    88.0, 1.25, 10.0
            );

            assertThat(result.bracket()).isEqualTo(PerformanceBracket.PROFICIENT);
            assertThat(result.slowPace()).isFalse();
            assertThat(result.pedagogicalAction()).isEqualTo(SubtopicClassifierService.ACTION_ADVANCED_TIER);
            assertThat(result.recommendationNote()).contains("advanced-tier multi-correct");
        }
    }

    @Nested
    @DisplayName("LearnerSubtopicPerformance Entity Overload Tests")
    class EntityOverloadTests {

        @Test
        @DisplayName("Correctly maps fields from LearnerSubtopicPerformance domain entity")
        void testEntityOverload() {
            LearnerSubtopicPerformance perf = LearnerSubtopicPerformance.builder()
                    .candidateId(UUID.randomUUID())
                    .subjectId(1L)
                    .subjectName("Physics")
                    .topicId(10L)
                    .topicName("Optics")
                    .subtopicId(101L)
                    .subtopicName("Wave Optics")
                    .avgAccuracyPercent(45.0)
                    .avgPaceRatio(1.1)
                    .avgSkipRatePercent(15.0)
                    .build();

            SubtopicClassificationResult result = classifierService.classify(perf);
            assertThat(result.bracket()).isEqualTo(PerformanceBracket.CONCEPTUAL_GAP);
            assertThat(result.subtopicName()).isEqualTo("Wave Optics");
            assertThat(result.topicName()).isEqualTo("Optics");
            assertThat(result.subjectName()).isEqualTo("Physics");
        }
    }
}
