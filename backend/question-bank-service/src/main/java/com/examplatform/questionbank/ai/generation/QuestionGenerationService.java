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

import com.examplatform.questionbank.ai.parser.NormalizedSampleQuestion;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for AI-powered question generation.
 *
 * <p>Coordinates model routing, sample question multimodal ingestion,
 * conditional complexity triage (Fast Path vs. Multi-Agent Committee),
 * psychometric critique, duplicate auditing, and persistence.
 *
 * @see QuestionGenerationRequest
 * @see QuestionGenerationResponse
 * @see ModelRouter
 */
public interface QuestionGenerationService {

    /**
     * Generates questions based on the provided request parameters.
     */
    QuestionGenerationResponse generate(QuestionGenerationRequest request, String tenantId, UUID authorId);

    /**
     * Generates questions with normalized sample questions guiding style, depth, and structure.
     * Evaluates complexity to route dynamically between Single-Model Fast Path and Multi-Agent pipeline.
     */
    QuestionGenerationResponse generateWithSamples(
            QuestionGenerationRequest request,
            List<NormalizedSampleQuestion> sampleQuestions,
            String tenantId,
            UUID authorId);

    /**
     * Interactively clarifies generation requirements when author parameters are broad or ambiguous.
     */
    ClarifyRequirementsResponse clarifyRequirements(ClarifyRequirementsRequest request);
}
