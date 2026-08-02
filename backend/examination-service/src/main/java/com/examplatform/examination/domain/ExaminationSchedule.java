package com.examplatform.examination.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Examination schedule entity.
 *
 * <p>Each {@link Examination} may have one or more schedules (multi-phase support).
 * Amendments to a Published schedule do NOT mutate existing rows — a new row is
 * created with an incremented {@link #scheduleVersion} and {@link #previousVersionId}
 * pointing to the prior row, forming an immutable version chain.
 *
 * <p>Approval workflow states (Req 7b.7):
 * <pre>
 *   DRAFT → SCHEDULER_REVIEW → CONTROLLER_APPROVED
 *        → SECURITY_REVIEW  → CHAIRMAN_APPROVED → PUBLISHED
 *   Any state → CANCELLED
 * </pre>
 *
 * <p>The business {@link #scheduleVersion} (1, 2, 3…) is stored in the DB column
 * {@code schedule_version}. The inherited BaseEntity {@code @Version} optimistic-lock
 * counter uses the column {@code version} as usual.
 *
 * Validates: Requirements 7b.1, 7b.7, 7b.8, 7b.9
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "examination_schedule", schema = "examination_service")
public class ExaminationSchedule extends BaseEntity {

    /** FK → examination.id */
    @Column(name = "examination_id", nullable = false, columnDefinition = "uuid")
    private UUID examinationId;

    @Column(name = "schedule_name", nullable = false, length = 255)
    private String scheduleName;

    /**
     * Business version number — 1 for the initial schedule, incremented on every
     * published amendment. Distinct from the inherited JPA optimistic-lock counter.
     */
    @Column(name = "schedule_version", nullable = false)
    @Builder.Default
    private int scheduleVersion = 1;

    /** Government notification / gazette reference number. */
    @Column(name = "notification_number", length = 100)
    private String notificationNumber;

    /** Primary date on which this schedule runs. */
    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    /**
     * Backup date. Must not overlap with any other schedule's exam_date
     * within the same tenant (validated at service layer, Req 7b.10).
     */
    @Column(name = "reserve_date")
    private LocalDate reserveDate;

    /** IANA time-zone identifier; default "Asia/Kolkata". */
    @Column(name = "time_zone", nullable = false, length = 60)
    @Builder.Default
    private String timeZone = "Asia/Kolkata";

    /**
     * Workflow status.
     * DRAFT | SCHEDULER_REVIEW | CONTROLLER_APPROVED |
     * SECURITY_REVIEW | CHAIRMAN_APPROVED | PUBLISHED | CANCELLED
     */
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    /** Mandatory when amending a PUBLISHED schedule. */
    @Column(name = "change_reason", length = 1000)
    private String changeReason;

    /** Date from which this version of the schedule takes effect. */
    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    /**
     * FK → the immediately preceding version of this schedule.
     * Null for {@code scheduleVersion == 1}. Forms an immutable linked-list.
     */
    @Column(name = "previous_version_id", columnDefinition = "uuid")
    private UUID previousVersionId;

    /** UUID of the user who created this schedule. */
    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;

    /** UUID of the user who last modified this schedule. */
    @Column(name = "modified_by", columnDefinition = "uuid")
    private UUID modifiedBy;

    /** UUID of the user who performed the final approval step. */
    @Column(name = "approved_by", columnDefinition = "uuid")
    private UUID approvedBy;

    /** Timestamp of the final approval step. */
    @Column(name = "approved_at")
    private Instant approvedAt;
}
