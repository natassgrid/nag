// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BatchGenerateResponse(
        String status, // IN_PROGRESS, COMPLETED
        int totalQueued,
        int totalGenerated,
        int totalFailed,
        List<String> generatedSubtopics,
        String message
) implements Serializable {}
