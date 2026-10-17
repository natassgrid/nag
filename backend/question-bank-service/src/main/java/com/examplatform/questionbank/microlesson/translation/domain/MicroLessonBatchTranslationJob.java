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

package com.examplatform.questionbank.microlesson.translation.domain;

import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity tracking asynchronous batch translation jobs for micro-lessons.
 */
@Entity
@Table(name = "microlesson_batch_translation_job", schema = "question_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class MicroLessonBatchTranslationJob extends BaseEntity {

    @Column(name = SharedConstants.STATUS, nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING";

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

    @Column(name = "topic_filter", length = 200)
    private String topicFilter;

    @Column(name = "target_exam_filter", length = 50)
    private String targetExamFilter;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "lesson_ids", columnDefinition = "jsonb")
    @Builder.Default
    private List<UUID> lessonIds = new ArrayList<>();

    @Column(name = "overwrite_existing", nullable = false)
    @Builder.Default
    private boolean overwriteExisting = true;

    @Column(name = "total_lessons", nullable = false)
    @Builder.Default
    private int totalLessons = 0;

    @Column(name = "processed_lessons", nullable = false)
    @Builder.Default
    private int processedLessons = 0;

    @Column(name = "successful_lessons", nullable = false)
    @Builder.Default
    private int successfulLessons = 0;

    @Column(name = "failed_lessons", nullable = false)
    @Builder.Default
    private int failedLessons = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "failed_lesson_ids", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> failedLessonIds = new ArrayList<>();

    @Column(name = "batch_size", nullable = false)
    @Builder.Default
    private int batchSize = 20;

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

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;
}
