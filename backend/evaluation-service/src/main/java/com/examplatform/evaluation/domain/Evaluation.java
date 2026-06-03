package com.examplatform.evaluation.domain;

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

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents the evaluation of a single candidate response to a question.
 * Supports both auto-evaluation (MCQ, numerical) and manual evaluation (subjective).
 */
@Entity
@Table(name = "evaluation", schema = "evaluation_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evaluation extends BaseEntity {

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_type", nullable = false, length = 10)
    private EvaluationType evaluationType;

    @Column(name = "evaluator_id")
    private UUID evaluatorId;

    @Column(name = "score", nullable = false, precision = 10, scale = 2)
    private BigDecimal score;

    @Column(name = "max_marks", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxMarks;

    @Column(name = "negative_marks", nullable = false, precision = 10, scale = 2)
    private BigDecimal negativeMarks;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EvaluationStatus status;

    /**
     * Type of evaluation performed.
     */
    public enum EvaluationType {
        AUTO,
        MANUAL
    }

    /**
     * Evaluation lifecycle states.
     */
    public enum EvaluationStatus {
        PENDING,
        AUTO_EVALUATED,
        MANUAL_EVALUATED,
        ARBITRATION,
        FINALIZED
    }
}
