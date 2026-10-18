/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.recommendation.mastery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BktLiteMasteryModel Unit Tests")
class BktLiteMasteryModelTest {

    private BktLiteMasteryModel model;
    private Instant now;

    @BeforeEach
    void setUp() {
        model = new BktLiteMasteryModel();
        now = Instant.parse("2026-10-18T10:00:00Z");
    }

    @Test
    @DisplayName("Completing a lesson increases foundational mastery and sets LEARNING status")
    void testLessonCompletedMasteryBoost() {
        MasteryUpdateResult result = model.onLessonCompleted(0.0, 0.1, 0, 0, now);

        assertThat(result.newMasteryScore()).isGreaterThan(0.20);
        assertThat(result.newConfidence()).isGreaterThan(0.20);
        assertThat(result.status()).isEqualTo("LEARNING");
        assertThat(result.nextReviewAt()).isAfter(now);
    }

    @Test
    @DisplayName("Practicing with high accuracy drives mastery up and triggers MASTERED with 14-day spaced review")
    void testPracticeHighAccuracyMastery() {
        // High initial practice performance
        MasteryUpdateResult result = model.onPracticeCompleted(
                0.75, 0.5, 5, 4, 10, 10, now
        );

        assertThat(result.newMasteryScore()).isGreaterThanOrEqualTo(0.80);
        assertThat(result.newConfidence()).isGreaterThan(0.60);
        assertThat(result.status()).isEqualTo("MASTERED");
        assertThat(result.nextReviewAt()).isEqualTo(now.plus(Duration.ofDays(14)));
    }

    @Test
    @DisplayName("Practicing with low accuracy decreases mastery and sets LEARNING status with 1-day spaced review")
    void testPracticeLowAccuracyMastery() {
        MasteryUpdateResult result = model.onPracticeCompleted(
                0.60, 0.4, 5, 3, 10, 2, now
        );

        assertThat(result.newMasteryScore()).isLessThan(0.60);
        assertThat(result.status()).isIn("LEARNING", "PRACTISING");
        assertThat(result.totalAttempts()).isEqualTo(15);
        assertThat(result.totalCorrect()).isEqualTo(5);
    }

    @Test
    @DisplayName("Spaced repetition flags REVIEW_DUE when review time is in the past")
    void testSpacedRepetitionReviewDue() {
        Instant past = now.minus(Duration.ofDays(2));
        String status = model.determineStatus(0.70, past, 10, now);

        assertThat(status).isEqualTo("REVIEW_DUE");
    }
}
