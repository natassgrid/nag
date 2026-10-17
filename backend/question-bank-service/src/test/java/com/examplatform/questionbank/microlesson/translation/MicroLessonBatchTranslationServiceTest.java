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

import com.examplatform.questionbank.microlesson.translation.domain.MicroLessonBatchTranslationJob;
import com.examplatform.questionbank.microlesson.translation.dto.MicroLessonBatchTranslationJobResponse;
import com.examplatform.questionbank.microlesson.translation.dto.MicroLessonBatchTranslationRequest;
import com.examplatform.questionbank.microlesson.translation.repository.MicroLessonBatchTranslationJobRepository;
import com.examplatform.questionbank.microlesson.translation.service.AsyncMicroLessonBatchTranslationWorker;
import com.examplatform.questionbank.microlesson.translation.service.MicroLessonBatchTranslationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MicroLessonBatchTranslationServiceTest {

    @Mock
    private MicroLessonBatchTranslationJobRepository jobRepository;

    @Mock
    private AsyncMicroLessonBatchTranslationWorker asyncWorker;

    private MicroLessonBatchTranslationService batchService;

    @BeforeEach
    void setUp() {
        batchService = new MicroLessonBatchTranslationService(jobRepository, asyncWorker);
    }

    @Test
    @DisplayName("Should start batch job and trigger async background worker")
    void shouldStartBatchJobAndTriggerAsyncWorker() {
        UUID lessonId = UUID.randomUUID();
        MicroLessonBatchTranslationRequest request = MicroLessonBatchTranslationRequest.builder()
                .subject("Physics")
                .topic("Mechanics")
                .targetExam("JEE_MAIN")
                .targetLanguage("hi")
                .lessonIds(List.of(lessonId))
                .batchSize(20)
                .throttleDelayMs(10)
                .maxConcurrency(2)
                .build();

        UUID initiatedBy = UUID.randomUUID();

        when(jobRepository.save(any(MicroLessonBatchTranslationJob.class))).thenAnswer(invocation -> {
            MicroLessonBatchTranslationJob job = invocation.getArgument(0);
            ReflectionTestUtils.setField(job, "id", UUID.randomUUID());
            return job;
        });

        MicroLessonBatchTranslationJobResponse response = batchService.startBatchJob(request, initiatedBy, "default");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getJobId()).isEqualTo(response.getId());
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getTargetLanguage()).isEqualTo("hi");
        assertThat(response.getSubjectFilter()).isEqualTo("Physics");

        verify(jobRepository).save(any(MicroLessonBatchTranslationJob.class));
        verify(asyncWorker).processBatchTranslationJob(eq(response.getId()), eq("default"));
    }

    @Test
    @DisplayName("Should reject unsupported target language code with BAD_REQUEST")
    void shouldRejectUnsupportedLanguageCode() {
        MicroLessonBatchTranslationRequest request = MicroLessonBatchTranslationRequest.builder()
                .targetLanguage("unsupported_lang")
                .build();

        assertThatThrownBy(() -> batchService.startBatchJob(request, UUID.randomUUID(), "default"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unsupported target language code");

        verifyNoInteractions(asyncWorker);
    }

    @Test
    @DisplayName("Should query batch job status and calculate progress percentage accurately")
    void shouldGetBatchJobStatusAndCalculateProgress() {
        UUID jobId = UUID.randomUUID();
        MicroLessonBatchTranslationJob job = MicroLessonBatchTranslationJob.builder()
                .status("IN_PROGRESS")
                .targetLanguage("bn")
                .totalLessons(50)
                .processedLessons(25)
                .successfulLessons(24)
                .failedLessons(1)
                .build();
        ReflectionTestUtils.setField(job, "id", jobId);
        job.setTenantId("default");

        when(jobRepository.findByIdAndTenantId(jobId, "default")).thenReturn(Optional.of(job));

        MicroLessonBatchTranslationJobResponse response = batchService.getJobStatus(jobId, "default");

        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(response.getTotalLessons()).isEqualTo(50);
        assertThat(response.getProcessedLessons()).isEqualTo(25);
        assertThat(response.getProgressPercentage()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Should cancel a running batch job successfully")
    void shouldCancelRunningBatchJob() {
        UUID jobId = UUID.randomUUID();
        MicroLessonBatchTranslationJob job = MicroLessonBatchTranslationJob.builder()
                .status("IN_PROGRESS")
                .build();
        ReflectionTestUtils.setField(job, "id", jobId);
        job.setTenantId("default");

        when(jobRepository.findByIdAndTenantId(jobId, "default")).thenReturn(Optional.of(job));
        when(jobRepository.save(any(MicroLessonBatchTranslationJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MicroLessonBatchTranslationJobResponse response = batchService.cancelJob(jobId, "default");

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        assertThat(response.getCompletedAt()).isNotNull();
        verify(jobRepository).save(job);
    }
}
