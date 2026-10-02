// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

@Entity
@Table(name = "practice_set", schema = "practice_service")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PracticeSet extends BaseEntity {
    @Column(nullable = false, length = 200)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_by", nullable = false, columnDefinition = "uuid")
    private UUID createdBy;
    
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String source = "MANUAL"; // MANUAL | AUTO_GENERATED | EXAM_CLONE
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_ids", columnDefinition = "jsonb")
    private String questionIds; // JSON array of UUID strings
    
    @Column(name = "duration_minutes", nullable = false)
    @Builder.Default
    private int durationMinutes = 60;
    
    @Column(name = "subject_slug", length = 100)
    private String subjectSlug;
    
    @Column(nullable = false)
    @Builder.Default
    private boolean published = false;
    
    @Column(name = "total_questions", nullable = false)
    @Builder.Default
    private int totalQuestions = 0;
}
