// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BatchGenerateResponse(
        String batchId,
        String status, // QUEUED, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
        int totalQueued,
        int totalGenerated,
        int totalFailed,
        int percentComplete,
        String currentSubtopic,
        List<String> generatedSubtopics,
        List<String> failedSubtopics,
        String message
) implements Serializable {

    public BatchGenerateResponse(
            String status,
            int totalQueued,
            int totalGenerated,
            int totalFailed,
            List<String> generatedSubtopics,
            String message) {
        this(
                null,
                status,
                totalQueued,
                totalGenerated,
                totalFailed,
                totalQueued > 0 ? (totalGenerated + totalFailed) * 100 / totalQueued : 100,
                null,
                generatedSubtopics,
                List.of(),
                message
        );
    }
}
