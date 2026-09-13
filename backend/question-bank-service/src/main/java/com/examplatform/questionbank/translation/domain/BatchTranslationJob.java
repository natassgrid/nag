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

package com.examplatform.questionbank.translation.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity tracking asynchronous batch translation jobs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "batch_translation_job", schema = "question_service")
public class BatchTranslationJob extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private BatchTranslationJobStatus status = BatchTranslationJobStatus.PENDING;

    @Column(name = "source_language", nullable = false, length = 10)
    @Builder.Default
    private String sourceLanguage = "en";

    @Column(name = "target_language", nullable = false, length = 10)
    @Builder.Default
    private String targetLanguage = "hi";

    @Column(name = "target_status", nullable = false, length = 20)
    @Builder.Default
    private String targetStatus = "PUBLISHED";

    @Column(name = "subject_filter", length = 100)
    private String subjectFilter;

    @Column(name = "overwrite_existing", nullable = false)
    @Builder.Default
    private boolean overwriteExisting = true;

    @Column(name = "total_questions", nullable = false)
    @Builder.Default
    private int totalQuestions = 0;

    @Column(name = "processed_questions", nullable = false)
    @Builder.Default
    private int processedQuestions = 0;

    @Column(name = "successful_questions", nullable = false)
    @Builder.Default
    private int successfulQuestions = 0;

    @Column(name = "failed_questions", nullable = false)
    @Builder.Default
    private int failedQuestions = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "failed_question_ids", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> failedQuestionIds = new ArrayList<>();

    @Column(name = "batch_size", nullable = false)
    @Builder.Default
    private int batchSize = 50;

    @Column(name = "throttle_delay_ms", nullable = false)
    @Builder.Default
    private int throttleDelayMs = 50;

    @Column(name = "max_concurrency", nullable = false)
    @Builder.Default
    private int maxConcurrency = 2;

    @Column(name = "initiated_by", nullable = false)
    private UUID initiatedBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
