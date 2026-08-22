/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.examplatform.questionbank.ai.generation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ModelRouter.class)
@TestPropertySource(properties = {
        "app.ai.models.easy=nova-micro",
        "app.ai.models.medium=nova-lite",
        "app.ai.models.hard=nova-lite"
})
@DisplayName("ModelRouter â€” difficulty-based model routing")
class ModelRouterTest {

    @Autowired
    private ModelRouter modelRouter;

    @ParameterizedTest
    @ValueSource(strings = {"EASY", "easy", "Easy"})
    @DisplayName("selectModel() routes EASY difficulty to nova-micro")
    void selectModel_easy_returnsNovaMicro(String difficulty) {
        assertThat(modelRouter.selectModel(difficulty)).isEqualTo("nova-micro");
    }

    @ParameterizedTest
    @ValueSource(strings = {"MEDIUM", "medium", "Medium"})
    @DisplayName("selectModel() routes MEDIUM difficulty to nova-lite")
    void selectModel_medium_returnsNovaLite(String difficulty) {
        assertThat(modelRouter.selectModel(difficulty)).isEqualTo("nova-lite");
    }

    @ParameterizedTest
    @ValueSource(strings = {"HARD", "hard", "Hard"})
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
    @DisplayName("selectBatchModelId() returns correct Bedrock model IDs by difficulty")
    void selectBatchModelId_returnCorrectIds() {
        assertThat(modelRouter.selectBatchModelId("EASY")).isEqualTo("amazon.nova-micro-v1:0");
        assertThat(modelRouter.selectBatchModelId("MEDIUM")).isEqualTo("amazon.nova-lite-v1:0");
        assertThat(modelRouter.selectBatchModelId("HARD")).isEqualTo("amazon.nova-lite-v1:0");
        assertThat(modelRouter.selectBatchModelId(null)).isEqualTo("amazon.nova-lite-v1:0");
    }
}
