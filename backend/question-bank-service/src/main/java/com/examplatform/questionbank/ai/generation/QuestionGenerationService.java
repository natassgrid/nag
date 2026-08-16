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

/**
 * Service interface for AI-powered question generation.
 *
 * <p>Implementations coordinate model selection (via {@link ModelRouter}),
 * RAG context retrieval, LLM invocation, schema/answer validation,
 * duplicate detection, and optional auto-save of generated questions.
 *
 * <p>The generation pipeline:
 * <ol>
 *   <li>Select the appropriate LLM model based on subject</li>
 *   <li>Retrieve top-K similar existing questions via halfvec embedding (RAG)</li>
 *   <li>Build a prompt with retrieved context and generation parameters</li>
 *   <li>Call the selected model via LiteLLM (OpenAI-compatible endpoint)</li>
 *   <li>Parse the structured JSON response into question DTOs</li>
 *   <li>Validate each generated question (schema + answer correctness)</li>
 *   <li>Run duplicate detection against existing questions</li>
 *   <li>Optionally persist valid, non-duplicate questions as DRAFT</li>
 * </ol>
 *
 * @see QuestionGenerationRequest
 * @see QuestionGenerationResponse
 * @see ModelRouter
 */
public interface QuestionGenerationService {

    /**
     * Generates questions based on the provided request parameters.
     *
     * <p>The method selects the appropriate LLM model for the subject,
     * retrieves relevant existing questions for RAG context, invokes the model,
     * validates generated output, performs duplicate detection, and optionally
     * auto-saves valid questions as DRAFT.
     *
     * @param request  the generation parameters (subject, topic, difficulty, count, etc.)
     * @param tenantId the tenant identifier for multi-tenant isolation
     * @return the generation response containing questions, validation results,
     *         duplicate detection outcomes, and metadata
     */
    QuestionGenerationResponse generate(QuestionGenerationRequest request, String tenantId);
}
