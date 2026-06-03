package com.examplatform.evaluation.repository;

import com.examplatform.evaluation.domain.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for evaluations.
 */
@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    /**
     * Retrieves all evaluations for a given session within a tenant.
     */
    List<Evaluation> findBySessionIdAndTenantId(UUID sessionId, String tenantId);

    /**
     * Retrieves all evaluations for a given candidate within a tenant.
     */
    List<Evaluation> findByCandidateIdAndTenantId(UUID candidateId, String tenantId);

    /**
     * Retrieves evaluations by status within a tenant.
     */
    List<Evaluation> findByStatusAndTenantId(Evaluation.EvaluationStatus status, String tenantId);
}
