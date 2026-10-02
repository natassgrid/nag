// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.repository;

import com.examplatform.recommendation.domain.LearnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearnerProfileRepository extends JpaRepository<LearnerProfile, UUID> {
    Optional<LearnerProfile> findByCandidateId(UUID candidateId);
}
