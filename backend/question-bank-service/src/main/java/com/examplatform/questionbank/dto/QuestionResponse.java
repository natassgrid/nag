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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO representing a question with decrypted content fields.
 *
 * Validates: Requirements 4.1, 4.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {

    private UUID id;
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
    private String references;
    private String state;
    private UUID authorId;
    private LocalDateTime createdAt;

    /** Parsed options for MCQ/MSQ questions */
    private List<QuestionOption> options;

    /**
     * Warnings about similar questions detected during creation (similarity 0.85–0.92).
     * Null/empty when no similar questions were found or for non-creation responses.
     *
     * Validates: Requirements FR-2 (Duplicate Detection — flag for human review)
     */
    private List<SimilarQuestionWarning> warnings;

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
