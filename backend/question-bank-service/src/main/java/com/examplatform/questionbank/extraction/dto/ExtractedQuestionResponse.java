/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

package com.examplatform.questionbank.extraction.dto;

import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.extraction.domain.ExtractedQuestion;
import com.examplatform.questionbank.extraction.domain.ExtractedQuestionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO representing an extracted staging question.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedQuestionResponse {

    private UUID id;
    private Instant createdAt;
    private Instant updatedAt;
    private String tenantId;
    private Long version;

    private UUID paperMetadataId;
    private Integer questionNumber;
    private Integer sourcePageNumber;
    private String sectionName;
    private String questionType;
    private String content;

    @Builder.Default
    private List<QuestionOption> options = new ArrayList<>();

    private String answerKey;
    private String explanation;
    private String passageText;

    @Builder.Default
    private Map<String, Map<String, Object>> translations = new HashMap<>();

    private boolean hasImages;
    private boolean hasTables;
    private boolean hasFormulas;

    @Builder.Default
    private List<String> mediaUrls = new ArrayList<>();

    private Long suggestedSubjectId;
    private Long suggestedTopicId;
    private Long suggestedSubtopicId;
    private String suggestedDifficulty;
    private String suggestedCognitiveLevel;
    private double extractionConfidence;
    private String parsingTier;
    private Map<String, Object> rawLlmPayload;
    private ExtractedQuestionStatus status;
    private UUID duplicateOfQuestionId;
    private UUID promotedQuestionId;
    private UUID reviewerId;
    private String reviewComments;

    /**
     * Converts an {@link ExtractedQuestion} domain entity into an {@link ExtractedQuestionResponse} DTO.
     */
    public static ExtractedQuestionResponse fromEntity(ExtractedQuestion entity) {
        if (entity == null) {
            return null;
        }

        return ExtractedQuestionResponse.builder()
                .id(entity.getId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .tenantId(entity.getTenantId())
                .version(entity.getVersion())
                .paperMetadataId(entity.getPaperMetadataId())
                .questionNumber(entity.getQuestionNumber())
                .sourcePageNumber(entity.getSourcePageNumber())
                .sectionName(entity.getSectionName())
                .questionType(entity.getQuestionType())
                .content(entity.getContent())
                .options(entity.getOptions() != null ? new ArrayList<>(entity.getOptions()) : new ArrayList<>())
                .answerKey(entity.getAnswerKey())
                .explanation(entity.getExplanation())
                .passageText(entity.getPassageText())
                .translations(entity.getTranslations() != null ? new HashMap<>(entity.getTranslations()) : new HashMap<>())
                .hasImages(entity.isHasImages())
                .hasTables(entity.isHasTables())
                .hasFormulas(entity.isHasFormulas())
                .mediaUrls(entity.getMediaUrls() != null ? new ArrayList<>(entity.getMediaUrls()) : new ArrayList<>())
                .suggestedSubjectId(entity.getSuggestedSubjectId())
                .suggestedTopicId(entity.getSuggestedTopicId())
                .suggestedSubtopicId(entity.getSuggestedSubtopicId())
                .suggestedDifficulty(entity.getSuggestedDifficulty())
                .suggestedCognitiveLevel(entity.getSuggestedCognitiveLevel())
                .extractionConfidence(entity.getExtractionConfidence())
                .parsingTier(entity.getParsingTier())
                .rawLlmPayload(entity.getRawLlmPayload() != null ? new HashMap<>(entity.getRawLlmPayload()) : null)
                .status(entity.getStatus())
                .duplicateOfQuestionId(entity.getDuplicateOfQuestionId())
                .promotedQuestionId(entity.getPromotedQuestionId())
                .reviewerId(entity.getReviewerId())
                .reviewComments(entity.getReviewComments())
                .build();
    }
}
