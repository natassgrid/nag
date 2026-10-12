// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.repository;

import com.examplatform.recommendation.domain.DynamicLearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DynamicLearningPathRepository extends JpaRepository<DynamicLearningPath, UUID> {

    Optional<DynamicLearningPath> findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(UUID candidateId, String status);

    Optional<DynamicLearningPath> findTopByCandidateIdOrderByCreatedAtDesc(UUID candidateId);

    List<DynamicLearningPath> findByCandidateIdAndStatus(UUID candidateId, String status);

    List<DynamicLearningPath> findByCandidateIdOrderByCreatedAtDesc(UUID candidateId);
}
