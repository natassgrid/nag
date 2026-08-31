/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.examination.repository;

import com.examplatform.examination.domain.ExamApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamApplicationRepository extends JpaRepository<ExamApplication, UUID> {

    List<ExamApplication> findByCandidateIdAndTenantId(UUID candidateId, String tenantId);

    Optional<ExamApplication> findByCandidateIdAndExaminationIdAndTenantId(
            UUID candidateId, UUID examinationId, String tenantId);

    boolean existsByCandidateIdAndExaminationIdAndTenantId(
            UUID candidateId, UUID examinationId, String tenantId);
}
