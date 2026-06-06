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

/**
 * Represents an examination subject (e.g. General Studies, Mathematics).
 * Unique per name + tenant combination.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "subject", schema = "question_service",
       uniqueConstraints = @UniqueConstraint(columnNames = {"name", "tenant_id"}))
public class Subject extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "description")
    private String description;

    @Builder.Default
    @Column(name = "active")
    private boolean active = true;
}
