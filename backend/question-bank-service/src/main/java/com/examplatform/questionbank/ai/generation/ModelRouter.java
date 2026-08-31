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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Routes question-generation requests to the appropriate LLM model
 * based on the difficulty level.
 *
 * <p>Model selection strategy (configurable via application.yml):
 * <ul>
 *   <li><b>EASY</b> → Amazon Nova Micro (fast, cost-efficient)</li>
 *   <li><b>MEDIUM</b> → Amazon Nova Lite (balanced)</li>
 *   <li><b>HARD</b> → Amazon Nova Lite (balanced)</li>
 * </ul>
 *
 * <p>All model names refer to LiteLLM gateway route names. The actual
 * Bedrock model IDs are configured in {@code litellm-config.yaml}.
 *
 * @see <a href="https://docs.litellm.ai/">LiteLLM Documentation</a>
 */
@Component
public class ModelRouter {

    @Value("${app.ai.models.easy:nova-micro}")
    private String easyModel;

    @Value("${app.ai.models.medium:nova-lite}")
    private String mediumModel;

    @Value("${app.ai.models.hard:nova-lite}")
    private String hardModel;

    /**
     * Selects the LLM model based on the difficulty level.
     *
     * @param difficulty the difficulty level (EASY, MEDIUM, HARD)
     * @return the LiteLLM model name to use for question generation
     */
    public String selectModel(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return mediumModel;
        }

        return switch (difficulty.toUpperCase().trim()) {
            case "EASY" -> easyModel;
            case "MEDIUM" -> mediumModel;
            case "HARD" -> hardModel;
            default -> mediumModel;
        };
    }

    /**
     * Returns the Bedrock model ID for batch inference based on difficulty.
     *
     * @param difficulty the difficulty level (EASY, MEDIUM, HARD)
     * @return the AWS Bedrock model ID for batch inference
     */
    public String selectBatchModelId(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return "amazon.nova-lite-v1:0";
        }

        return switch (difficulty.toUpperCase().trim()) {
            case "EASY" -> "amazon.nova-micro-v1:0";
            case "MEDIUM", "HARD" -> "amazon.nova-lite-v1:0";
            default -> "amazon.nova-lite-v1:0";
        };
    }
}
