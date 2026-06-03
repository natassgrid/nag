package com.examplatform.delivery.controller;

import com.examplatform.delivery.service.OfflineDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for offline exam delivery operations.
 * Handles pre-loading exam packages for offline use and reconciling data on reconnect.
 *
 * Validates: Requirements 9.7
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/offline")
@RequiredArgsConstructor
public class OfflineDeliveryController {

    private final OfflineDeliveryService offlineDeliveryService;

    /**
     * Pre-loads an exam package for offline delivery at a specific center.
     *
     * @param sessionId the exam session UUID
     * @param centerId  the examination center identifier
     * @param jwt       the authenticated user's JWT
     * @return the decrypted exam package content
     */
    @PostMapping("/preload")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER', 'CENTER_ADMIN')")
    public ResponseEntity<Map<String, String>> preloadExamPackage(
            @PathVariable UUID sessionId,
            @RequestParam String centerId,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        log.info("POST preload offline package for session={}, center={}, tenant={}",
                sessionId, centerId, tenantId);

        String decryptedPackage = offlineDeliveryService.preloadExamPackage(sessionId, centerId, tenantId);

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId.toString(),
                "status", "PRELOADED",
                "content", decryptedPackage
        ));
    }

    /**
     * Reconciles offline data when connectivity is restored.
     *
     * @param sessionId the exam session UUID
     * @param jwt       the authenticated user's JWT
     * @return confirmation of reconciliation
     */
    @PostMapping("/reconcile")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER', 'CENTER_ADMIN')")
    public ResponseEntity<Map<String, String>> reconcileOnReconnect(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        log.info("POST reconcile offline data for session={}, tenant={}", sessionId, tenantId);

        offlineDeliveryService.reconcileOnReconnect(sessionId, tenantId);

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId.toString(),
                "status", "RECONCILED"
        ));
    }

    private String extractTenantId(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        return tenantId != null ? tenantId : "default";
    }
}
