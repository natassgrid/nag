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
package com.examplatform.questionbank.ai.parser;

import com.examplatform.questionbank.dto.QuestionOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Standardized DTO produced by {@link SampleDocumentParserService}.
 * Normalizes input from clean PDFs, embedded image PDFs, scanned PDFs, or standalone images
 * into a structured format ready for few-shot prompt injection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NormalizedSampleQuestion {

    /** Extracted prompt/stem with normalized KaTeX ($$...$$) and chemistry (\ce{...}). */
    private String stem;

    /** Extracted options if question is MCQ/MSQ. */
    private List<QuestionOption> options;

    /** Identified answer key or model answer if present in sample. */
    private String answerKey;

    /** Identified rationale or explanation if present. */
    private String explanation;

    /** Media reference (S3 URL, data URI, or diagram description) of isolated diagram crop. */
    private String embeddedMedia;

    /** Inferred Bloom taxonomy cognitive level. */
    private String bloomLevel;

    /** Inferred difficulty: EASY, MEDIUM, HARD. */
    private String difficulty;

    /** Question format: SINGLE_MCQ, MULTI_MCQ, NUMERICAL, etc. */
    private String questionType;

    /** Whether mathematical formulas or chemical equations were detected. */
    private boolean hasMathOrChemistry;

    /** Whether an embedded diagram, chart, or figure was extracted. */
    private boolean hasDiagram;

    /** Extraction tier ladder used: TIER_0_LOCAL_TEXT, TIER_1_SELECTIVE_VISION, TIER_2_SCANNED_FALLBACK, STANDALONE_IMAGE. */
    private String parsingTier;
}
