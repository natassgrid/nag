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

import com.examplatform.questionbank.translation.domain.BatchTranslationJob;
import com.examplatform.questionbank.translation.domain.BatchTranslationJobStatus;
import com.examplatform.questionbank.translation.dto.BatchTranslationJobResponse;
import com.examplatform.questionbank.translation.dto.BatchTranslationRequest;
import com.examplatform.questionbank.translation.repository.BatchTranslationJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchTranslationServiceTest {

    @Mock
    private BatchTranslationJobRepository jobRepository;

    @Mock
    private AsyncBatchTranslationWorker asyncWorker;

    @InjectMocks
    private BatchTranslationService batchTranslationService;

    private UUID userId;
    private String tenantId;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tenantId = "test-tenant";
        jobId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should start batch translation job successfully and trigger async worker")
    void shouldStartBatchJobSuccessfully() {
        BatchTranslationRequest request = BatchTranslationRequest.builder()
                .sourceLanguage("en")
                .targetLanguage("hi")
                .targetStatus("PUBLISHED")
                .batchSize(25)
                .throttleDelayMs(30)
                .maxConcurrency(2)
                .build();

        when(jobRepository.save(any(BatchTranslationJob.class))).thenAnswer(invocation -> {
            BatchTranslationJob job = invocation.getArgument(0);
            ReflectionTestUtils.setField(job, "id", jobId);
            return job;
        });

        BatchTranslationJobResponse response = batchTranslationService.startBatchJob(request, userId, tenantId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(jobId);
        assertThat(response.getStatus()).isEqualTo(BatchTranslationJobStatus.PENDING);
        assertThat(response.getTargetLanguage()).isEqualTo("hi");
        assertThat(response.getTargetStatus()).isEqualTo("PUBLISHED");

        verify(jobRepository).save(any(BatchTranslationJob.class));
        verify(asyncWorker).processBatchTranslationJob(jobId, tenantId);
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when target language is unsupported")
    void shouldThrowWhenUnsupportedLanguage() {
        BatchTranslationRequest request = BatchTranslationRequest.builder()
                .targetLanguage("invalid-lang")
                .build();

        assertThatThrownBy(() -> batchTranslationService.startBatchJob(request, userId, tenantId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unsupported target language");
    }

    @Test
    @DisplayName("Should retrieve job status with computed progress percentage")
    void shouldGetJobStatusWithProgress() {
        BatchTranslationJob job = BatchTranslationJob.builder()
                .status(BatchTranslationJobStatus.IN_PROGRESS)
                .totalQuestions(100)
                .processedQuestions(50)
                .successfulQuestions(48)
                .failedQuestions(2)
                .sourceLanguage("en")
                .targetLanguage("hi")
                .targetStatus("PUBLISHED")
                .build();
        ReflectionTestUtils.setField(job, "id", jobId);
        job.setTenantId(tenantId);

        when(jobRepository.findByIdAndTenantId(jobId, tenantId)).thenReturn(Optional.of(job));

        BatchTranslationJobResponse response = batchTranslationService.getJobStatus(jobId, tenantId);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BatchTranslationJobStatus.IN_PROGRESS);
        assertThat(response.getProgressPercentage()).isEqualTo(50.0);
        assertThat(response.getProcessedQuestions()).isEqualTo(50);
        assertThat(response.getSuccessfulQuestions()).isEqualTo(48);
        assertThat(response.getFailedQuestions()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should list all batch translation jobs for tenant")
    void shouldListJobsForTenant() {
        BatchTranslationJob job = BatchTranslationJob.builder()
                .status(BatchTranslationJobStatus.COMPLETED)
                .totalQuestions(20)
                .processedQuestions(20)
                .successfulQuestions(20)
                .build();
        ReflectionTestUtils.setField(job, "id", jobId);
        job.setTenantId(tenantId);

        when(jobRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(List.of(job));

        List<BatchTranslationJobResponse> responses = batchTranslationService.listJobs(tenantId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("Should cancel running batch job")
    void shouldCancelRunningJob() {
        BatchTranslationJob job = BatchTranslationJob.builder()
                .status(BatchTranslationJobStatus.IN_PROGRESS)
                .build();
        ReflectionTestUtils.setField(job, "id", jobId);
        job.setTenantId(tenantId);

        when(jobRepository.findByIdAndTenantId(jobId, tenantId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(BatchTranslationJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BatchTranslationJobResponse response = batchTranslationService.cancelJob(jobId, tenantId);

        assertThat(response.getStatus()).isEqualTo(BatchTranslationJobStatus.CANCELLED);
        assertThat(response.getCompletedAt()).isNotNull();
    }
}
