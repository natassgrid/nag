// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.repository;

import com.examplatform.recommendation.domain.Recommendation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {
    Page<Recommendation> findByCandidateIdOrderByCreatedAtDesc(UUID candidateId, Pageable pageable);
    Optional<Recommendation> findTopByCandidateIdOrderByCreatedAtDesc(UUID candidateId);
}
