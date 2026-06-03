package com.examplatform.audit.repository;

import com.examplatform.audit.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for audit events.
 * Read-only access pattern — no updates or deletes are permitted.
 * Extends JpaSpecificationExecutor for dynamic filtering in audit queries.
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID>, JpaSpecificationExecutor<AuditEvent> {

    /**
     * Retrieves audit events for a given actor within a tenant.
     */
    List<AuditEvent> findByActorIdAndTenantId(UUID actorId, String tenantId);

    /**
     * Retrieves audit events by type within a time range for a tenant.
     */
    List<AuditEvent> findByEventTypeAndTenantIdAndOccurredAtBetween(
            String eventType, String tenantId, Instant from, Instant to);

    /**
     * Retrieves audit events for a tenant within a time range.
     */
    List<AuditEvent> findByTenantIdAndOccurredAtBetween(String tenantId, Instant from, Instant to);
}
