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
 * Represents a subtopic within a topic (e.g. Ancient India under Indian History).
 * Unique per name + topic + tenant combination.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "subtopic", schema = "question_service",
       uniqueConstraints = @UniqueConstraint(columnNames = {"name", "topic_id", "tenant_id"}))
public class Subtopic extends BaseEntity {

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description")
    private String description;

    @Builder.Default
    @Column(name = "active")
    private boolean active = true;
}
