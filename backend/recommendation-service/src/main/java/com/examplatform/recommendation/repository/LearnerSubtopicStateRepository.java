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

package com.examplatform.recommendation.repository;

import com.examplatform.recommendation.domain.LearnerSubtopicState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearnerSubtopicStateRepository extends JpaRepository<LearnerSubtopicState, UUID> {
    Optional<LearnerSubtopicState> findByEnrollmentIdAndSubtopicId(UUID enrollmentId, Long subtopicId);
    List<LearnerSubtopicState> findByEnrollmentId(UUID enrollmentId);
    List<LearnerSubtopicState> findByCandidateId(UUID candidateId);
    List<LearnerSubtopicState> findByEnrollmentIdAndStatus(UUID enrollmentId, String status);
    List<LearnerSubtopicState> findByEnrollmentIdAndNextReviewAtBefore(UUID enrollmentId, Instant time);
}
