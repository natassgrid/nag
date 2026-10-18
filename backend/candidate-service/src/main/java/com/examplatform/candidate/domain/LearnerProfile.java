/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.candidate.domain;

import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Canonical learner profile entity for exam-adaptive onboarding.
 *
 * Under the Unified Exam Spine architecture, learner_profile must be completed
 * (language, education level, daily study minutes) before enrollment is permitted.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "CandidateLearnerProfile")
@Table(name = "learner_profile", schema = "candidate_service")
public class LearnerProfile {

    @Id
    @Column(name = "candidate_id", nullable = false, columnDefinition = "uuid")
    private UUID candidateId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "education_level", length = 100)
    private String educationLevel;

    @Column(name = "preferred_language", length = 50)
    private String preferredLanguage;

    @Column(name = "daily_study_minutes")
    private Integer dailyStudyMinutes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_lesson_types", columnDefinition = "jsonb")
    private String preferredLessonTypes;

    @Column(name = "prior_experience", columnDefinition = "TEXT")
    private String priorExperience;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    protected void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.tenantId == null) {
            String contextTenant = TenantContext.get();
            this.tenantId = (contextTenant != null) ? contextTenant : SharedConstants.DEFAULT_TENANT;
        }
    }

    @PreUpdate
    protected void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean isComplete() {
        return this.completedAt != null;
    }
}
