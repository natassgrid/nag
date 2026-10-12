// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record GatekeeperStatusResponse(
        boolean gatekeeperRequired,
        boolean clearedForFullMock,
        String currentPhase,
        UUID gatekeeperSessionId,
        int remainingMilestonesCount,
        List<String> remedialSubtopics
) implements Serializable {}
