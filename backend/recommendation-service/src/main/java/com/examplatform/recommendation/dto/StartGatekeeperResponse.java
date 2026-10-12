// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record StartGatekeeperResponse(
        UUID gatekeeperSessionId,
        String currentPhase,
        double passThreshold,
        int questionCount,
        List<String> failedSubtopics,
        String message
) implements Serializable {}
