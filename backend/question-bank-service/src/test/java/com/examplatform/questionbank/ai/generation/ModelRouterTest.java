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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ModelRouter}.
 * Validates subject-to-model routing logic.
 */
@DisplayName("ModelRouter")
class ModelRouterTest {

    private final ModelRouter modelRouter = new ModelRouter();

    @ParameterizedTest
    @ValueSource(strings = {"mathematics", "Mathematics", "MATHEMATICS", "general science",
            "General Science", "physics", "Physics", "chemistry", "Chemistry"})
    @DisplayName("selectModel() routes math/science subjects to qwen2-math-1.5b")
    void selectModel_mathAndScience_returnsQwen2Math(String subject) {
        assertThat(modelRouter.selectModel(subject)).isEqualTo(ModelRouter.MODEL_MATH);
    }

    @ParameterizedTest
    @ValueSource(strings = {"general studies", "General Studies", "indian history",
            "Indian History", "indian geography", "Indian Geography",
            "current affairs", "Current Affairs", "sports", "Sports"})
    @DisplayName("selectModel() routes trivia/GK subjects to llama3.2-1b")
    void selectModel_triviaSubjects_returnsLlama(String subject) {
        assertThat(modelRouter.selectModel(subject)).isEqualTo(ModelRouter.MODEL_TRIVIA);
    }

    @ParameterizedTest
    @ValueSource(strings = {"english", "Hindi", "computer science", "Economics",
            "Political Science", "unknown subject"})
    @DisplayName("selectModel() routes other subjects to qwen2.5-1.5b")
    void selectModel_otherSubjects_returnsGeneralModel(String subject) {
        assertThat(modelRouter.selectModel(subject)).isEqualTo(ModelRouter.MODEL_GENERAL);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("selectModel() returns general model for null/empty subject")
    void selectModel_nullOrEmpty_returnsGeneralModel(String subject) {
        assertThat(modelRouter.selectModel(subject)).isEqualTo(ModelRouter.MODEL_GENERAL);
    }

    @Test
    @DisplayName("selectModel() trims whitespace before matching")
    void selectModel_withWhitespace_matchesCorrectly() {
        assertThat(modelRouter.selectModel("  mathematics  ")).isEqualTo(ModelRouter.MODEL_MATH);
        assertThat(modelRouter.selectModel("  sports  ")).isEqualTo(ModelRouter.MODEL_TRIVIA);
    }

    @Test
    @DisplayName("selectModel() returns general model for blank string")
    void selectModel_blankString_returnsGeneralModel() {
        assertThat(modelRouter.selectModel("   ")).isEqualTo(ModelRouter.MODEL_GENERAL);
    }
}
