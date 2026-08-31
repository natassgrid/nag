/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.examplatform.questionbank.ai.batch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for batch generation job tracking.
 */
@Repository
public interface BatchGenerationJobRepository extends JpaRepository<BatchGenerationJob, UUID> {

    /** Find all jobs for a given tenant, ordered by creation time descending. */
    Page<BatchGenerationJob> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    /** Find all jobs initiated by a specific user. */
    Page<BatchGenerationJob> findByInitiatedByAndTenantIdOrderByCreatedAtDesc(
            UUID initiatedBy, String tenantId, Pageable pageable);

    /** Find all pending/processing jobs (for the async worker to pick up). */
    @Query("SELECT j FROM BatchGenerationJob j WHERE j.status IN :statuses AND j.tenantId = :tenantId")
    List<BatchGenerationJob> findActiveJobs(
            @Param("statuses") List<BatchJobStatus> statuses,
            @Param("tenantId") String tenantId);

    /** Find pending jobs globally (for the scheduler). */
    List<BatchGenerationJob> findByStatusOrderByCreatedAtAsc(BatchJobStatus status);
}
