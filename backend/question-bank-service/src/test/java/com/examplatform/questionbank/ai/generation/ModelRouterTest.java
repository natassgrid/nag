/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.examplatform.questionbank.ai.generation;

import com.examplatform.shared.constants.QuestionTaxonomyConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static com.examplatform.questionbank.ai.generation.ModelRouter.AMAZON_NOVA_LITE_V_1_0;
import static com.examplatform.questionbank.ai.generation.ModelRouter.AMAZON_NOVA_MICRO_V_1_0;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ModelRouter.class)
@TestPropertySource(properties = {
        "app.ai.models.easy=nova-micro",
        "app.ai.models.medium=nova-lite",
        "app.ai.models.hard=nova-lite"
})
@DisplayName("ModelRouter — difficulty-based model routing")
class ModelRouterTest {

    @Autowired
    private ModelRouter modelRouter;

    @AfterEach
    void resetModelRouter() {
        modelRouter.setProvider("bedrock");
        modelRouter.setGeminiEasyModel("gemini-flash-lite");
        modelRouter.setGeminiMediumModel("gemini-flash");
        modelRouter.setGeminiHardModel("gemini-pro");
        modelRouter.setEasyModel("nova-micro");
        modelRouter.setMediumModel("nova-lite");
        modelRouter.setHardModel("nova-lite");
        modelRouter.setLocalEasyModel("llama3.2:1b");
        modelRouter.setLocalMediumModel("qwen2.5:1.5b");
        modelRouter.setLocalHardModel("qwen2.5:1.5b");
    }

    @ParameterizedTest
    @ValueSource(strings = {QuestionTaxonomyConstants.Difficulty.EASY, "easy", "Easy"})
    @DisplayName("selectModel() routes EASY difficulty to nova-micro")
    void selectModel_easy_returnsNovaMicro(String difficulty) {
        assertThat(modelRouter.selectModel(difficulty)).isEqualTo("nova-micro");
    }

    @ParameterizedTest
    @ValueSource(strings = {QuestionTaxonomyConstants.Difficulty.MEDIUM, "medium", "Medium"})
    @DisplayName("selectModel() routes MEDIUM difficulty to nova-lite")
    void selectModel_medium_returnsNovaLite(String difficulty) {
        assertThat(modelRouter.selectModel(difficulty)).isEqualTo("nova-lite");
    }

    @ParameterizedTest
    @ValueSource(strings = {QuestionTaxonomyConstants.Difficulty.HARD, "hard", "Hard"})
    @DisplayName("selectModel() routes HARD difficulty to nova-lite")
    void selectModel_hard_returnsNovaLite(String difficulty) {
        assertThat(modelRouter.selectModel(difficulty)).isEqualTo("nova-lite");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("selectModel() returns medium model for null/empty difficulty")
    void selectModel_nullOrEmpty_returnsMediumModel(String difficulty) {
        assertThat(modelRouter.selectModel(difficulty)).isEqualTo("nova-lite");
    }

    @Test
    @DisplayName("selectModel() trims whitespace before matching")
    void selectModel_withWhitespace_matchesCorrectly() {
        assertThat(modelRouter.selectModel("  EASY  ")).isEqualTo("nova-micro");
        assertThat(modelRouter.selectModel("  HARD  ")).isEqualTo("nova-lite");
    }

    @Test
    @DisplayName("selectModel() returns medium model for unknown difficulty")
    void selectModel_unknownDifficulty_returnsMediumModel() {
        assertThat(modelRouter.selectModel("EXTREME")).isEqualTo("nova-lite");
    }

    @Test
    @DisplayName("selectModel() routes to local Ollama models when provider=local")
    void selectModel_whenProviderLocal_routesToLocalModels() {
        modelRouter.setProvider("local");
        assertThat(modelRouter.selectModel(QuestionTaxonomyConstants.Difficulty.EASY)).isEqualTo("llama3.2:1b");
        assertThat(modelRouter.selectModel(QuestionTaxonomyConstants.Difficulty.MEDIUM)).isEqualTo("qwen2.5:1.5b");
        assertThat(modelRouter.selectModel(QuestionTaxonomyConstants.Difficulty.HARD)).isEqualTo("qwen2.5:1.5b");
    }

    @Test
    @DisplayName("selectBatchModelId() returns correct Bedrock model IDs by difficulty")
    void selectBatchModelId_returnCorrectIds() {
        assertThat(modelRouter.selectBatchModelId(QuestionTaxonomyConstants.Difficulty.EASY)).isEqualTo(AMAZON_NOVA_MICRO_V_1_0);
        assertThat(modelRouter.selectBatchModelId(QuestionTaxonomyConstants.Difficulty.MEDIUM)).isEqualTo(AMAZON_NOVA_LITE_V_1_0);
        assertThat(modelRouter.selectBatchModelId(QuestionTaxonomyConstants.Difficulty.HARD)).isEqualTo(AMAZON_NOVA_LITE_V_1_0);
        assertThat(modelRouter.selectBatchModelId(null)).isEqualTo(AMAZON_NOVA_LITE_V_1_0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"gemini", "GEMINI", " Gemini ", " gemini "})
    @DisplayName("resolveProvider() returns GEMINI for case-insensitive and trimmed values")
    void resolveProvider_geminiCaseInsensitive(String providerValue) {
        modelRouter.setProvider(providerValue);
        assertThat(modelRouter.resolveProvider()).isEqualTo(ModelRouter.AiProvider.GEMINI);
    }

    @Test
    @DisplayName("selectModel() routes EASY to gemini-flash-lite when provider=gemini")
    void selectModel_gemini_routesEasyToGeminiFlashLite() {
        modelRouter.setProvider("gemini");
        assertThat(modelRouter.selectModel(QuestionTaxonomyConstants.Difficulty.EASY)).isEqualTo("gemini-flash-lite");
        assertThat(modelRouter.selectModel("easy")).isEqualTo("gemini-flash-lite");
    }

    @Test
    @DisplayName("selectModel() routes MEDIUM to gemini-flash when provider=gemini")
    void selectModel_gemini_routesMediumToGeminiFlash() {
        modelRouter.setProvider("gemini");
        assertThat(modelRouter.selectModel(QuestionTaxonomyConstants.Difficulty.MEDIUM)).isEqualTo("gemini-flash");
        assertThat(modelRouter.selectModel("medium")).isEqualTo("gemini-flash");
    }

    @Test
    @DisplayName("selectModel() routes HARD to gemini-pro when provider=gemini")
    void selectModel_gemini_routesHardToGeminiPro() {
        modelRouter.setProvider("gemini");
        assertThat(modelRouter.selectModel(QuestionTaxonomyConstants.Difficulty.HARD)).isEqualTo("gemini-pro");
        assertThat(modelRouter.selectModel("hard")).isEqualTo("gemini-pro");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "UNKNOWN", "EXTREME"})
    @DisplayName("selectModel() routes null/blank/unknown difficulty to gemini-flash when provider=gemini")
    void selectModel_gemini_nullOrBlankOrUnknown_routesToGeminiFlash(String difficulty) {
        modelRouter.setProvider("gemini");
        assertThat(modelRouter.selectModel(difficulty)).isEqualTo("gemini-flash");
    }

