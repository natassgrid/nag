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

package com.examplatform.questionbank.extraction.domain;

import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.shared.constants.QuestionTaxonomyConstants;
import com.examplatform.shared.constants.QuestionTypeConstants;
import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Staging entity representing a single question extracted from an uploaded Question Paper PDF.
 * Holds normalized Markdown + {@code $$...$$} LaTeX content, options, bilingual translations,
 * diagram media references, and AI-suggested taxonomy before review and promotion to
 * {@code question_service.question}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "extracted_question", schema = "question_service")
public class ExtractedQuestion extends BaseEntity {

    @Column(name = "paper_metadata_id", nullable = false)
    private UUID paperMetadataId;

    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;

    @Column(name = "source_page_number", nullable = false)
    private Integer sourcePageNumber;

    @Column(name = "section_name")
    private String sectionName;

    @Column(name = "question_type", nullable = false, length = 30)
    @Builder.Default
    private String questionType = QuestionTypeConstants.SINGLE_MCQ;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<QuestionOption> options = new ArrayList<>();

    @Column(name = "answer_key", columnDefinition = "TEXT")
    private String answerKey;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "passage_text", columnDefinition = "TEXT")
    private String passageText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "translations_json", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Map<String, Object>> translations = new HashMap<>();

    @Column(name = "has_images", nullable = false)
    @Builder.Default
    private boolean hasImages = false;

    @Column(name = "has_tables", nullable = false)
    @Builder.Default
    private boolean hasTables = false;

    @Column(name = "has_formulas", nullable = false)
    @Builder.Default
    private boolean hasFormulas = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "media_urls_json", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<String> mediaUrls = new ArrayList<>();

    @Column(name = "suggested_subject_id")
    private Long suggestedSubjectId;

    @Column(name = "suggested_topic_id")
    private Long suggestedTopicId;

    @Column(name = "suggested_subtopic_id")
    private Long suggestedSubtopicId;

    @Column(name = "suggested_difficulty", nullable = false, length = 20)
    @Builder.Default
    private String suggestedDifficulty = QuestionTaxonomyConstants.Difficulty.MEDIUM;

    @Column(name = "suggested_cognitive_level", nullable = false, length = 20)
    @Builder.Default
    private String suggestedCognitiveLevel = QuestionTaxonomyConstants.CognitiveLevel.UNDERSTAND;

    @Column(name = "extraction_confidence", nullable = false)
    @Builder.Default
    private double extractionConfidence = 0.85;

    @Column(name = "parsing_tier", nullable = false, length = 40)
    @Builder.Default
    private String parsingTier = "TIER_0_LOCAL_TEXT";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_llm_payload_json", columnDefinition = "jsonb")
    private Map<String, Object> rawLlmPayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ExtractedQuestionStatus status = ExtractedQuestionStatus.EXTRACTED;

    @Column(name = "duplicate_of_question_id")
    private UUID duplicateOfQuestionId;

    @Column(name = "promoted_question_id")
    private UUID promotedQuestionId;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @Column(name = "review_comments", columnDefinition = "TEXT")
    private String reviewComments;
}
