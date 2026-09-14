/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.\n *\n * This program is distributed in the hope that it will be useful,\n * but WITHOUT ANY WARRANTY; without even the implied warranty of\n * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the\n * GNU Affero General Public License for more details.\n *\n * You should have received a copy of the GNU Affero General Public License\n * along with this program. If not, see <https://www.gnu.org/licenses/>.\n */

package com.examplatform.questionbank.translation.dto;

import com.examplatform.questionbank.translation.domain.BatchTranslationJobStatus;
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
public class BatchTranslationJobResponse {

    private UUID id;
    private String tenantId;
    private BatchTranslationJobStatus status;
    private String sourceLanguage;
    private String targetLanguage;
    private String targetStatus;
    private String subjectFilter;
    private UUID paperId;
    private List<UUID> questionIds;
    private boolean overwriteExisting;
    private int totalQuestions;
    private int processedQuestions;
    private int successfulQuestions;
    private int failedQuestions;
    private double progressPercentage;
    private List<String> failedQuestionIds;
    private int batchSize;
    private int throttleDelayMs;
    private int maxConcurrency;
    private UUID initiatedBy;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;
}
