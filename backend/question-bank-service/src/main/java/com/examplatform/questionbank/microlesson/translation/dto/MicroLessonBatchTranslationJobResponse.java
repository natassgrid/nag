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

package com.examplatform.questionbank.microlesson.translation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MicroLessonBatchTranslationJobResponse implements Serializable {

    private UUID id;
    private String tenantId;
    private String status;
    private String sourceLanguage;
    private String targetLanguage;
    private String targetStatus;
    private String subjectFilter;
    private String topicFilter;
    private String targetExamFilter;
    private List<UUID> lessonIds;
    private boolean overwriteExisting;
    private int totalLessons;
    private int processedLessons;
    private int successfulLessons;
    private int failedLessons;
    private double progressPercentage;
    private List<String> failedLessonIds;
    private int batchSize;
    private int throttleDelayMs;
    private int maxConcurrency;
    private UUID initiatedBy;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;

    @JsonProperty("jobId")
    public UUID getJobId() {
        return id;
    }
}
