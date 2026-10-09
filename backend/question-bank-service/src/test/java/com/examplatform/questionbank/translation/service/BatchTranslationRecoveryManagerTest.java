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
import com.examplatform.questionbank.translation.repository.BatchTranslationJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchTranslationRecoveryManagerTest {

    @Mock
    private BatchTranslationJobRepository jobRepository;

    @Mock
    private AsyncBatchTranslationWorker asyncWorker;

    @InjectMocks
    private BatchTranslationRecoveryManager recoveryManager;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(recoveryManager, "autoResumeOnStartup", true);
    }

    @Test
    @DisplayName("Should detect and auto-resume IN_PROGRESS and PENDING jobs on application startup")
    void shouldAutoResumeInterruptedJobsOnStartup() {
        UUID jobId1 = UUID.randomUUID();
        UUID jobId2 = UUID.randomUUID();

        BatchTranslationJob job1 = BatchTranslationJob.builder()
                .status(BatchTranslationJobStatus.IN_PROGRESS)
                .targetLanguage("hi")
                .build();
        ReflectionTestUtils.setField(job1, "id", jobId1);
        job1.setTenantId("tenant-alpha");

        BatchTranslationJob job2 = BatchTranslationJob.builder()
                .status(BatchTranslationJobStatus.PENDING)
                .targetLanguage("ta")
                .build();
        ReflectionTestUtils.setField(job2, "id", jobId2);
        job2.setTenantId("tenant-beta");

        when(jobRepository.findByStatusInOrderByCreatedAtAsc(
                List.of(BatchTranslationJobStatus.IN_PROGRESS, BatchTranslationJobStatus.PENDING)
        )).thenReturn(List.of(job1, job2));

        recoveryManager.onApplicationReady();

        verify(asyncWorker).processBatchTranslationJob(jobId1, "tenant-alpha");
        verify(asyncWorker).processBatchTranslationJob(jobId2, "tenant-beta");
    }

    @Test
    @DisplayName("Should do nothing when no interrupted jobs found")
    void shouldDoNothingWhenNoInterruptedJobs() {
        when(jobRepository.findByStatusInOrderByCreatedAtAsc(any()))
                .thenReturn(List.of());

        int count = recoveryManager.resumeInterruptedJobs();

        assertThat(count).isEqualTo(0);
        verify(asyncWorker, never()).processBatchTranslationJob(any(), any());
    }

    @Test
    @DisplayName("Should skip auto-resume when autoResumeOnStartup is disabled")
    void shouldSkipWhenAutoResumeDisabled() {
        ReflectionTestUtils.setField(recoveryManager, "autoResumeOnStartup", false);

        recoveryManager.onApplicationReady();

        verify(jobRepository, never()).findByStatusInOrderByCreatedAtAsc(any());
        verify(asyncWorker, never()).processBatchTranslationJob(any(), any());
    }
}
