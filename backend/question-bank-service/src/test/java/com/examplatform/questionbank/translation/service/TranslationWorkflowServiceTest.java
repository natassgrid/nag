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

package com.examplatform.questionbank.translation.service;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.translation.domain.Translation;
import com.examplatform.questionbank.translation.dto.TranslatedOptionDto;
import com.examplatform.questionbank.translation.dto.TranslationRequest;
import com.examplatform.questionbank.translation.repository.TranslationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslationWorkflowServiceTest {

    @Mock private TranslationRepository translationRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private TranslationPayloadService payloadService;

    @InjectMocks
    private TranslationWorkflowService translationWorkflowService;

    private UUID questionId;
    private UUID translatorId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        questionId = UUID.randomUUID();
        translatorId = UUID.randomUUID();
        tenantId = "tenant-test";
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private TranslationRequest buildRequest(String lang) {
        TranslationRequest req = new TranslationRequest();
        req.setQuestionId(questionId);
        req.setLanguageCode(lang);
        req.setTranslatorId(translatorId);
        req.setTranslatedContent("नमस्ते दुनिया");
        req.setTranslatedOptions(List.of(
                new TranslatedOptionDto("A", "पहला विकल्प"),
                new TranslatedOptionDto("B", "दूसरा विकल्प")
        ));
        req.setTranslatedExplanation("यह सही उत्तर है क्योंकि...");
        return req;
    }

    private Question buildQuestion() {
        return Question.builder()
                .options(List.of(
                        QuestionOption.builder().id("A").text("Option A").correct(true).build(),
                        QuestionOption.builder().id("B").text("Option B").correct(false).build()
                ))
                .build();
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should successfully create a translation with structured payload")
    void shouldRequestTranslationSuccessfully() {
        TranslationRequest request = buildRequest("hi");
        Question question = buildQuestion();

        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(translationRepository.findByQuestionIdAndLanguageCodeAndTenantId(
                questionId, "hi", tenantId)).thenReturn(Collections.emptyList());
        when(payloadService.serialize(any())).thenReturn("{\"content\":\"नमस्ते दुनिया\"}");
        when(payloadService.isEncryptionEnabled()).thenReturn(false);
        when(translationRepository.save(any(Translation.class))).thenAnswer(i -> i.getArgument(0));

        Translation result = translationWorkflowService.requestTranslation(request, tenantId);

        assertThat(result).isNotNull();
        assertThat(result.getQuestionId()).isEqualTo(questionId);
        assertThat(result.getLanguageCode()).isEqualTo("hi");
        assertThat(result.getStatus()).isEqualTo(Translation.TranslationStatus.DRAFT);
        assertThat(result.getTranslatorId()).isEqualTo(translatorId);
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.isPayloadEncrypted()).isFalse();
        assertThat(result.getTranslatedPayload()).isNotBlank();
        verify(translationRepository).save(any(Translation.class));
        verify(payloadService).serialize(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when language is unsupported")
    void shouldThrowWhenLanguageUnsupported() {
        TranslationRequest request = buildRequest("xx");

        assertThatThrownBy(() -> translationWorkflowService.requestTranslation(request, tenantId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported language code");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when source question does not exist")
    void shouldThrowWhenQuestionNotFound() {
        TranslationRequest request = buildRequest("hi");
        when(questionRepository.findById(questionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> translationWorkflowService.requestTranslation(request, tenantId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source question not found");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when translation already exists")
    void shouldThrowWhenTranslationAlreadyExists() {
        TranslationRequest request = buildRequest("hi");
        Question question = buildQuestion();

        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(translationRepository.findByQuestionIdAndLanguageCodeAndTenantId(
                questionId, "hi", tenantId))
                .thenReturn(List.of(Translation.builder().build()));

        assertThatThrownBy(() -> translationWorkflowService.requestTranslation(request, tenantId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Translation already exists");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when option IDs do not match source question")
    void shouldThrowWhenOptionIdsMismatch() {
        TranslationRequest request = buildRequest("hi");
        // Add an option with ID "Z" that doesn't exist on the source question
        request.setTranslatedOptions(List.of(
                new TranslatedOptionDto("A", "पहला विकल्प"),
                new TranslatedOptionDto("Z", "अज्ञात विकल्प") // invalid id
        ));
        Question question = buildQuestion();

        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(translationRepository.findByQuestionIdAndLanguageCodeAndTenantId(
                questionId, "hi", tenantId)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> translationWorkflowService.requestTranslation(request, tenantId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Z");
    }

    @Test
    @DisplayName("Should allow null/empty options for question types without choices")
    void shouldAllowEmptyOptionsForNonMcqQuestions() {
        TranslationRequest request = buildRequest("ta");
        request.setTranslatedOptions(null); // SHORT_ANSWER — no options
        Question question = Question.builder().options(null).build();

        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(translationRepository.findByQuestionIdAndLanguageCodeAndTenantId(
                questionId, "ta", tenantId)).thenReturn(Collections.emptyList());
        when(payloadService.serialize(any())).thenReturn("{\"content\":\"வணக்கம்\"}");
        when(payloadService.isEncryptionEnabled()).thenReturn(false);
        when(translationRepository.save(any(Translation.class))).thenAnswer(i -> i.getArgument(0));

        Translation result = translationWorkflowService.requestTranslation(request, tenantId);
        assertThat(result).isNotNull();
    }
}
