/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.questionbank.dto;

import com.examplatform.questionbank.domain.Question;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO representing a question with decrypted content fields and localization metadata.
 *
 * Validates: Requirements 4.1, 4.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {

    private UUID id;
    private Long subjectId;
    private Long topicId;
    private Long subtopicId;
    private String subject;
    private String topic;
    private String subtopic;
    private String chapter;
    private String difficulty;
    private String cognitiveLevel;
    private String questionType;
    private String content;
    private String answerKey;
    private String explanation;
    private String sourceReferences;
    private String state;
    private Long version;
    private UUID authorId;
    private UUID reviewerId;

    /**
     * Reviewer feedback comments set when a question is rejected back to DRAFT.
     * Visible to the author so they can revise the question accordingly.
     *
     * Validates: Requirements 5.3 (rejection feedback visibility)
     */
    private String reviewComments;

    private String encryptionKeyId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Lightweight flag indicating this question contains image/SVG media */
    private boolean hasImages;

    /** Parsed options for MCQ/MSQ questions */
    private List<QuestionOption> options;

    /** Optional passage FK if this question belongs to a comprehension group */
    private UUID passageId;

    /** 0-based order index within its passage group */
    private Integer passageOrderIndex;

    /** List of language codes that have translations for this question */
    private List<String> translatedLanguages;

    /** Lightweight map of language code -> translation status (e.g., "hi" -> "APPROVED", "ta" -> "DRAFT") */
    private Map<String, String> translationStatusMap;

    /** Translation status for the specifically requested targetLang in query, if any */
    private String translationStatus;

    /**
     * Warnings about similar questions detected during creation (similarity 0.85–0.92).
     * Null/empty when no similar questions were found or for non-creation responses.
     *
     * Validates: Requirements FR-2 (Duplicate Detection — flag for human review)
     */
    private List<SimilarQuestionWarning> warnings;

    /**
     * Creates a builder pre-populated with standard common question properties from a domain entity.
     */
    public static QuestionResponseBuilder builderFrom(Question question) {
        if (question == null) {
            return QuestionResponse.builder();
        }
        LocalDateTime createdAt = question.getCreatedAt() != null
                ? LocalDateTime.ofInstant(question.getCreatedAt(), ZoneOffset.UTC)
                : null;
        LocalDateTime updatedAt = question.getUpdatedAt() != null
                ? LocalDateTime.ofInstant(question.getUpdatedAt(), ZoneOffset.UTC)
                : null;

        return QuestionResponse.builder()
                .id(question.getId())
                .subjectId(question.getSubjectId())
                .topicId(question.getTopicId())
                .subtopicId(question.getSubtopicId())
                .subject(question.getSubject())
                .topic(question.getTopic())
                .subtopic(question.getSubtopic())
                .chapter(question.getChapter())
                .difficulty(question.getDifficulty())
                .cognitiveLevel(question.getCognitiveLevel())
                .questionType(question.getQuestionType())
                .content(question.getContent())
                .answerKey(question.getAnswerKey())
                .explanation(question.getExplanation())
                .sourceReferences(question.getSourceReferences())
                .state(question.getState())
                .version(question.getVersion())
                .authorId(question.getAuthorId())
                .reviewerId(question.getReviewerId())
                .reviewComments(question.getReviewComments())
                .encryptionKeyId(question.getEncryptionKeyId())
                .passageId(question.getPassageId())
                .passageOrderIndex(question.getPassageOrderIndex())
                .hasImages(question.isHasImages())
                .createdAt(createdAt)
                .updatedAt(updatedAt);
    }

    /**
     * Converts a Question domain entity into a QuestionResponse DTO.
     */
    public static QuestionResponse fromEntity(Question question) {
        if (question == null) {
            return null;
        }
        return builderFrom(question)
                .options(question.getOptions())
                .build();
    }

    /**
     * Warning metadata about a similar question detected during duplicate checking.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimilarQuestionWarning {
        /** ID of the similar existing question */
        private UUID questionId;
        /** Cosine similarity score (0.85–0.92 range) */
        private double similarity;
        /** Snippet of the similar question's content */
        private String contentSnippet;
    }
}
