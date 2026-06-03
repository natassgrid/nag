package com.examplatform.response.repository;

import com.examplatform.response.domain.Response;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for candidate responses.
 * Query methods leverage the composite index on (session_id, question_id, revision_sequence DESC)
 * and the partial index on candidate_id WHERE is_final=TRUE.
 */
@Repository
public interface ResponseRepository extends JpaRepository<Response, UUID> {

    /**
     * Retrieves all response revisions for a specific question within a session,
     * ordered by revision_sequence descending (latest first).
     * Uses composite index: (session_id, question_id, revision_sequence DESC).
     */
    List<Response> findBySessionIdAndQuestionIdOrderByRevisionSequenceDesc(UUID sessionId, UUID questionId);

    /**
     * Retrieves all responses for a given session within a tenant.
     */
    List<Response> findBySessionIdAndTenantId(UUID sessionId, String tenantId);

    /**
     * Retrieves all final responses for a candidate within a tenant.
     * Uses partial index: candidate_id WHERE is_final=TRUE.
     */
    List<Response> findByCandidateIdAndIsFinalTrueAndTenantId(UUID candidateId, String tenantId);
}
