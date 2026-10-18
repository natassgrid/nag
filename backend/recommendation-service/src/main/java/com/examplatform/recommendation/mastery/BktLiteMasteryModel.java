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

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Bayesian Knowledge Tracing (BKT-lite) and Elo-hybrid adaptive mastery model.
 * Provides deterministic updates, confidence growth, and Leitner-style spaced repetition scheduling.
 */
@Component
public class BktLiteMasteryModel implements MasteryModel {

    private static final double LESSON_TRANSITION_PROBABILITY = 0.25;

    @Override
    public MasteryUpdateResult onLessonCompleted(
            double currentMastery,
            double currentConfidence,
            int currentAttempts,
            int currentCorrect,
            Instant now) {

        // Completing a lesson yields knowledge acquisition transition towards foundation
        double newMastery = Math.min(0.85, currentMastery + LESSON_TRANSITION_PROBABILITY * (1.0 - currentMastery));
        double newConfidence = Math.min(1.0, currentConfidence + 0.15);

        Instant nextReview = calculateNextReview(newMastery, newConfidence, currentAttempts, now);
        String status = determineStatus(newMastery, nextReview, currentAttempts, now);

        return new MasteryUpdateResult(
                round3(newMastery),
                round3(newConfidence),
                currentAttempts,
                currentCorrect,
                status,
                nextReview
        );
    }

    @Override
    public MasteryUpdateResult onPracticeCompleted(
            double currentMastery,
            double currentConfidence,
            int currentAttempts,
            int currentCorrect,
            int sessionAttempts,
            int sessionCorrect,
            Instant now) {

        int totalAttempts = currentAttempts + sessionAttempts;
        int totalCorrect = currentCorrect + sessionCorrect;

        double sessionAccuracy = (sessionAttempts > 0)
                ? (double) sessionCorrect / sessionAttempts
                : 0.0;

        // Adaptive K-factor decreases smoothly as sample size increases
        double kFactor = Math.max(0.10, 0.45 / (1.0 + 0.10 * currentAttempts));
        double delta = kFactor * (sessionAccuracy - currentMastery);
        double newMastery = Math.clamp(currentMastery + delta, 0.0, 1.0);

        // Confidence increases asymptotically with evidence volume
        double newConfidence = Math.min(1.0, Math.max(0.20, 1.0 - Math.exp(-0.25 * totalAttempts)));

        Instant nextReview = calculateNextReview(newMastery, newConfidence, totalAttempts, now);
        String status = determineStatus(newMastery, nextReview, totalAttempts, now);

        return new MasteryUpdateResult(
                round3(newMastery),
                round3(newConfidence),
                totalAttempts,
                totalCorrect,
                status,
                nextReview
        );
    }

    @Override
    public Instant calculateNextReview(double masteryScore, double confidence, int attempts, Instant now) {
        Instant base = (now != null) ? now : Instant.now();

        // Spaced repetition interval based on mastery and confidence
        long intervalDays;
        if (masteryScore >= 0.80 && confidence >= 0.60) {
            intervalDays = 14;
        } else if (masteryScore >= 0.70) {
            intervalDays = 7;
        } else if (masteryScore >= 0.50) {
            intervalDays = 3;
        } else {
            intervalDays = 1;
        }

        return base.plus(Duration.ofDays(intervalDays));
    }

    @Override
    public String determineStatus(double masteryScore, Instant nextReviewAt, int attempts, Instant now) {
        Instant checkTime = (now != null) ? now : Instant.now();

        if (nextReviewAt != null && checkTime.isAfter(nextReviewAt) && masteryScore >= 0.50) {
            return "REVIEW_DUE";
        }

        if (masteryScore >= 0.80) {
            return "MASTERED";
        }
        if (attempts > 0 || masteryScore >= 0.40) {
            return "PRACTISING";
        }
        if (masteryScore > 0.0) {
            return "LEARNING";
        }
        return "NOT_STARTED";
    }

    private double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
