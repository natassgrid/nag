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

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuestionPromptFormatter Unit Tests")
class QuestionPromptFormatterTest {

    @Test
    @DisplayName("resolveEffectiveDescription prefers rawTextInput when non-blank")
    void testResolveEffectiveDescriptionPrefersRawText() {
        QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                .rawTextInput("Raw text prompt")
                .description("Fallback description")
                .build();

        assertThat(QuestionPromptFormatter.resolveEffectiveDescription(request)).isEqualTo("Raw text prompt");
        assertThat(request.getEffectiveDescription()).isEqualTo("Raw text prompt");
    }

    @Test
    @DisplayName("resolveEffectiveDescription falls back to description when rawTextInput is blank or null")
    void testResolveEffectiveDescriptionFallsBackToDescription() {
        QuestionGenerationRequest requestWithBlankRaw = QuestionGenerationRequest.builder()
                .rawTextInput("   ")
                .description("Fallback description")
                .build();

        assertThat(QuestionPromptFormatter.resolveEffectiveDescription(requestWithBlankRaw))
                .isEqualTo("Fallback description");

        QuestionGenerationRequest requestWithNullRaw = QuestionGenerationRequest.builder()
                .rawTextInput(null)
                .description("Fallback description")
                .build();

        assertThat(QuestionPromptFormatter.resolveEffectiveDescription(requestWithNullRaw))
                .isEqualTo("Fallback description");
    }

    @Test
    @DisplayName("resolveEffectiveDescription returns null when both inputs are blank or null")
    void testResolveEffectiveDescriptionReturnsNullWhenBothBlank() {
        QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                .rawTextInput("   ")
                .description("   ")
                .build();

        assertThat(QuestionPromptFormatter.resolveEffectiveDescription(request)).isNull();
        assertThat(QuestionPromptFormatter.resolveEffectiveDescription(null)).isNull();
    }

    @Test
    @DisplayName("appendGenerationParameters formats parameters with bullet prefix")
    void testAppendGenerationParametersWithPrefix() {
        QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                .subject("Physics")
                .topic("Thermodynamics")
                .subtopic("Carnot Engine")
                .rawTextInput("Explain Carnot cycle efficiency")
                .difficulty("HARD")
                .cognitiveLevel("ANALYZE")
                .questionType("SINGLE_MCQ")
                .targetExam("JEE_ADV")
                .build();

        StringBuilder sb = new StringBuilder();
        QuestionPromptFormatter.appendGenerationParameters(
                sb, request, "- ", "Description/Requirements", "Target Exam");

        String formatted = sb.toString();
        assertThat(formatted).contains("- Subject: Physics\n");
        assertThat(formatted).contains("- Topic: Thermodynamics\n");
        assertThat(formatted).contains("- Subtopic: Carnot Engine\n");
        assertThat(formatted).contains("- Description/Requirements: Explain Carnot cycle efficiency\n");
        assertThat(formatted).contains("- Difficulty: HARD\n");
        assertThat(formatted).contains("- Cognitive Level: ANALYZE\n");
        assertThat(formatted).contains("- Question Type: SINGLE_MCQ\n");
        assertThat(formatted).contains("- Target Exam: JEE_ADV\n");
    }

    @Test
    @DisplayName("appendGenerationParameters formats parameters without prefix and omits optional blank fields")
    void testAppendGenerationParametersWithoutPrefixAndOmittedFields() {
        QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                .subject("Mathematics")
                .topic("Algebra")
                .subtopic(null)
                .rawTextInput(null)
                .description(null)
                .difficulty("EASY")
                .cognitiveLevel("REMEMBER")
                .questionType("NUMERICAL")
                .targetExam(null)
                .build();

        StringBuilder sb = new StringBuilder();
        QuestionPromptFormatter.appendGenerationParameters(
                sb, request, "", "Author Question Description/Prompt", "Target Exam Standard");

        String formatted = sb.toString();
        assertThat(formatted).contains("Subject: Mathematics\n");
        assertThat(formatted).contains("Topic: Algebra\n");
        assertThat(formatted).doesNotContain("Subtopic:");
        assertThat(formatted).doesNotContain("Author Question Description/Prompt:");
        assertThat(formatted).contains("Difficulty: EASY\n");
        assertThat(formatted).contains("Cognitive Level: REMEMBER\n");
        assertThat(formatted).contains("Question Type: NUMERICAL\n");
        assertThat(formatted).doesNotContain("Target Exam Standard:");
    }
}
