package com.examplatform.delivery.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an active exam session for a candidate.
 * Tracks the live state of an ongoing examination delivery.
 */
@Entity
@Table(name = "exam_session", schema = "delivery_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSession extends BaseEntity {

    @Column(name = "session_id", nullable = false, unique = true)
    private UUID sessionId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "shift_id", nullable = false)
    private UUID shiftId;

    @Column(name = "paper_id", nullable = false)
    private UUID paperId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExamSessionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "scheduled_end_at", nullable = false)
    private Instant scheduledEndAt;

    @Column(name = "current_question_index", nullable = false)
    private Integer currentQuestionIndex;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @Column(name = "full_screen_exit_count", nullable = false)
    private Integer fullScreenExitCount;

    /**
     * Exam session lifecycle states.
     */
    public enum ExamSessionStatus {
        ACTIVE,
        SUBMITTED,
        EXPIRED
    }
}
