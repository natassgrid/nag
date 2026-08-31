/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.examplatform.questionbank.ai.batch;

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
import java.util.UUID;

/**
 * Entity tracking an async batch question generation job.
 * A single job contains multiple generation items (stored as JSON),
 * all processed in one Bedrock batch inference call for cost efficiency.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "batch_generation_job", schema = "question_service")
public class BatchGenerationJob extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private BatchJobStatus status = BatchJobStatus.PENDING;

    /**
     * JSON array of generation items. Each item has:
     * subject, topic, subtopic, difficulty, cognitiveLevel, questionType, count.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items", columnDefinition = "jsonb", nullable = false)
    private String items;

    /** Total number of questions requested (sum of all items' counts). */
    @Column(name = "total_requested", nullable = false)
    private int totalRequested;

    /** Number of questions successfully generated. */
    @Column(name = "total_generated", nullable = false)
    @Builder.Default
    private int totalGenerated = 0;

    /** Number of questions that failed validation. */
    @Column(name = "total_failed", nullable = false)
    @Builder.Default
    private int totalFailed = 0;

    /** Number of duplicates detected and skipped. */
    @Column(name = "total_duplicates", nullable = false)
    @Builder.Default
    private int totalDuplicates = 0;

    /** The Bedrock model ID used (e.g., "amazon.nova-lite-v1:0"). */
    @Column(name = "model_used", length = 100)
    private String modelUsed;

    /** Whether to check for duplicates. */
    @Column(name = "avoid_duplicates", nullable = false)
    @Builder.Default
    private boolean avoidDuplicates = true;

    /** User who initiated the batch job. */
    @Column(name = "initiated_by", nullable = false)
    private UUID initiatedBy;

    /** Timestamp when processing started. */
    @Column(name = "started_at")
    private Instant startedAt;

    /** Timestamp when processing completed. */
    @Column(name = "completed_at")
    private Instant completedAt;

    /** Error message if the job failed. */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** The AWS Bedrock model invocation job ARN. */
    @Column(name = "bedrock_job_arn", length = 500)
    private String bedrockJobArn;

    /** JSON array of generated question IDs. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "generated_question_ids", columnDefinition = "jsonb")
    private String generatedQuestionIds;
}
