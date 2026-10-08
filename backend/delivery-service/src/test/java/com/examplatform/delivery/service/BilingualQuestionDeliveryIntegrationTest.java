/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.delivery.service;

import com.examplatform.delivery.dto.QuestionDeliveryDto;
import com.examplatform.delivery.dto.QuestionOptionDeliveryDto;
import com.examplatform.delivery.dto.TranslatedQuestionDeliveryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Bilingual Question Delivery & Fallback Integration Tests (Issue #276 & #277)")
class BilingualQuestionDeliveryIntegrationTest {

    private ExamQuestionDeliveryService service;

    @BeforeEach
    void setUp() {
        service = new ExamQuestionDeliveryService(null, null, null, null, null, null);
    }

    private QuestionDeliveryDto createSampleQuestion() {
        Map<String, TranslatedQuestionDeliveryDto> translations = new HashMap<>();
        translations.put("hi", TranslatedQuestionDeliveryDto.builder()
                .languageCode("hi")
                .content("भारत की राजधानी क्या है?")
                .options(List.of(
                        QuestionOptionDeliveryDto.builder().id("A").index(0).originalIndex(0).text("नई दिल्ली").build(),
                        QuestionOptionDeliveryDto.builder().id("B").index(1).originalIndex(1).text("मुंबई").build()
                ))
                .build());

        return QuestionDeliveryDto.builder()
                .id("q-001")
                .text("What is the capital of India?")
                .options(List.of(
                        QuestionOptionDeliveryDto.builder().id("A").index(0).originalIndex(0).text("New Delhi").build(),
                        QuestionOptionDeliveryDto.builder().id("B").index(1).originalIndex(1).text("Mumbai").build()
                ))
                .translations(translations)
                .build();
    }

    @Test
    @DisplayName("Should deliver question bilingually when candidate's preferred language has approved translation")
    void shouldDeliverBilingualWhenTranslationAvailable() {
        List<QuestionDeliveryDto> questions = new ArrayList<>(List.of(createSampleQuestion()));

        service.applyLanguagePreference(questions, "hi");

        QuestionDeliveryDto q = questions.get(0);
        assertThat(q.getPrimaryLanguage()).isEqualTo("hi");
        assertThat(q.getFallbackToEnglish()).isFalse();
        assertThat(q.getPrimaryTranslation()).isNotNull();
        assertThat(q.getPrimaryTranslation().getContent()).isEqualTo("भारत की राजधानी क्या है?");
        // Baseline English content remains intact
        assertThat(q.getText()).isEqualTo("What is the capital of India?");
        assertThat(q.getOptions()).hasSize(2);
    }

    @Test
    @DisplayName("Should gracefully fall back to English when translation for preferred language is missing")
    void shouldFallbackToEnglishWhenTranslationMissing() {
        List<QuestionDeliveryDto> questions = new ArrayList<>(List.of(createSampleQuestion()));

        // Candidate requests Tamil ("ta"), but question only has "hi"
        service.applyLanguagePreference(questions, "ta");

        QuestionDeliveryDto q = questions.get(0);
        assertThat(q.getPrimaryLanguage()).isEqualTo("ta");
        assertThat(q.getFallbackToEnglish()).isTrue();
        assertThat(q.getPrimaryTranslation()).isNull();
        // Baseline English is served cleanly without error
        assertThat(q.getText()).isEqualTo("What is the capital of India?");
        assertThat(q.getOptions().get(0).getText()).isEqualTo("New Delhi");
    }

    @Test
    @DisplayName("Should deliver English baseline cleanly without fallback flag when candidate selects English")
    void shouldDeliverEnglishBaselineCleanly() {
        List<QuestionDeliveryDto> questions = new ArrayList<>(List.of(createSampleQuestion()));

        service.applyLanguagePreference(questions, "en");

        QuestionDeliveryDto q = questions.get(0);
        assertThat(q.getPrimaryLanguage()).isEqualTo("en");
        assertThat(q.getFallbackToEnglish()).isFalse();
        assertThat(q.getPrimaryTranslation()).isNull();
        assertThat(q.getText()).isEqualTo("What is the capital of India?");
    }
}
