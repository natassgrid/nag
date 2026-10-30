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
import com.examplatform.questionbank.microlesson.repository.SubtopicMicroLessonRepository;
import com.examplatform.questionbank.microlesson.translation.domain.MicroLessonBatchTranslationJob;
import com.examplatform.questionbank.microlesson.translation.domain.MicroLessonTranslation;
import com.examplatform.questionbank.microlesson.translation.repository.MicroLessonBatchTranslationJobRepository;
import com.examplatform.questionbank.microlesson.translation.repository.MicroLessonTranslationRepository;
import com.examplatform.questionbank.microlesson.translation.service.AsyncMicroLessonBatchTranslationWorker;
import com.examplatform.questionbank.microlesson.translation.service.MicroLessonTranslationPayloadService;
import com.examplatform.questionbank.microlesson.translation.service.MicroLessonTranslationWorkflowService;
import com.examplatform.shared.constants.SharedConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncMicroLessonBatchTranslationWorkerTest {

    @Mock
    private MicroLessonBatchTranslationJobRepository jobRepository;

    @Mock
    private SubtopicMicroLessonRepository lessonRepository;

    @Mock
    private MicroLessonTranslationRepository translationRepository;

    @Mock
    private MicroLessonTranslationPayloadService payloadService;

    @Mock
    private MicroLessonTranslationWorkflowService workflowService;

    private AsyncMicroLessonBatchTranslationWorker worker;

    private UUID jobId;

    @BeforeEach
    void setUp() {
        worker = new AsyncMicroLessonBatchTranslationWorker(
                jobRepository, lessonRepository, translationRepository, payloadService, workflowService);
        jobId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should process explicit micro-lessons with bounded concurrency and complete job")
    void shouldProcessExplicitLessonBatchWithBoundedConcurrency() {
        UUID lesson1 = UUID.randomUUID();
        UUID lesson2 = UUID.randomUUID();

        MicroLessonBatchTranslationJob job = MicroLessonBatchTranslationJob.builder()
                .status("PENDING")
                .targetLanguage("hi")
                .targetStatus("PUBLISHED")
                .lessonIds(List.of(lesson1, lesson2))
                .batchSize(10)
                .throttleDelayMs(0)
                .maxConcurrency(2)
                .overwriteExisting(true)
                .build();
        ReflectionTestUtils.setField(job, "id", jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        SubtopicMicroLesson m1 = SubtopicMicroLesson.builder().coreConcept("Concept 1").contentVersion(1).build();
        ReflectionTestUtils.setField(m1, "id", lesson1);
        SubtopicMicroLesson m2 = SubtopicMicroLesson.builder().coreConcept("Concept 2").contentVersion(1).build();
        ReflectionTestUtils.setField(m2, "id", lesson2);

        when(lessonRepository.findById(lesson1)).thenReturn(Optional.of(m1));
        when(lessonRepository.findById(lesson2)).thenReturn(Optional.of(m2));

        MicroLessonTranslationPayloadService.TranslatedMicroLessonPayload mockPayload =
                new MicroLessonTranslationPayloadService.TranslatedMicroLessonPayload(
                        "अवधारणा", "जाल", "[]", "{}", List.of(), null);
        when(payloadService.translateMicroLesson(any(), eq("hi"))).thenReturn(mockPayload);

        worker.processBatchTranslationJob(jobId, "default");

        verify(workflowService, times(2)).upsertTranslation(
                any(), eq("hi"), any(), any(), any(), any(), eq("PUBLISHED"), any(), any(), any(), eq(1), eq("default"));
        verify(jobRepository, times(2)).incrementSuccess(jobId);
        assertThat(job.getStatus()).isEqualTo(SharedConstants.COMPLETED);
        assertThat(job.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should isolate individual lesson failures without aborting the batch job")
    void shouldIsolateIndividualLessonFailuresWithoutAbortingBatch() {
        UUID lesson1 = UUID.randomUUID();
        UUID lesson2 = UUID.randomUUID();

        MicroLessonBatchTranslationJob job = MicroLessonBatchTranslationJob.builder()
                .status("PENDING")
                .targetLanguage("hi")
                .lessonIds(List.of(lesson1, lesson2))
                .batchSize(10)
                .throttleDelayMs(0)
                .maxConcurrency(1)
                .overwriteExisting(true)
                .build();
        ReflectionTestUtils.setField(job, "id", jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        SubtopicMicroLesson m1 = SubtopicMicroLesson.builder().coreConcept("Concept 1").contentVersion(1).build();
        ReflectionTestUtils.setField(m1, "id", lesson1);
        SubtopicMicroLesson m2 = SubtopicMicroLesson.builder().coreConcept("Concept 2").contentVersion(1).build();
        ReflectionTestUtils.setField(m2, "id", lesson2);

        when(lessonRepository.findById(lesson1)).thenReturn(Optional.of(m1));
        when(lessonRepository.findById(lesson2)).thenReturn(Optional.of(m2));

        // Lesson 1 throws translation error, Lesson 2 succeeds
        when(payloadService.translateMicroLesson(m1, "hi")).thenThrow(new RuntimeException("IndicTrans2 timeout"));
        MicroLessonTranslationPayloadService.TranslatedMicroLessonPayload mockPayload =
                new MicroLessonTranslationPayloadService.TranslatedMicroLessonPayload(
                        "अवधारणा 2", "जाल 2", "[]", "{}", List.of(), null);
        when(payloadService.translateMicroLesson(m2, "hi")).thenReturn(mockPayload);

        worker.processBatchTranslationJob(jobId, "default");

        // Failure incremented for lesson 1
        verify(jobRepository).incrementFailure(eq(jobId), eq("[\"" + lesson1 + "\"]"));
        // Success incremented for lesson 2
        verify(jobRepository).incrementSuccess(jobId);
        // Batch completed successfully despite individual failure
        assertThat(job.getStatus()).isEqualTo(SharedConstants.COMPLETED);
    }

    @Test
    @DisplayName("Should skip AI translation when overwriteExisting is false and translation already exists")
    void shouldSkipTranslationWhenOverwriteExistingIsFalse() {
        UUID lessonId = UUID.randomUUID();

        MicroLessonBatchTranslationJob job = MicroLessonBatchTranslationJob.builder()
                .status("PENDING")
                .targetLanguage("hi")
                .lessonIds(List.of(lessonId))
                .batchSize(10)
                .throttleDelayMs(0)
                .maxConcurrency(1)
                .overwriteExisting(false)
                .build();
        ReflectionTestUtils.setField(job, "id", jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        SubtopicMicroLesson m1 = SubtopicMicroLesson.builder().coreConcept("Concept 1").contentVersion(1).build();
        ReflectionTestUtils.setField(m1, "id", lessonId);
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(m1));

        MicroLessonTranslation existing = MicroLessonTranslation.builder().coreConcept("मौजूदा").build();
        when(translationRepository.findByMicrolessonIdAndLanguageCodeAndTenantId(lessonId, "hi", "default"))
                .thenReturn(Optional.of(existing));

        worker.processBatchTranslationJob(jobId, "default");

        // AI payload service must NOT be called
        verifyNoInteractions(payloadService);
        verifyNoInteractions(workflowService);
        // Direct success increment
        verify(jobRepository).incrementSuccess(jobId);
        assertThat(job.getStatus()).isEqualTo(SharedConstants.COMPLETED);
    }

    @Test
    @DisplayName("Should halt processing when job is cancelled mid-batch")
    void shouldHaltWhenJobCancelled() {
        UUID lesson1 = UUID.randomUUID();
        UUID lesson2 = UUID.randomUUID();

        MicroLessonBatchTranslationJob job = MicroLessonBatchTranslationJob.builder()
                .status("PENDING")
                .targetLanguage("hi")
                .lessonIds(List.of(lesson1, lesson2))
                .batchSize(1)
                .throttleDelayMs(0)
                .maxConcurrency(1)
                .build();
        ReflectionTestUtils.setField(job, "id", jobId);

        MicroLessonBatchTranslationJob cancelledJob = MicroLessonBatchTranslationJob.builder()
                .status(SharedConstants.CANCELLED)
                .build();
        ReflectionTestUtils.setField(cancelledJob, "id", jobId);

        when(jobRepository.findById(jobId))
                .thenReturn(Optional.of(job)) // initial load
                .thenReturn(Optional.of(cancelledJob)); // cancellation check

        worker.processBatchTranslationJob(jobId, "default");

        verifyNoInteractions(payloadService);
        verify(jobRepository, never()).incrementSuccess(any());
    }
}
