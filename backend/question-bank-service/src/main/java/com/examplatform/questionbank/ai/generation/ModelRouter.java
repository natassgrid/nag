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
package com.examplatform.questionbank.ai.generation;

import com.examplatform.shared.constants.QuestionTaxonomyConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Routes question-generation requests to the appropriate LLM model
 * based on the difficulty level.
 *
 * <p>Model selection strategy (configurable via application.yml / environment):
 * <ul>
 *   <li><b>Bedrock Enabled:</b>
 *     <ul>
 *       <li><b>EASY</b> &rarr; Amazon Nova Micro (fast, cost-efficient)</li>
 *       <li><b>MEDIUM</b> &rarr; Amazon Nova Lite (balanced)</li>
 *       <li><b>HARD</b> &rarr; Amazon Nova Lite (balanced)</li>
 *     </ul>
 *   </li>
 *   <li><b>Bedrock Disabled (Local Dev / Offline):</b>
 *     <ul>
 *       <li><b>EASY</b> &rarr; llama3.2:1b (local Ollama)</li>
 *       <li><b>MEDIUM</b> &rarr; qwen2.5:1.5b (local Ollama)</li>
 *       <li><b>HARD</b> &rarr; qwen2.5:1.5b (local Ollama)</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>All model names refer to LiteLLM gateway route names. The actual
 * Bedrock model IDs are configured in {@code litellm-config.yaml}.
 *
 * @see <a href="https://docs.litellm.ai/">LiteLLM Documentation</a>
 */
@Component
public class ModelRouter {

    public static final String AMAZON_NOVA_MICRO_V_1_0 = "amazon.nova-micro-v1:0";
    public static final String AMAZON_NOVA_LITE_V_1_0 = "amazon.nova-lite-v1:0";

    @Value("${app.ai.bedrock.enabled:${AI_BEDROCK_ENABLED:true}}")
    private boolean bedrockEnabled = true;

    @Value("${app.ai.models.easy:nova-micro}")
    private String easyModel = "nova-micro";

    @Value("${app.ai.models.medium:nova-lite}")
    private String mediumModel = "nova-lite";

    @Value("${app.ai.models.hard:nova-lite}")
    private String hardModel = "nova-lite";

    @Value("${app.ai.models.local.easy:llama3.2:1b}")
    private String localEasyModel = "llama3.2:1b";

    @Value("${app.ai.models.local.medium:qwen2.5:1.5b}")
    private String localMediumModel = "qwen2.5:1.5b";

    @Value("${app.ai.models.local.hard:qwen2.5:1.5b}")
    private String localHardModel = "qwen2.5:1.5b";

    public boolean isBedrockEnabled() {
        return bedrockEnabled;
    }

    public void setBedrockEnabled(boolean bedrockEnabled) {
        this.bedrockEnabled = bedrockEnabled;
    }

    /**
     * Selects the LLM model based on the difficulty level.
     * If Bedrock is disabled, routes to zero-cost local Ollama models.
     *
     * @param difficulty the difficulty level (EASY, MEDIUM, HARD)
     * @return the LiteLLM model name to use for question generation
     */
    public String selectModel(String difficulty) {
        if (!bedrockEnabled) {
            if (difficulty == null || difficulty.isBlank()) {
                return localMediumModel;
            }
            return switch (difficulty.toUpperCase().trim()) {
                case QuestionTaxonomyConstants.Difficulty.EASY -> localEasyModel;
                case QuestionTaxonomyConstants.Difficulty.HARD -> localHardModel;
                default -> localMediumModel;
            };
        }

        if (difficulty == null || difficulty.isBlank()) {
            return mediumModel;
        }

        return switch (difficulty.toUpperCase().trim()) {
            case QuestionTaxonomyConstants.Difficulty.EASY -> easyModel;
            case QuestionTaxonomyConstants.Difficulty.HARD -> hardModel;
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
            return AMAZON_NOVA_LITE_V_1_0;
        }

        return switch (difficulty.toUpperCase().trim()) {
            case QuestionTaxonomyConstants.Difficulty.EASY -> AMAZON_NOVA_MICRO_V_1_0;
            default -> AMAZON_NOVA_LITE_V_1_0;
        };
    }
}
