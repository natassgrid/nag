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
 * based on the active AI provider and difficulty level.
 *
 * <p>Supported AI providers:
 * <ul>
 *   <li><b>Bedrock (AWS):</b>
 *     <ul>
 *       <li><b>EASY</b> &rarr; Amazon Nova Micro (fast, cost-efficient)</li>
 *       <li><b>MEDIUM</b> &rarr; Amazon Nova Lite (balanced)</li>
 *       <li><b>HARD</b> &rarr; Amazon Nova Lite (balanced)</li>
 *     </ul>
 *   </li>
 *   <li><b>Gemini (Google AI Studio / Vertex AI):</b>
 *     <ul>
 *       <li><b>EASY</b> &rarr; gemini-flash-lite (fast, cost-efficient)</li>
 *       <li><b>MEDIUM</b> &rarr; gemini-flash (balanced)</li>
 *       <li><b>HARD</b> &rarr; gemini-pro (reasoning-intensive)</li>
 *     </ul>
 *   </li>
 *   <li><b>Local (Ollama / Offline):</b>
 *     <ul>
 *       <li><b>EASY</b> &rarr; llama3.2:1b (local Ollama)</li>
 *       <li><b>MEDIUM</b> &rarr; qwen2.5:1.5b (local Ollama)</li>
 *       <li><b>HARD</b> &rarr; qwen2.5:1.5b (local Ollama)</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>All model names refer to LiteLLM gateway route names. The actual
 * Bedrock and Gemini model IDs are configured in {@code litellm-config.yaml}.
 *
 * @see <a href="https://docs.litellm.ai/">LiteLLM Documentation</a>
 */
@Component
public class ModelRouter {

    public enum AiProvider {
        BEDROCK,
        GEMINI,
        LOCAL
    }

    public static final String AMAZON_NOVA_MICRO_V_1_0 = "amazon.nova-micro-v1:0";
    public static final String AMAZON_NOVA_LITE_V_1_0 = "amazon.nova-lite-v1:0";

    @Value("${app.ai.provider:${AI_PROVIDER:bedrock}}")
    private String provider = "bedrock";

    @Value("${app.ai.models.easy:nova-micro}")
    private String easyModel = "nova-micro";

    @Value("${app.ai.models.medium:nova-lite}")
    private String mediumModel = "nova-lite";

    @Value("${app.ai.models.hard:nova-lite}")
    private String hardModel = "nova-lite";

    @Value("${app.ai.models.gemini.easy:gemini-flash-lite}")
    private String geminiEasyModel = "gemini-flash-lite";

    @Value("${app.ai.models.gemini.medium:gemini-flash}")
    private String geminiMediumModel = "gemini-flash";

    @Value("${app.ai.models.gemini.hard:gemini-pro}")
    private String geminiHardModel = "gemini-pro";

    @Value("${app.ai.models.local.easy:llama3.2:1b}")
    private String localEasyModel = "llama3.2:1b";

    @Value("${app.ai.models.local.medium:qwen2.5:1.5b}")
    private String localMediumModel = "qwen2.5:1.5b";

    @Value("${app.ai.models.local.hard:qwen2.5:1.5b}")
    private String localHardModel = "qwen2.5:1.5b";

    @Value("${app.ai.models.extraction.vision:paper-extract-vision}")
    private String paperExtractionVisionModel = "paper-extract-vision";

    @Value("${app.ai.models.extraction.text:paper-extract-text}")
    private String paperExtractionTextModel = "paper-extract-text";

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getEasyModel() {
        return easyModel;
    }

    public void setEasyModel(String easyModel) {
        this.easyModel = easyModel;
    }

    public String getMediumModel() {
        return mediumModel;
    }

    public void setMediumModel(String mediumModel) {
        this.mediumModel = mediumModel;
    }

    public String getHardModel() {
        return hardModel;
    }

    public void setHardModel(String hardModel) {
        this.hardModel = hardModel;
    }

    public String getGeminiEasyModel() {
        return geminiEasyModel;
    }

    public void setGeminiEasyModel(String geminiEasyModel) {
        this.geminiEasyModel = geminiEasyModel;
    }

    public String getGeminiMediumModel() {
        return geminiMediumModel;
    }

    public void setGeminiMediumModel(String geminiMediumModel) {
        this.geminiMediumModel = geminiMediumModel;
    }

    public String getGeminiHardModel() {
        return geminiHardModel;
    }

    public void setGeminiHardModel(String geminiHardModel) {
        this.geminiHardModel = geminiHardModel;
    }

    public String getLocalEasyModel() {
        return localEasyModel;
    }

    public void setLocalEasyModel(String localEasyModel) {
        this.localEasyModel = localEasyModel;
    }

    public String getLocalMediumModel() {
        return localMediumModel;
    }

    public void setLocalMediumModel(String localMediumModel) {
        this.localMediumModel = localMediumModel;
    }

    public String getLocalHardModel() {
        return localHardModel;
    }

    public void setLocalHardModel(String localHardModel) {
        this.localHardModel = localHardModel;
    }

    public String getPaperExtractionVisionModel() {
        return paperExtractionVisionModel;
    }

    public void setPaperExtractionVisionModel(String paperExtractionVisionModel) {
        this.paperExtractionVisionModel = paperExtractionVisionModel;
    }

    public String getPaperExtractionTextModel() {
        return paperExtractionTextModel;
    }

    public void setPaperExtractionTextModel(String paperExtractionTextModel) {
        this.paperExtractionTextModel = paperExtractionTextModel;
    }

    /**
     * Resolves the active AI provider based on configuration.
     *
     * @return the resolved {@link AiProvider}
     */
    public AiProvider resolveProvider() {
        if (provider != null) {
            String trimmed = provider.trim();
            if ("gemini".equalsIgnoreCase(trimmed)) {
                return AiProvider.GEMINI;
            }
            if ("local".equalsIgnoreCase(trimmed) || "ollama".equalsIgnoreCase(trimmed)) {
                return AiProvider.LOCAL;
            }
        }
        return AiProvider.BEDROCK;
    }

    /**
     * Selects the LLM model based on the difficulty level and active AI provider.
     *
     * @param difficulty the difficulty level (EASY, MEDIUM, HARD)
     * @return the LiteLLM model name to use for question generation
     */
    public String selectModel(String difficulty) {
        AiProvider activeProvider = resolveProvider();
        String diff = (difficulty == null) ? "" : difficulty.toUpperCase().trim();

        return switch (activeProvider) {
            case GEMINI -> switch (diff) {
                case QuestionTaxonomyConstants.Difficulty.EASY -> geminiEasyModel;
                case QuestionTaxonomyConstants.Difficulty.HARD -> geminiHardModel;
                default -> geminiMediumModel;
            };
            case LOCAL -> switch (diff) {
                case QuestionTaxonomyConstants.Difficulty.EASY -> localEasyModel;
                case QuestionTaxonomyConstants.Difficulty.HARD -> localHardModel;
                default -> localMediumModel;
            };
            case BEDROCK -> switch (diff) {
                case QuestionTaxonomyConstants.Difficulty.EASY -> easyModel;
                case QuestionTaxonomyConstants.Difficulty.HARD -> hardModel;
                default -> mediumModel;
            };
        };
    }

    /**
     * Selects the LiteLLM route alias for question paper PDF extraction.
     *
     * @param requiresVision true if the page is scanned or contains visual diagrams/tables
     * @return the LiteLLM model alias to use
     */
    public String selectPaperExtractionModel(boolean requiresVision) {
        return requiresVision ? paperExtractionVisionModel : paperExtractionTextModel;
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
