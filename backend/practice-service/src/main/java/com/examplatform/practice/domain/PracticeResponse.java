// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

@Entity
@Table(name = "practice_response", schema = "practice_service")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PracticeResponse extends BaseEntity {
    @Column(name = "practice_session_id", nullable = false, columnDefinition = "uuid")
    private UUID practiceSessionId;
    
    @Column(name = "question_id", nullable = false, columnDefinition = "uuid")
    private UUID questionId;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_option_ids", columnDefinition = "jsonb")
    private String selectedOptionIds;
    
    @Column(name = "entered_value", columnDefinition = "text")
    private String enteredValue;
    
    @Column(nullable = false)
    @Builder.Default
    private boolean correct = false;
    
    @Column(name = "marks_awarded", nullable = false)
    @Builder.Default
    private int marksAwarded = 0;
    
    @Column(name = "time_spent_ms", nullable = false)
    @Builder.Default
    private long timeSpentMs = 0;
    
    @Column(name = "marked_for_review", nullable = false)
    @Builder.Default
    private boolean markedForReview = false;
    
    @Column(name = "revision_sequence", nullable = false)
    @Builder.Default
    private int revisionSequence = 1;
}
