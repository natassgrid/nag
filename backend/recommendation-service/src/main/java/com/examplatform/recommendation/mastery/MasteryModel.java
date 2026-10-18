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

import java.time.Instant;

/**
 * Interface defining the pedagogical mastery update and spaced-repetition scheduling contract.
 */
public interface MasteryModel {

    MasteryUpdateResult onLessonCompleted(
            double currentMastery,
            double currentConfidence,
            int currentAttempts,
            int currentCorrect,
            Instant now
    );

    MasteryUpdateResult onPracticeCompleted(
            double currentMastery,
            double currentConfidence,
            int currentAttempts,
            int currentCorrect,
            int sessionAttempts,
            int sessionCorrect,
            Instant now
    );

    Instant calculateNextReview(double masteryScore, double confidence, int attempts, Instant now);

    String determineStatus(double masteryScore, Instant nextReviewAt, int attempts, Instant now);
}
