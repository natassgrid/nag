package com.examplatform.audit.controller;

import com.examplatform.audit.service.AuditIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for audit events.
 * Audit records are immutable — any PUT or DELETE request is rejected with HTTP 403
 * and a tamper-attempt audit event is recorded.
 *
 * Validates: Requirements 15.4
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/audit/events")
@RequiredArgsConstructor
public class AuditEventController {

    private static final String DEFAULT_TENANT_ID = "default";

    private final AuditIngestionService auditIngestionService;

    /**
     * Reject any UPDATE attempt on an existing audit event.
     * Returns HTTP 403 and writes a TAMPER_ATTEMPT audit event.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> rejectUpdate(@PathVariable UUID id, Authentication auth) {
        String actorId = extractActorId(auth);
        String tenantId = extractTenantId(auth);
        log.warn("SECURITY: PUT attempt on audit event [{}] by actor [{}]", id, actorId);
        auditIngestionService.recordTamperAttempt(id, actorId, tenantId);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * Reject any DELETE attempt on an existing audit event.
     * Returns HTTP 403 and writes a TAMPER_ATTEMPT audit event.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> rejectDelete(@PathVariable UUID id, Authentication auth) {
        String actorId = extractActorId(auth);
        String tenantId = extractTenantId(auth);
        log.warn("SECURITY: DELETE attempt on audit event [{}] by actor [{}]", id, actorId);
        auditIngestionService.recordTamperAttempt(id, actorId, tenantId);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    private String extractActorId(Authentication auth) {
        return auth != null ? auth.getName() : "anonymous";
    }

    private String extractTenantId(Authentication auth) {
        // In a full implementation, tenant ID would be extracted from JWT claims
        // For now, use a default or retrieve from the authentication token
        if (auth != null && auth.getDetails() instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> details = (java.util.Map<String, Object>) auth.getDetails();
            Object tenant = details.get("tenant_id");
            if (tenant != null) {
                return tenant.toString();
            }
        }
        return DEFAULT_TENANT_ID;
    }
}