    @Test
    @DisplayName("AI_PROVIDER=gemini routes to Gemini models")
    void selectModel_geminiRoutesCorrectly() {
        modelRouter.setProvider("gemini");
        assertThat(modelRouter.resolveProvider()).isEqualTo(ModelRouter.AiProvider.GEMINI);
        assertThat(modelRouter.selectModel(QuestionTaxonomyConstants.Difficulty.EASY)).isEqualTo("gemini-flash-lite");
        assertThat(modelRouter.selectModel(QuestionTaxonomyConstants.Difficulty.MEDIUM)).isEqualTo("gemini-flash");
        assertThat(modelRouter.selectModel(QuestionTaxonomyConstants.Difficulty.HARD)).isEqualTo("gemini-pro");
    }

    @Test
    @DisplayName("selectModel() honors custom Gemini model overrides")
    void selectModel_customGeminiModelOverrides() {
        modelRouter.setProvider("gemini");
        modelRouter.setGeminiEasyModel("custom-gemini-lite");
        modelRouter.setGeminiMediumModel("custom-gemini-medium");
        modelRouter.setGeminiHardModel("custom-gemini-pro");

        assertThat(modelRouter.selectModel(QuestionTaxonomyConstants.Difficulty.EASY)).isEqualTo("custom-gemini-lite");
        assertThat(modelRouter.selectModel(QuestionTaxonomyConstants.Difficulty.MEDIUM)).isEqualTo("custom-gemini-medium");
        assertThat(modelRouter.selectModel(QuestionTaxonomyConstants.Difficulty.HARD)).isEqualTo("custom-gemini-pro");
        assertThat(modelRouter.selectModel(null)).isEqualTo("custom-gemini-medium");
    }

    @ParameterizedTest
    @ValueSource(strings = {"local", "LOCAL", " Local ", "ollama", "OLLAMA", " ollama "})
    @DisplayName("AI_PROVIDER=local or ollama routes to Ollama models")
    void selectModel_whenProviderLocal_routesToLocalModels(String providerValue) {
        modelRouter.setProvider(providerValue);
        assertThat(modelRouter.resolveProvider()).isEqualTo(ModelRouter.AiProvider.LOCAL);
        assertThat(modelRouter.selectModel(QuestionTaxonomyConstants.Difficulty.EASY)).isEqualTo("llama3.2:1b");
        assertThat(modelRouter.selectModel(QuestionTaxonomyConstants.Difficulty.MEDIUM)).isEqualTo("qwen2.5:1.5b");
        assertThat(modelRouter.selectModel(QuestionTaxonomyConstants.Difficulty.HARD)).isEqualTo("qwen2.5:1.5b");
        assertThat(modelRouter.selectModel(null)).isEqualTo("qwen2.5:1.5b");
        assertThat(modelRouter.selectModel("UNKNOWN")).isEqualTo("qwen2.5:1.5b");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "bedrock", "BEDROCK", " Bedrock "})
    @DisplayName("AI_PROVIDER=bedrock, null, or empty defaults to Bedrock models")
    void selectModel_defaultBedrock_routesToBedrockModels(String providerValue) {
        modelRouter.setProvider(providerValue);
        assertThat(modelRouter.resolveProvider()).isEqualTo(ModelRouter.AiProvider.BEDROCK);
        assertThat(modelRouter.selectModel(QuestionTaxonomyConstants.Difficulty.EASY)).isEqualTo("nova-micro");
        assertThat(modelRouter.selectModel(QuestionTaxonomyConstants.Difficulty.MEDIUM)).isEqualTo("nova-lite");
        assertThat(modelRouter.selectModel(QuestionTaxonomyConstants.Difficulty.HARD)).isEqualTo("nova-lite");
        assertThat(modelRouter.selectModel(null)).isEqualTo("nova-lite");
    }
}
