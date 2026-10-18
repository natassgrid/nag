// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.dto;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record GenerateAdaptiveSetRequest(
        UUID candidateId,
        List<String> targetSubtopics,
        String targetExam,
        Integer totalQuestions,
        Boolean isGatekeeper
) implements Serializable {}
