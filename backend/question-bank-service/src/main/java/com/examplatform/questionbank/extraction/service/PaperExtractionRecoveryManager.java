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
import com.examplatform.shared.constants.SharedConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Service recovery manager detecting and auto-resuming question paper extraction jobs
 * that were left in {@code PENDING} or {@code PROCESSING} status upon service startup or restart.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaperExtractionRecoveryManager {

    private final QuestionPaperMetadataRepository metadataRepository;
    private final AsyncPaperExtractionWorker asyncWorker;

    @Value("${app.extraction.auto-resume-on-startup:true}")
    private boolean autoResumeOnStartup = true;

    /**
     * Application readiness event listener that evaluates startup recovery flags.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!autoResumeOnStartup) {
            log.info("Paper extraction auto-resume on startup is disabled.");
            return;
        }

        resumeInterruptedJobs();
    }

    /**
     * Queries for interrupted jobs in PENDING or PROCESSING status and dispatches them
     * to the async extraction worker.
     *
     * @return count of resumed extraction jobs
     */
    public int resumeInterruptedJobs() {
        log.info("Scanning for interrupted question paper extraction jobs to auto-resume...");
        try {
            List<QuestionPaperMetadata> stuckJobs = metadataRepository.findByExtractionStatusInOrderByCreatedAtAsc(
                    List.of(PaperExtractionStatus.PENDING, PaperExtractionStatus.PROCESSING)
            );

            if (stuckJobs.isEmpty()) {
                log.info("No interrupted question paper extraction jobs found.");
                return 0;
            }

            log.info("Found {} interrupted question paper extraction job(s) to auto-resume.", stuckJobs.size());
            for (QuestionPaperMetadata job : stuckJobs) {
                String tenantId = job.getTenantId() != null ? job.getTenantId() : SharedConstants.DEFAULT_TENANT;
                log.info("Auto-resuming paper extraction job {} (tenant: {}, status: {})",
                        job.getId(), tenantId, job.getExtractionStatus());
                asyncWorker.processPaperExtractionJob(job.getId(), tenantId);
            }
            return stuckJobs.size();
        } catch (Exception e) {
            log.error("Failed to auto-resume interrupted paper extraction jobs on startup: {}", e.getMessage(), e);
            return 0;
        }
    }
}
