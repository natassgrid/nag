// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.repository;

import com.examplatform.recommendation.domain.LearnerSubtopicPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearnerSubtopicPerformanceRepository extends JpaRepository<LearnerSubtopicPerformance, UUID> {
    Optional<LearnerSubtopicPerformance> findByCandidateIdAndSubtopicName(UUID candidateId, String subtopicName);
    List<LearnerSubtopicPerformance> findByCandidateId(UUID candidateId);
}
