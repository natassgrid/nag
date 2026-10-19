/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 Open Digital Public Infrastructure (DPI) Platform Contributors
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
package com.examplatform.questionbank.extraction.ai;

import com.examplatform.questionbank.dto.QuestionOption;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Structured response payload produced by LiteLLM / Vision Extraction for a single Question Paper page.
 * Accommodates cover/instruction page metadata as well as lists of structured questions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmPageExtractionResult {

    /** True if this page was identified as a cover or general candidate instruction page. */
    private boolean coverPage;

    /** Paper code or booklet series (e.g. "SET-A", "CODE-401"). */
    private String detectedPaperCode;

    /** Booklet serial or unique identifier number if printed on the page. */
    private String detectedBookletSerialNo;

    /** Detected examination year (e.g. 2024, 2025). */
    private Integer detectedExamYear;

    /** Detected subject or paper name (e.g. "Physics", "General Studies Paper I"). */
    private String detectedSubjectName;

    /** Total questions declared on the cover page. */
    private Integer detectedTotalQuestions;

    /** Examination duration in minutes detected on the cover page. */
    private Integer detectedDurationMinutes;

    /** Maximum total marks for the question paper. */
    private Double detectedMaxMarks;

    /** Negative marking deduction penalty ratio (e.g. 0.25, 0.33, 0.50). */
    private Double detectedNegativeMarkingRatio;

    /** Section definitions (e.g., Section A: Physics, Section B: Chemistry). */
    @Builder.Default
    private List<Map<String, Object>> detectedSections = new ArrayList<>();

    /** Structured items/questions extracted from this page. */
    @Builder.Default
    private List<ExtractedItemPayload> questions = new ArrayList<>();

    /**
     * Payload for a single extracted question item.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExtractedItemPayload {

        /** Question index or printed question number on the paper (e.g., 1, 2, 45). */
        private Integer questionNumber;

        /** Name of the section to which the question belongs (e.g., "Section A", "Physics"). */
        private String sectionName;

        /**
         * Question taxonomy type: SINGLE_MCQ, MULTI_MCQ, MATRIX_MATCH,
         * NUMERICAL, DESCRIPTIVE, ASSERTION_REASON, PARAGRAPH_SET.
         */
        private String questionType;

        /** Primary (English) question stem with GFM Markdown for statements/tables and $$...$$ LaTeX. */
        private String content;

        /** Answer choices (A, B, C, D). */
        @Builder.Default
        private List<QuestionOption> options = new ArrayList<>();

        /** Correct answer key if provided on the page or answer key section (e.g. "B" or "42"). */
        private String answerKey;

        /** Step-by-step solution derivation if printed on the page. */
        private String explanation;

        /** Reading comprehension or context passage text for paragraph sets. */
        private String passageText;

        /** Hindi stem for bilingual question papers. */
        private String hindiContent;

        /** Hindi option choices for bilingual papers. */
        @Builder.Default
        private List<QuestionOption> hindiOptions = new ArrayList<>();

        /** True if mathematical, chemical, or physical formulas are present in the stem/options. */
        private boolean hasFormulas;

        /** True if Markdown or tabular data is contained in the question. */
        private boolean hasTables;

        /** True if the question references an embedded diagram or image. */
        private boolean hasDiagram;

        /** AI-suggested difficulty: EASY, MEDIUM, or HARD. */
        private String suggestedDifficulty;

        /** AI-suggested cognitive level: REMEMBER, UNDERSTAND, APPLY, ANALYZE, EVALUATE, CREATE. */
        private String suggestedCognitiveLevel;

        /** Confidence score (0.0 to 1.0) of the extraction. */
        private Double confidence;
    }
}
