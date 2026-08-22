/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.examplatform.questionbank.ai.batch;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for submitting a batch question generation job.
 * Contains multiple generation items, each producing one JSONL record.
 * All items are processed in a single Bedrock batch inference job for cost efficiency.
 *
 * <p>Total questions across all items must not exceed 100.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchGenerationRequest {

    /** List of generation items — each becomes one record in the JSONL input. */
    @NotEmpty(message = "At least one generation item is required")
    @Valid
    private List<BatchItem> items;

    /** Whether to check for duplicates before saving generated questions. */
    @Builder.Default
    private boolean avoidDuplicates = true;

    /**
     * A single generation item within the batch.
     * Each item generates up to 5 questions for one subject/topic/difficulty combination.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchItem {

        @NotBlank(message = "Subject is required")
        private String subject;

        @NotBlank(message = "Topic is required")
        private String topic;

        private String subtopic;

        @NotBlank(message = "Difficulty is required")
        private String difficulty;

        @NotBlank(message = "Cognitive level is required")
        private String cognitiveLevel;

        @NotBlank(message = "Question type is required")
        private String questionType;

        /** Number of questions to generate for this item (1–5). */
        @Min(value = 1, message = "Count must be at least 1")
        @Max(value = 5, message = "Count must be at most 5 per item")
        @Builder.Default
        private int count = 5;
    }
}
