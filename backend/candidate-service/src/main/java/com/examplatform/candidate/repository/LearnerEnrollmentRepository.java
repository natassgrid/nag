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

package com.examplatform.candidate.repository;

import com.examplatform.candidate.domain.LearnerEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearnerEnrollmentRepository extends JpaRepository<LearnerEnrollment, UUID> {
    List<LearnerEnrollment> findByCandidateId(UUID candidateId);
    List<LearnerEnrollment> findByCandidateIdOrderByStartedAtDesc(UUID candidateId);
    Optional<LearnerEnrollment> findByCandidateIdAndStatus(UUID candidateId, String status);
    Optional<LearnerEnrollment> findByIdAndCandidateId(UUID id, UUID candidateId);
    List<LearnerEnrollment> findByCandidateIdAndStatusIn(UUID candidateId, Collection<String> statuses);
}
