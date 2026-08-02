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

import java.time.LocalTime;
import java.util.UUID;

/**
 * A single shift within an {@link ExaminationSchedule}.
 *
 * <p>All time fields are plain {@link LocalTime} (no date component). The
 * authoritative full instant is formed by combining the parent schedule's
 * {@code examDate} and {@code timeZone} with the shift time.
 *
 * <p>Timing ordering invariants enforced at the service layer (Req 7b.3):
 * <ul>
 *   <li>{@link #reportingTime} &lt; {@link #gateClosingTime}
 *   <li>{@link #gateClosingTime} &lt; {@link #loginStartTime}
 *   <li>{@link #loginStartTime} &lt; {@link #examStartTime}
 *   <li>{@link #examStartTime} &lt; {@link #examEndTime}
 *   <li>{@link #durationMinutes} == minutes between {@link #examStartTime} and {@link #examEndTime}
 * </ul>
 *
 * Validates: Requirements 7b.2, 7b.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "exam_shift", schema = "examination_service")
public class ExamShift extends BaseEntity {

    /** FK → examination_schedule.id */
    @Column(name = "schedule_id", nullable = false, columnDefinition = "uuid")
    private UUID scheduleId;

    /** 1-based position within the schedule (1 = first shift of the day). */
    @Column(name = "shift_number", nullable = false)
    private int shiftNumber;

    /** Human-readable label, e.g. "Morning", "Afternoon", "Evening". */
    @Column(name = "shift_name", length = 100)
    private String shiftName;

    /** Candidate reporting time. Must precede {@link #gateClosingTime}. */
    @Column(name = "reporting_time", nullable = false)
    private LocalTime reportingTime;

    /** Entry deadline. Must be after {@link #reportingTime} and before {@link #loginStartTime}. */
    @Column(name = "gate_closing_time", nullable = false)
    private LocalTime gateClosingTime;

    /** Platform login opens. Must precede {@link #examStartTime}. */
    @Column(name = "login_start_time", nullable = false)
    private LocalTime loginStartTime;

    /** Examination begins. Must precede {@link #examEndTime}. */
    @Column(name = "exam_start_time", nullable = false)
    private LocalTime examStartTime;

    /** Examination ends. Must be after {@link #examStartTime}. */
    @Column(name = "exam_end_time", nullable = false)
    private LocalTime examEndTime;

    /** Candidate exit time. Optional — recorded but not validated against other fields. */
    @Column(name = "exit_time")
    private LocalTime exitTime;

    /**
     * Duration of the examination in minutes.
     * Must equal {@code ChronoUnit.MINUTES.between(examStartTime, examEndTime)}.
     */
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    /** Buffer (in minutes) to allow between this shift's exit and the next shift's reporting. */
    @Column(name = "buffer_minutes", nullable = false)
    @Builder.Default
    private int bufferMinutes = 0;
}
