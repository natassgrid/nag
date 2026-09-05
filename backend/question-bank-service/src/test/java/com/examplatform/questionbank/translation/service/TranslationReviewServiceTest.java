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

import com.examplatform.questionbank.translation.domain.Translation;
import com.examplatform.questionbank.translation.repository.TranslationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslationReviewServiceTest {

    @Mock
    private TranslationRepository translationRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private TranslationReviewService translationReviewService;

    private UUID translationId;
    private UUID reviewerId;
    private UUID translatorId;
    private UUID questionId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        translationId = UUID.randomUUID();
        reviewerId = UUID.randomUUID();
        translatorId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        tenantId = "tenant-test";
    }

    private void setEntityId(Translation translation, UUID id) {
        try {
            Field idField = translation.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(translation, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Should approve DRAFT translation successfully")
    void shouldApproveDraftTranslation() {
        Translation draft = Translation.builder()
                .questionId(questionId)
                .languageCode("ta")
                .status(Translation.TranslationStatus.DRAFT)
                .translatorId(translatorId)
                .build();
        setEntityId(draft, translationId);

        when(translationRepository.findById(translationId)).thenReturn(Optional.of(draft));
        when(translationRepository.save(any(Translation.class))).thenAnswer(i -> i.getArgument(0));

        Translation result = translationReviewService.approve(translationId, reviewerId, tenantId);

        assertThat(result.getStatus()).isEqualTo(Translation.TranslationStatus.APPROVED);
        assertThat(result.getReviewerId()).isEqualTo(reviewerId);
        verify(translationRepository).save(draft);
    }

    @Test
    @DisplayName("Should reject DRAFT translation and send Kafka notification")
    void shouldRejectDraftTranslation() {
        Translation draft = Translation.builder()
                .questionId(questionId)
                .languageCode("hi")
                .status(Translation.TranslationStatus.DRAFT)
                .translatorId(translatorId)
                .build();
        setEntityId(draft, translationId);

        when(translationRepository.findById(translationId)).thenReturn(Optional.of(draft));
        when(translationRepository.save(any(Translation.class))).thenAnswer(i -> i.getArgument(0));

        translationReviewService.reject(translationId, reviewerId, "Needs correction in terminology", tenantId);

        assertThat(draft.getReviewerId()).isEqualTo(reviewerId);
        assertThat(draft.getReviewComments()).isEqualTo("Needs correction in terminology");
        verify(kafkaTemplate).send(eq("exam.translation.events"), eq(translationId.toString()), any());
    }

    @Test
    @DisplayName("Should mark approved translations as STALE and send event")
    void shouldMarkApprovedTranslationsStale() {
        Translation approved = Translation.builder()
                .questionId(questionId)
                .languageCode("hi")
                .status(Translation.TranslationStatus.APPROVED)
                .translatorId(translatorId)
                .build();
        setEntityId(approved, translationId);

        when(translationRepository.findByQuestionIdAndStatusAndTenantId(questionId, Translation.TranslationStatus.APPROVED, tenantId))
                .thenReturn(List.of(approved));

        translationReviewService.markStale(questionId, tenantId);

        assertThat(approved.getStatus()).isEqualTo(Translation.TranslationStatus.STALE);
        verify(translationRepository).saveAll(any());
        verify(kafkaTemplate).send(eq("exam.translation.events"), eq(questionId.toString()), any());
    }

    @Test
    @DisplayName("Should do nothing if no approved translations exist when marking stale")
    void shouldDoNothingIfNoApprovedTranslationsWhenMarkingStale() {
        when(translationRepository.findByQuestionIdAndStatusAndTenantId(questionId, Translation.TranslationStatus.APPROVED, tenantId))
                .thenReturn(Collections.emptyList());

        translationReviewService.markStale(questionId, tenantId);
    }
}
