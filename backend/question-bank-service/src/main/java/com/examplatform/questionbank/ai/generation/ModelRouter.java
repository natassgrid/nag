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

import org.springframework.stereotype.Component;

/**
 * Routes question-generation requests to the appropriate LLM model
 * based on the subject domain.
 *
 * <p>Model selection strategy (via LiteLLM gateway model names):
 * <ul>
 *   <li><b>Math/Science</b> (mathematics, general science, physics, chemistry)
 *       → {@code qwen2-math-1.5b}</li>
 *   <li><b>Trivia/GK</b> (general studies, indian history, indian geography,
 *       current affairs, sports) → {@code llama3.2-1b}</li>
 *   <li><b>All other subjects</b> → {@code qwen2.5-1.5b} (balanced/structured)</li>
 * </ul>
 *
 * @see <a href="https://docs.litellm.ai/">LiteLLM Documentation</a>
 */
@Component
public class ModelRouter {

    /** LiteLLM model name for math and science subjects. */
    public static final String MODEL_MATH = "qwen2-math-1.5b";

    /** LiteLLM model name for trivia / general knowledge subjects. */
    public static final String MODEL_TRIVIA = "llama3.2-1b";

    /** LiteLLM model name for all other (general) subjects. */
    public static final String MODEL_GENERAL = "qwen2.5-1.5b";

    /**
     * Selects the appropriate LLM model name for a given subject.
     *
     * <p>Matching is case-insensitive. If the subject is {@code null} or blank,
     * the general-purpose model is returned.
     *
     * @param subject the examination subject (e.g., "Mathematics", "Indian History")
     * @return the LiteLLM model name to use for question generation
     */
    public String selectModel(String subject) {
        if (subject == null || subject.isBlank()) {
            return MODEL_GENERAL;
        }

        return switch (subject.toLowerCase().trim()) {
            case "mathematics", "general science", "physics", "chemistry" -> MODEL_MATH;
            case "general studies", "indian history", "indian geography",
                 "current affairs", "sports" -> MODEL_TRIVIA;
            default -> MODEL_GENERAL;
        };
    }
}
