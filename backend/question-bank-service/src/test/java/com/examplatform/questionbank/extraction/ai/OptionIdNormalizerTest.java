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
package com.examplatform.questionbank.extraction.ai;

import com.examplatform.questionbank.dto.QuestionOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OptionIdNormalizer Unit Tests")
class OptionIdNormalizerTest {

    @ParameterizedTest(name = "token ''{0}'' normalizes to ''{1}''")
    @CsvSource({
            "1, A",
            "(1), A",
            "1., A",
            "[1], A",
            "a, A",
            "(a), A",
            "A, A",
            "i, A",
            "(i), A",
            "I, A",
            "क, A",
            "(क), A",
            "[क], A",
            "अ, A",
            "(अ), A",
            "2, B",
            "(2), B",
            "2., B",
            "b, B",
            "(b), B",
            "B, B",
            "ii, B",
            "(ii), B",
            "II, B",
            "ख, B",
            "(ख), B",
            "ब, B",
            "(ब), B",
            "3, C",
            "(3), C",
            "3., C",
            "c, C",
            "(c), C",
            "C, C",
            "iii, C",
            "(iii), C",
            "III, C",
            "ग, C",
            "(ग), C",
            "स, C",
            "(स), C",
            "4, D",
            "(4), D",
            "4., D",
            "d, D",
            "(d), D",
            "D, D",
            "iv, D",
            "(iv), D",
            "IV, D",
            "घ, D",
            "(घ), D",
            "द, D",
            "(द), D"
    })
    void testNormalizeAllTokenVariants(String inputToken, String expectedCanonical) {
        String canonical = OptionIdNormalizer.normalize(inputToken);
        assertThat(canonical).isEqualTo(expectedCanonical);
    }

    @Test
    @DisplayName("Positional assignment when Hindi options have null or blank IDs")
    void testPositionalAssignmentWhenHindiOptionsLackIds() {
        var item = LlmPageExtractionResult.ExtractedItemPayload.builder()
                .questionNumber(1)
                .options(new ArrayList<>(List.of(
                        QuestionOption.builder().id("A").text("First English").build(),
                        QuestionOption.builder().id("B").text("Second English").build(),
                        QuestionOption.builder().id("C").text("Third English").build(),
                        QuestionOption.builder().id("D").text("Fourth English").build()
                )))
                .hindiOptions(new ArrayList<>(List.of(
                        QuestionOption.builder().id(null).text("पहला हिंदी").build(),
                        QuestionOption.builder().id("").text("दूसरा हिंदी").build(),
                        QuestionOption.builder().id(" ").text("तीसरा हिंदी").build(),
                        QuestionOption.builder().id(null).text("चौथा हिंदी").build()
                )))
                .build();

        OptionIdNormalizer.normalizeQuestionOptions(item);

        assertThat(item.getHindiOptions()).extracting(QuestionOption::getId)
                .containsExactly("A", "B", "C", "D");
    }

    @Test
    @DisplayName("Devanagari (क, ख, ग, घ) options in Hindi aligned with English (A, B, C, D)")
    void testDevanagariHindiOptionsAlignedWithEnglish() {
        var item = LlmPageExtractionResult.ExtractedItemPayload.builder()
                .questionNumber(2)
                .options(new ArrayList<>(List.of(
                        QuestionOption.builder().id("(a)").text("Option 1").build(),
                        QuestionOption.builder().id("(b)").text("Option 2").build(),
                        QuestionOption.builder().id("(c)").text("Option 3").build(),
                        QuestionOption.builder().id("(d)").text("Option 4").build()
                )))
                .hindiOptions(new ArrayList<>(List.of(
                        QuestionOption.builder().id("(क)").text("विकल्प 1").build(),
                        QuestionOption.builder().id("(ख)").text("विकल्प 2").build(),
                        QuestionOption.builder().id("(ग)").text("विकल्प 3").build(),
                        QuestionOption.builder().id("(घ)").text("विकल्प 4").build()
                )))
                .answerKey("(ख)")
                .build();

        OptionIdNormalizer.normalizeQuestionOptions(item);

        assertThat(item.getOptions()).extracting(QuestionOption::getId)
                .containsExactly("A", "B", "C", "D");
        assertThat(item.getHindiOptions()).extracting(QuestionOption::getId)
                .containsExactly("A", "B", "C", "D");
        assertThat(item.getAnswerKey()).isEqualTo("B");
        assertThat(item.getOptions().get(1).isCorrect()).isTrue();
        assertThat(item.getHindiOptions().get(1).isCorrect()).isTrue();
    }

    @Test
    @DisplayName("Answer key is normalized and synchronized with isCorrect flag")
    void testAnswerKeyNormalizationAndFlagSync() {
        var item = LlmPageExtractionResult.ExtractedItemPayload.builder()
                .questionNumber(3)
                .options(new ArrayList<>(List.of(
                        QuestionOption.builder().id("1").text("First").build(),
                        QuestionOption.builder().id("2").text("Second").build(),
                        QuestionOption.builder().id("3").text("Third").build(),
                        QuestionOption.builder().id("4").text("Fourth").build()
                )))
                .answerKey("3")
                .build();

        OptionIdNormalizer.normalizeQuestionOptions(item);

        assertThat(item.getAnswerKey()).isEqualTo("C");
        assertThat(item.getOptions().get(2).isCorrect()).isTrue();
        assertThat(item.getOptions().get(0).isCorrect()).isFalse();
    }
}
