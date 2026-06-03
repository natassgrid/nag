package com.examplatform.result.repository;

import com.examplatform.result.domain.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for results.
 */
@Repository
public interface ResultRepository extends JpaRepository<Result, UUID> {

    /**
     * Retrieves all results for a given candidate within a tenant.
     */
    List<Result> findByCandidateIdAndTenantId(UUID candidateId, String tenantId);

    /**
     * Retrieves a specific result for a candidate in an exam within a tenant.
     */
    Optional<Result> findByCandidateIdAndExamIdAndTenantId(UUID candidateId, UUID examId, String tenantId);

    /**
     * Retrieves all results for an exam within a tenant, ordered by rank.
     */
    List<Result> findByExamIdAndTenantIdOrderByOverallRankAsc(UUID examId, String tenantId);
}
