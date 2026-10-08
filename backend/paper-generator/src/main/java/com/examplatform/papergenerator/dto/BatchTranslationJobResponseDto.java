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

package com.examplatform.papergenerator.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BatchTranslationJobResponseDto {

    private UUID id;
    private String tenantId;
    private String status;
    private String sourceLanguage;
    private String targetLanguage;
    private String targetStatus;
    private String subjectFilter;
    private UUID paperId;
    private List<UUID> questionIds;
    private Boolean overwriteExisting;
    private Integer totalQuestions;
    private Integer processedQuestions;
    private Integer successfulQuestions;
    private Integer failedQuestions;
    private Double progressPercentage;
    private List<String> failedQuestionIds;
    private Integer batchSize;
    private Integer throttleDelayMs;
    private Integer maxConcurrency;
    private UUID initiatedBy;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;

    public boolean isOverwriteExisting() {
        return Boolean.TRUE.equals(overwriteExisting);
    }

    public int getTotalQuestions() {
        return totalQuestions != null ? totalQuestions : 0;
    }

    public int getProcessedQuestions() {
        return processedQuestions != null ? processedQuestions : 0;
    }

    public int getSuccessfulQuestions() {
        return successfulQuestions != null ? successfulQuestions : 0;
    }

    public int getFailedQuestions() {
        return failedQuestions != null ? failedQuestions : 0;
    }

    public double getProgressPercentage() {
        return progressPercentage != null ? progressPercentage : 0.0;
    }

    public int getBatchSize() {
        return batchSize != null ? batchSize : 0;
    }

    public int getThrottleDelayMs() {
        return throttleDelayMs != null ? throttleDelayMs : 0;
    }

    public int getMaxConcurrency() {
        return maxConcurrency != null ? maxConcurrency : 0;
    }
}
