package com.examplatform.questionbank.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Represents a topic within a subject (e.g. Indian History under General Studies).
 * Unique per name + subject + tenant combination.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "topic", schema = "question_service",
       uniqueConstraints = @UniqueConstraint(columnNames = {"name", "subject_id", "tenant_id"}))
public class Topic extends BaseEntity {

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description")
    private String description;

    @Builder.Default
    @Column(name = "active")
    private boolean active = true;
}
