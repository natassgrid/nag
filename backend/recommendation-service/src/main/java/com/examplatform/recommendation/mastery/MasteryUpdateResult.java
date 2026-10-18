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

public record MasteryUpdateResult(
        double newMasteryScore,
        double newConfidence,
        int totalAttempts,
        int totalCorrect,
        String status,
        Instant nextReviewAt
) {}
