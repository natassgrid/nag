// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.repository;
import com.examplatform.practice.domain.PracticeSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface PracticeSessionRepository extends JpaRepository<PracticeSession, UUID> {
    Page<PracticeSession> findByCandidateIdOrderByStartedAtDesc(UUID candidateId, Pageable pageable);
    Optional<PracticeSession> findByIdAndCandidateId(UUID id, UUID candidateId);
}
