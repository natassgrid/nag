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

package com.examplatform.questionbank.extraction.service;

import com.examplatform.questionbank.extraction.domain.PaperExtractionStatus;
import com.examplatform.questionbank.extraction.domain.QuestionPaperMetadata;
import com.examplatform.questionbank.extraction.repository.QuestionPaperMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaperExtractionRecoveryManager Unit Tests")
class PaperExtractionRecoveryManagerTest {

    @Mock
    private QuestionPaperMetadataRepository metadataRepository;

    @Mock
    private AsyncPaperExtractionWorker asyncWorker;

    private PaperExtractionRecoveryManager recoveryManager;

    @BeforeEach
    void setUp() {
        recoveryManager = new PaperExtractionRecoveryManager(metadataRepository, asyncWorker);
    }

    @Test
    @DisplayName("resumeInterruptedJobs auto-resumes PENDING and PROCESSING jobs")
    void testResumeInterruptedJobs() {
        UUID job1 = UUID.randomUUID();
        UUID job2 = UUID.randomUUID();

        QuestionPaperMetadata paper1 = QuestionPaperMetadata.builder()
                .examinationName("Exam 1")
                .extractionStatus(PaperExtractionStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(paper1, "id", job1);
        paper1.setTenantId("tenant-1");

        QuestionPaperMetadata paper2 = QuestionPaperMetadata.builder()
                .examinationName("Exam 2")
                .extractionStatus(PaperExtractionStatus.PROCESSING)
                .build();
        ReflectionTestUtils.setField(paper2, "id", job2);
        paper2.setTenantId("tenant-2");

        when(metadataRepository.findByExtractionStatusInOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(paper1, paper2));

        int resumed = recoveryManager.resumeInterruptedJobs();

        assertThat(resumed).isEqualTo(2);
        verify(asyncWorker).processPaperExtractionJob(job1, "tenant-1");
        verify(asyncWorker).processPaperExtractionJob(job2, "tenant-2");
    }

    @Test
    @DisplayName("onApplicationReady respects disabled flag")
    void testDisabledOnApplicationReady() {
        ReflectionTestUtils.setField(recoveryManager, "autoResumeOnStartup", false);

        recoveryManager.onApplicationReady();

        verifyNoInteractions(metadataRepository);
        verifyNoInteractions(asyncWorker);
    }
}
