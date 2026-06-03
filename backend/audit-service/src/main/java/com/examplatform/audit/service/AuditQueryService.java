package com.examplatform.audit.service;

import com.examplatform.audit.domain.AuditEvent;
import com.examplatform.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for querying audit events with dynamic filtering.
 * Builds JPA Specifications from non-null filter parameters and
 * returns paginated results. Accessible only to Auditor role.
 *
 * Validates: Requirements 15.3
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditQueryService {

    private final AuditEventRepository auditEventRepository;

    /**
     * Query audit events with dynamic filtering.
     *
     * @param actorId   filter by actor (userId) — nullable
     * @param eventType filter by event/action type — nullable
     * @param resource  filter by resource (examId as string) — nullable
     * @param from      filter events occurred at or after — nullable
     * @param to        filter events occurred at or before — nullable
     * @param tenantId  the tenant scope — required
     * @param pageable  pagination parameters
     * @return paginated audit events matching the filters
     */
    public Page<AuditEvent> queryEvents(UUID actorId, String eventType, String resource,
                                         Instant from, Instant to, String tenantId, Pageable pageable) {
        log.debug("Querying audit events: actorId={}, eventType={}, resource={}, from={}, to={}, tenant={}",
                actorId, eventType, resource, from, to, tenantId);

        Specification<AuditEvent> spec = buildSpecification(actorId, eventType, resource, from, to, tenantId);
        return auditEventRepository.findAll(spec, pageable);
    }

    private Specification<AuditEvent> buildSpecification(UUID actorId, String eventType, String resource,
                                                          Instant from, Instant to, String tenantId) {
        Specification<AuditEvent> spec = Specification.where(tenantEquals(tenantId));

        if (actorId != null) {
            spec = spec.and(actorIdEquals(actorId));
        }
        if (eventType != null && !eventType.isBlank()) {
            spec = spec.and(eventTypeEquals(eventType));
        }
        if (resource != null && !resource.isBlank()) {
            spec = spec.and(resourceContains(resource));
        }
        if (from != null) {
            spec = spec.and(occurredAtAfter(from));
        }
        if (to != null) {
            spec = spec.and(occurredAtBefore(to));
        }

        return spec;
    }

    private static Specification<AuditEvent> tenantEquals(String tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    private static Specification<AuditEvent> actorIdEquals(UUID actorId) {
        return (root, query, cb) -> cb.equal(root.get("actorId"), actorId);
    }

    private static Specification<AuditEvent> eventTypeEquals(String eventType) {
        return (root, query, cb) -> cb.equal(root.get("eventType"), eventType);
    }

    private static Specification<AuditEvent> resourceContains(String resource) {
        return (root, query, cb) -> cb.like(root.get("resource"), "%" + resource + "%");
    }

    private static Specification<AuditEvent> occurredAtAfter(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), from);
    }

    private static Specification<AuditEvent> occurredAtBefore(Instant to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), to);
    }
}
