// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import java.util.UUID;

public record SubmitQuizScoreRequest(
        UUID sessionId,
        double scorePercent
) {}
