// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "practice_session", schema = "practice_service")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PracticeSession extends BaseEntity {
    @Column(name = "candidate_id", nullable = false, columnDefinition = "uuid")
    private UUID candidateId;
    
    @Column(name = "practice_set_id", nullable = false, columnDefinition = "uuid")
    private UUID practiceSetId;
    
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String mode = "TIMED"; // TIMED | UNTIMED | SECTION_WISE
    
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "CREATED"; // CREATED | IN_PROGRESS | SUBMITTED | ABANDONED
    
    @Column(name = "started_at")
    private Instant startedAt;
    
    @Column(name = "submitted_at")
    private Instant submittedAt;
    
    @Column(name = "total_questions", nullable = false)
    @Builder.Default
    private int totalQuestions = 0;
    
    @Column(name = "duration_minutes", nullable = false)
    @Builder.Default
    private int durationMinutes = 0;
    
    @Column(name = "correct_count", nullable = false)
    @Builder.Default
    private int correctCount = 0;
    
    @Column(name = "incorrect_count", nullable = false)
    @Builder.Default
    private int incorrectCount = 0;
    
    @Column(name = "skipped_count", nullable = false)
    @Builder.Default
    private int skippedCount = 0;
    
    @Column(name = "total_marks", nullable = false)
    @Builder.Default
    private int totalMarks = 0;
    
    @Column(name = "obtained_marks", nullable = false)
    @Builder.Default
    private int obtainedMarks = 0;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "topic_wise_breakdown", columnDefinition = "jsonb")
    private String topicWiseBreakdown;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "difficulty_breakdown", columnDefinition = "jsonb")
    private String difficultyBreakdown;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "timing_breakdown", columnDefinition = "jsonb")
    private String timingBreakdown;
}
