/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

package com.examplatform.questionbank.microlesson.translation;

import com.examplatform.questionbank.microlesson.domain.SubtopicMicroLesson;
import com.examplatform.questionbank.microlesson.dto.MicroLessonResponse;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import com.examplatform.questionbank.microlesson.repository.SubtopicMicroLessonRepository;
import com.examplatform.questionbank.microlesson.translation.domain.MicroLessonTranslation;
import com.examplatform.questionbank.microlesson.translation.dto.MicroLessonTranslationResponse;
import com.examplatform.questionbank.microlesson.translation.repository.MicroLessonTranslationRepository;
import com.examplatform.questionbank.microlesson.translation.service.MicroLessonTranslationPayloadService;
import com.examplatform.questionbank.microlesson.translation.service.MicroLessonTranslationWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MicroLessonTranslationWorkflowServiceTest {

    @Mock
    private MicroLessonTranslationRepository translationRepository;

    @Mock
    private SubtopicMicroLessonRepository lessonRepository;

    @Mock
    private MicroLessonTranslationPayloadService payloadService;

    private MicroLessonTranslationWorkflowService workflowService;

    private UUID lessonId;
    private SubtopicMicroLesson sampleLesson;

    @BeforeEach
    void setUp() {
        workflowService = new MicroLessonTranslationWorkflowService(
                translationRepository, lessonRepository, payloadService);

        lessonId = UUID.randomUUID();
        sampleLesson = SubtopicMicroLesson.builder()
                .subjectId(10L)
                .topicId(20L)
                .subtopicId(30L)
                .subject("Mathematics")
                .topic("Calculus")
                .subtopic("Limits")
                .targetExam("JEE_MAIN")
                .coreConcept("Limit definition: $$\\lim_{x \\to a} f(x) = L$$")
                .trap("Direct substitution into indeterminate forms like $$0/0$$")
                .cheatSheetJson("[\"L'Hopital rule: $$\\frac{f'(x)}{g'(x)}$$\"]")
                .stepByStepBreakdownJson("{\"questionStem\": \"Find limit\", \"finalAnswer\": \"1\"}")
                .contentVersion(1)
                .build();
        ReflectionTestUtils.setField(sampleLesson, "id", lessonId);
    }

    @Test
    @DisplayName("Should auto-translate single micro-lesson and save as PUBLISHED")
    void shouldAutoTranslateSingleLessonSuccessfully() {
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(sampleLesson));

        MicroLessonTranslationPayloadService.TranslatedMicroLessonPayload mockPayload =
                new MicroLessonTranslationPayloadService.TranslatedMicroLessonPayload(
                        "सीमा परिभाषा",
                        "अनिर्धार्य रूप",
                        "[\"एल-हॉस्पिटल नियम\"]",
                        "{\"questionStem\": \"सीमा ज्ञात कीजिए\", \"finalAnswer\": \"1\"}",
                        List.of("एल-हॉस्पिटल नियम"),
                        new StepByStepBreakdownDto("सीमा ज्ञात कीजिए", List.of(), "1")
                );

        when(payloadService.translateMicroLesson(sampleLesson, "hi")).thenReturn(mockPayload);
        when(translationRepository.findByMicrolessonIdAndLanguageCodeAndTenantId(lessonId, "hi", "default"))
                .thenReturn(Optional.empty());

        when(translationRepository.save(any(MicroLessonTranslation.class))).thenAnswer(invocation -> {
            MicroLessonTranslation t = invocation.getArgument(0);
            ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
            return t;
        });

        MicroLessonTranslationResponse response = workflowService.autoTranslateSingleLesson(
                lessonId, "hi", UUID.randomUUID(), "default");

        assertThat(response).isNotNull();
        assertThat(response.getCoreConcept()).isEqualTo("सीमा परिभाषा");
        assertThat(response.getTrap()).isEqualTo("अनिर्धार्य रूप");
        assertThat(response.getStatus()).isEqualTo("PUBLISHED");

        verify(translationRepository).save(any(MicroLessonTranslation.class));
    }

    @Test
    @DisplayName("Should idempotently update existing translation record on re-translation without duplicate key errors")
    void shouldUpsertExistingTranslationIdempotently() {
        MicroLessonTranslation existing = MicroLessonTranslation.builder()
                .microlessonId(lessonId)
                .languageCode("hi")
                .coreConcept("Old core concept")
                .trap("Old trap")
                .cheatSheetJson("[]")
                .stepByStepBreakdownJson("{}")
                .sourceVersion(1)
                .status("PUBLISHED")
                .build();
        ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
        existing.setTenantId("default");

        when(translationRepository.findByMicrolessonIdAndLanguageCodeAndTenantId(lessonId, "hi", "default"))
                .thenReturn(Optional.of(existing));
        when(translationRepository.save(any(MicroLessonTranslation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MicroLessonTranslation updated = workflowService.upsertTranslation(
                lessonId,
                "hi",
                "New translated concept",
                "New translated trap",
                "[\"New cheat sheet\"]",
                "{\"steps\": []}",
                "PUBLISHED",
                UUID.randomUUID(),
                null,
                "Updated via re-translate",
                2,
                "default"
        );

        assertThat(updated.getId()).isEqualTo(existing.getId());
        assertThat(updated.getCoreConcept()).isEqualTo("New translated concept");
        assertThat(updated.getSourceVersion()).isEqualTo(2);
        assertThat(updated.getReviewComments()).isEqualTo("Updated via re-translate");
    }

    @Test
    @DisplayName("Should fall back to English when translation does not exist")
    void shouldFallbackToEnglishWhenTranslationNotFound() {
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(sampleLesson));
        when(translationRepository.findByMicrolessonIdAndLanguageCodeAndStatusIn(
                eq(lessonId), eq("te"), anyCollection(), eq("default")))
                .thenReturn(Collections.emptyList());

        MicroLessonResponse response = workflowService.getLocalizedMicroLesson(lessonId, "te", "default");

        assertThat(response).isNotNull();
        assertThat(response.languageCode()).isEqualTo("en");
        assertThat(response.fallbackToEnglish()).isTrue();
        assertThat(response.coreConcept()).isEqualTo(sampleLesson.getCoreConcept());
    }

    @Test
    @DisplayName("Should return localized content when approved translation exists")
    void shouldReturnLocalizedLessonWhenApprovedTranslationExists() {
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(sampleLesson));

        MicroLessonTranslation translation = MicroLessonTranslation.builder()
                .microlessonId(lessonId)
                .languageCode("hi")
                .coreConcept("हिंदी में सीमा सिद्धांत")
                .trap("हिंदी में सामान्य त्रुटि")
                .cheatSheetJson("[\"नियम 1\"]")
                .stepByStepBreakdownJson("{\"questionStem\": \"प्रश्न\"}")
                .status("APPROVED")
                .sourceVersion(1)
                .build();
        ReflectionTestUtils.setField(translation, "id", UUID.randomUUID());

        when(translationRepository.findByMicrolessonIdAndLanguageCodeAndStatusIn(
                eq(lessonId), eq("hi"), anyCollection(), eq("default")))
                .thenReturn(List.of(translation));

        when(payloadService.parseCheatSheet(any())).thenReturn(List.of("नियम 1"));
        when(payloadService.parseBreakdownJson(any())).thenReturn(new StepByStepBreakdownDto("प्रश्न", List.of(), null));

        MicroLessonResponse response = workflowService.getLocalizedMicroLesson(lessonId, "hi", "default");

        assertThat(response).isNotNull();
        assertThat(response.languageCode()).isEqualTo("hi");
        assertThat(response.fallbackToEnglish()).isFalse();
        assertThat(response.coreConcept()).isEqualTo("हिंदी में सीमा सिद्धांत");
    }

    @Test
    @DisplayName("Should approve and reject translations with reviewer comments")
    void shouldApproveAndRejectTranslationWorkflow() {
        UUID transId = UUID.randomUUID();
        MicroLessonTranslation translation = MicroLessonTranslation.builder()
                .microlessonId(lessonId)
                .languageCode("hi")
                .coreConcept("अवधारणा")
                .trap("जाल")
                .status("PUBLISHED")
                .build();
        ReflectionTestUtils.setField(translation, "id", transId);

        when(translationRepository.findById(transId)).thenReturn(Optional.of(translation));
        when(translationRepository.save(any(MicroLessonTranslation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID reviewerId = UUID.randomUUID();

        // 1. Approve
        MicroLessonTranslationResponse approved = workflowService.approveTranslation(
                transId, reviewerId, "Looks accurate and LaTeX verified", "default");
        assertThat(approved.getStatus()).isEqualTo("APPROVED");
        assertThat(approved.getReviewerId()).isEqualTo(reviewerId);
        assertThat(approved.getReviewComments()).isEqualTo("Looks accurate and LaTeX verified");

        // 2. Reject
        MicroLessonTranslationResponse rejected = workflowService.rejectTranslation(
                transId, reviewerId, "Formula symbol was mistranslated", "default");
        assertThat(rejected.getStatus()).isEqualTo("REJECTED");
        assertThat(rejected.getReviewComments()).isEqualTo("Formula symbol was mistranslated");
    }
}
