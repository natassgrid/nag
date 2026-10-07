/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 Open Digital Public Infrastructure (DPI) Platform Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */
package com.examplatform.questionbank.ai.generation;

import com.examplatform.questionbank.dto.QuestionOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for AI-powered question generation.
 *
 * <p>Contains the list of generated questions along with per-question validation
 * results, duplicate detection outcomes, and metadata about the generation run
 * (model used, counts, execution mode, triage rationale, and critic feedback).
 *
 * <p>Returned from {@code POST /api/v1/questions/generate} and
 * {@code POST /api/v1/questions/generate/with-samples}.
 *
 * @see QuestionGenerationRequest
 * @see ModelRouter
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionGenerationResponse {

    /** The list of generated questions with validation and duplicate info. */
    private List<GeneratedQuestion> questions;

    /** The LiteLLM model name that was used for generation (e.g., "nova-micro", "nova-lite"). */
    private String modelUsed;

    /** Total number of questions the LLM produced (before validation/duplicate filtering). */
    private int totalGenerated;

    /** Number of questions that passed schema and answer validation. */
    private int totalValid;

    /** Number of questions flagged as duplicates (similarity > 0.85/0.92). */
    private int totalDuplicates;

    /** The execution mode path resolved by the triage router: FAST or MULTI_AGENT. */
    private String executionMode;

    /** Rationale provided by ComplexityEvaluator for the chosen path. */
    private String triageRationale;

    /** Metadata regarding uploaded sample parsing (tier used, diagram presence, tokens saved). */
    private Map<String, Object> sampleParsingMetadata;

    /**
     * Represents a single AI-generated question with its validation and duplicate status.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedQuestion {

        /** The question content (mixed format: text + $$LaTeX$$ + <svg>). */
        private String content;

        /** The correct answer key (e.g., "A" for MCQ, or a value for numerical). */
        private String answerKey;

        /** Explanation of the correct answer (mixed format). */
        private String explanation;

        /** The answer options for MCQ/MSQ questions. */
        private List<QuestionOption> options;

        /** Difficulty level: EASY, MEDIUM, or HARD. */
        private String difficulty;

        /** Bloom's taxonomy cognitive level. */
        private String cognitiveLevel;

        /** Type of question: SINGLE_MCQ, MULTI_MCQ, NUMERICAL, DESCRIPTIVE, etc. */
        private String questionType;

        /** Validation result — whether this question passed schema and answer checks. */
        private ValidationResult validation;

        /** Duplicate detection result — null if no duplicate detected. */
        private DuplicateResult duplicate;

        /** The persisted question ID if auto-saved, null if preview-only mode. */
        private UUID savedQuestionId;

        /** Psychometric critic score (0.0 - 1.0) if reviewed in multi-agent mode. */
        private Double criticScore;

        /** Psychometric critic suggestions or verified rubric items. */
        private List<String> criticFeedback;
    }

    /**
     * Schema and answer validation result for a generated question.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationResult {

        /** Whether the question passed all validation checks. */
        private boolean valid;

        /** List of validation failure reasons (empty if valid). */
        private List<String> errors;
    }

    /**
     * Duplicate detection result identifying a similar existing question.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DuplicateResult {

        /** The ID of the existing similar question. */
        private UUID similarQuestionId;

        /** The cosine similarity score (0.0–1.0). */
        private double similarity;
    }
}
