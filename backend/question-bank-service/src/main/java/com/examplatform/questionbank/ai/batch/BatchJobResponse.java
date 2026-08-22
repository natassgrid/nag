/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.examplatform.questionbank.ai.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO representing the state of a batch generation job.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchJobResponse {

    private UUID id;
    private BatchJobStatus status;
    private String items;
    private int totalRequested;
    private int totalGenerated;
    private int totalFailed;
    private int totalDuplicates;
    private String modelUsed;
    private UUID initiatedBy;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;

    public int getProgress() {
        if (totalRequested == 0) return 0;
        return Math.min(100, (int) ((totalGenerated + totalFailed + totalDuplicates) * 100L / totalRequested));
    }

    public static BatchJobResponse from(BatchGenerationJob job) {
        return BatchJobResponse.builder()
                .id(job.getId())
                .status(job.getStatus())
                .items(job.getItems())
                .totalRequested(job.getTotalRequested())
                .totalGenerated(job.getTotalGenerated())
                .totalFailed(job.getTotalFailed())
                .totalDuplicates(job.getTotalDuplicates())
                .modelUsed(job.getModelUsed())
                .initiatedBy(job.getInitiatedBy())
                .createdAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .errorMessage(job.getErrorMessage())
                .build();
    }
}
