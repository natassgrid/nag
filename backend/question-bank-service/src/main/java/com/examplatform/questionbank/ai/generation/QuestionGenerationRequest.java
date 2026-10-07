/*
 * Copyright (c) 2026 Natassia Grid. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express association or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Request payload for AI-assisted question generation.
 *
 * <p>Specifies parameters including syllabus topic, target difficulty,
 * cognitive level (Bloom's taxonomy), question type, count, and optional
 * sample question reference texts for style/structure conditioning.
 *
 * @author Natassia Grid Development Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionGenerationRequest {

    /**
     * Numeric FK to the subject. When provided alongside {@link #subject}, the backend performs
     * an id-based lookup instead of calling {@code resolveOrCreateByName}, preventing silent
     * taxonomy creation from typos or free-text variants.
     */
    private Long subjectId;

    /**
     * Numeric FK to the topic (must belong to {@link #subjectId} when both are supplied).
     * When provided, the backend looks up the topic directly by id.
     */
    private Long topicId;

    /** Optional numeric FK to the subtopic. */
    private Long subtopicId;

    /** Subject domain (e.g., "Mathematics", "Physics"). */
    @NotBlank(message = "Subject is required")
    private String subject;

    /** Syllabus topic (e.g., "Algebra", "Kinematics"). */
    @NotBlank(message = "Topic is required")
    private String topic;

    /** Optional subtopic for more specific generation (e.g., "Discriminant"). */
    private String subtopic;

    /**
     * Raw text input or author description for the new question.
     * Combined with subject, topic, and optional subtopic, this forms the composite
     * vector search query for RAG top-N deduplication prior to calling any agent.
     */
    private String rawTextInput;

    /**
     * Optional description or specific instructions for the new question.
     */
    private String description;

    /** Difficulty level: EASY, MEDIUM, or HARD. */
    @NotBlank(message = "Difficulty is required")
    private String difficulty;

    /** Cognitive level per Bloom's taxonomy: REMEMBER, UNDERSTAND, APPLY, ANALYZE, EVALUATE, CREATE. */
    @NotBlank(message = "Cognitive level is required")
    private String cognitiveLevel;

    /** Question type: SINGLE_MCQ, MULTIPLE_MCQ, NUMERICAL, TRUE_FALSE, ASSERTION_REASON, PARAGRAPH_SET. */
    @NotBlank(message = "Question type is required")
    private String questionType;

    /** Optional configuration specifically for PARAGRAPH_SET (reading comprehension) questions. */
    private ParagraphSetConfig paragraphConfig;

    /** Number of questions to generate (1 to 5). Defaults to 3. */
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

    /**
     * Builds a composite search query string combining subject, topic, optional subtopic,
     * and raw text input or description for vector search and RAG top-N retrieval.
     * When raw text input / description is absent, falls back to topic (+ subtopic).
     */
    public String buildSearchQuery() {
        String desc = rawTextInput != null && !rawTextInput.isBlank() ? rawTextInput : description;
        if (desc != null && !desc.isBlank()) {
            StringBuilder query = new StringBuilder();
            if (subject != null && !subject.isBlank()) {
                query.append(subject.trim());
            }
            if (topic != null && !topic.isBlank()) {
                if (!query.isEmpty()) query.append(" ");
                query.append(topic.trim());
            }
            if (subtopic != null && !subtopic.isBlank()) {
                if (!query.isEmpty()) query.append(" ");
                query.append(subtopic.trim());
            }
            if (!query.isEmpty()) query.append(" ");
            query.append(desc.trim());
            return query.toString();
        }
        return topic != null ? (topic + (subtopic != null && !subtopic.isBlank() ? " " + subtopic : "")).trim() : "";
    }

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
