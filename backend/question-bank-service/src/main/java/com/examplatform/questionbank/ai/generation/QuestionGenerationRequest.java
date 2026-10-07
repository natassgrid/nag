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

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for AI-powered question generation.
 *
 * <p>Specifies the parameters for the generation endpoints
 * {@code POST /api/v1/questions/generate} and {@code POST /api/v1/questions/generate/with-samples}.
 * Supports sample reference questions, execution mode triage, and exam alignment.
 *
 * @see ModelRouter
 * @see ExecutionMode
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionGenerationRequest {

    /** The examination subject (e.g., "Mathematics", "Indian History"). */
    @NotBlank(message = "Subject is required")
    private String subject;

    /** The topic within the subject (e.g., "Quadratic Equations"). */
    @NotBlank(message = "Topic is required")
    private String topic;

    /** Optional subtopic for more specific generation (e.g., "Discriminant"). */
    private String subtopic;

    /** Difficulty level: EASY, MEDIUM, or HARD. */
    @NotBlank(message = "Difficulty is required")
    private String difficulty;

    /** Bloom's taxonomy cognitive level: REMEMBER, UNDERSTAND, APPLY, ANALYZE, EVALUATE, CREATE. */
    @NotBlank(message = "Cognitive level is required")
    private String cognitiveLevel;

    /** Type of question: SINGLE_MCQ, MULTI_MCQ, NUMERICAL, DESCRIPTIVE, PARAGRAPH_SET, ASSERTION_REASON, MATRIX_MATCH. */
    @NotBlank(message = "Question type is required")
    private String questionType;

    /** Optional generation mode: "STANDALONE", "PARAGRAPH_SET". */
    @Builder.Default
    private String generationType = "STANDALONE";

    /** Configuration for paragraph set / reading comprehension generation. */
    private ParagraphSetConfig paragraphSetConfig;

    /** Number of questions to generate (1–5). */
    @Min(value = 1, message = "Count must be at least 1")
    @Max(value = 5, message = "Count must be at most 5")
    @Builder.Default
    private int count = 3;

    /** Whether to check for duplicates before returning generated questions. */
    @Builder.Default
    private boolean avoidDuplicate = true;

    /** Whether to auto-save generated questions as DRAFT (false = preview-only mode). */
    @Builder.Default
    private boolean autoSave = false;

    /** Sample question texts provided by author to guide style, depth, and structure. */
    private List<String> sampleQuestions;

    /** URL or storage location of uploaded sample question file (PDF, PNG, JPEG). */
    private String sampleFileUrl;

    /**
     * Pipeline execution mode: AUTO (conditional triage), FAST (single lightweight model),
     * or MULTI_AGENT (deep review collaborative committee).
     */
    @Builder.Default
    private ExecutionMode executionMode = ExecutionMode.AUTO;

    /** Target competitive examination code (e.g. UPSC_CSE, JEE_ADV, GATE, NEET, CBSE_12). */
    private String targetExam;

    /** Explicit flag requesting psychometric critic committee review. */
    @Builder.Default
    private Boolean requireCriticReview = false;

    /** Generation quality rubric: "STANDARD", "EXAM_READY". */
    @Builder.Default
    private String generationQuality = "STANDARD";

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParagraphSetConfig {
        @Builder.Default
        private int passageWordLength = 250;
        @Builder.Default
        private int subQuestionCount = 3;
        private String passageTheme;
    }
}
