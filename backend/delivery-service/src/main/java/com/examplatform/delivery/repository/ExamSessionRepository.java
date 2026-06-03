package com.examplatform.delivery.repository;

import com.examplatform.delivery.domain.ExamSession;
import com.examplatform.delivery.domain.ExamSession.ExamSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for {@link ExamSession} entities.
 * Provides tenant-scoped queries for session management and HPA metrics.
 */
@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, UUID> {

    /**
     * Find all sessions for a specific candidate within a tenant.
     */
    List<ExamSession> findByCandidateIdAndTenantId(UUID candidateId, String tenantId);

    /**
     * Find all sessions with the given status within a tenant.
     */
    List<ExamSession> findByStatusAndTenantId(ExamSessionStatus status, String tenantId);

    /**
     * Count sessions with a given status within a tenant.
     * Used for HPA metrics (active_exam_sessions gauge).
     */
    long countByStatusAndTenantId(ExamSessionStatus status, String tenantId);

    /**
     * Count all sessions with the given status across all tenants.
     * Used for the HPA custom metric gauge.
     */
    long countByStatus(ExamSessionStatus status);
}
