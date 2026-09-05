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
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.translation.domain.Translation;
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

    @Mock
    private TranslationRepository translationRepository;

    @Mock
    private QuestionRepository questionRepository;

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

    @Test
    @DisplayName("Should successfully request a translation when language is valid and question exists")
    void shouldRequestTranslationSuccessfully() {
        Question mockQuestion = Question.builder().build();
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(mockQuestion));
        when(translationRepository.findByQuestionIdAndLanguageCodeAndTenantId(questionId, "hi", tenantId))
                .thenReturn(Collections.emptyList());
        when(translationRepository.save(any(Translation.class))).thenAnswer(i -> i.getArgument(0));

        Translation result = translationWorkflowService.requestTranslation(questionId, "hi", translatorId, tenantId);

        assertThat(result).isNotNull();
        assertThat(result.getQuestionId()).isEqualTo(questionId);
        assertThat(result.getLanguageCode()).isEqualTo("hi");
        assertThat(result.getStatus()).isEqualTo(Translation.TranslationStatus.DRAFT);
        assertThat(result.getTranslatorId()).isEqualTo(translatorId);
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        verify(translationRepository).save(any(Translation.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when language is unsupported")
    void shouldThrowWhenLanguageUnsupported() {
        assertThatThrownBy(() -> translationWorkflowService.requestTranslation(questionId, "invalid_lang", translatorId, tenantId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported language code");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when source question does not exist")
    void shouldThrowWhenQuestionNotFound() {
        when(questionRepository.findById(questionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> translationWorkflowService.requestTranslation(questionId, "hi", translatorId, tenantId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source question not found");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when translation already exists")
    void shouldThrowWhenTranslationAlreadyExists() {
        Question mockQuestion = Question.builder().build();
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(mockQuestion));
        when(translationRepository.findByQuestionIdAndLanguageCodeAndTenantId(questionId, "hi", tenantId))
                .thenReturn(List.of(Translation.builder().build()));

        assertThatThrownBy(() -> translationWorkflowService.requestTranslation(questionId, "hi", translatorId, tenantId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Translation already exists");
    }
}
