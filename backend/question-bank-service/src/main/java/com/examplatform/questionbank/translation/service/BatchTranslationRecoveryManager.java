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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Service recovery manager that automatically detects and resumes batch translation
 * jobs that were left in IN_PROGRESS or PENDING state when the service stopped or restarted.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchTranslationRecoveryManager {

    private final BatchTranslationJobRepository jobRepository;
    private final AsyncBatchTranslationWorker asyncWorker;

    @Value("${translation.batch.auto-resume-on-startup:true}")
    private boolean autoResumeOnStartup = true;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!autoResumeOnStartup) {
            log.info("Batch translation auto-resume on startup is disabled.");
            return;
        }

        resumeInterruptedJobs();
    }

    public int resumeInterruptedJobs() {
        log.info("Checking for interrupted batch translation jobs to auto-resume...");
        try {
            List<BatchTranslationJob> stuckJobs = jobRepository.findByStatusInOrderByCreatedAtAsc(
                    List.of(BatchTranslationJobStatus.IN_PROGRESS, BatchTranslationJobStatus.PENDING)
            );

            if (stuckJobs.isEmpty()) {
                log.info("No interrupted batch translation jobs found.");
                return 0;
            }

            log.info("Found {} interrupted batch translation job(s) to auto-resume.", stuckJobs.size());
            for (BatchTranslationJob job : stuckJobs) {
                String tenantId = job.getTenantId() != null ? job.getTenantId() : "default";
                log.info("Auto-resuming batch translation job {} (tenant: {}, status: {}, targetLang: {})",
                        job.getId(), tenantId, job.getStatus(), job.getTargetLanguage());
                asyncWorker.processBatchTranslationJob(job.getId(), tenantId);
            }
            return stuckJobs.size();
        } catch (Exception e) {
            log.error("Failed to auto-resume interrupted batch translation jobs on startup: {}", e.getMessage(), e);
            return 0;
        }
    }
}
