package com.examplatform.identity.controller;

import com.examplatform.identity.service.AuditEventPublisher;
import com.examplatform.identity.service.KeyRevocationScheduler;
import com.examplatform.identity.service.VaultCryptoService;
import com.examplatform.shared.audit.AuditEventType;
import com.examplatform.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Security_Admin endpoints for Vault Transit key management.
 * Provides key rotation and key revocation scheduling.
 *
 * Validates: Requirements 16.3, 16.4, 16.5
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/identity/admin/vault")
@RequiredArgsConstructor
public class VaultAdminController {

    private final KeyRevocationScheduler keyRevocationScheduler;
    private final VaultCryptoService vaultCryptoService;
    private final AuditEventPublisher auditEventPublisher;

    /**
     * Schedule a Vault Transit key for revocation within 60 seconds.
     * Only accessible by Security_Admin role.
     */
    @PostMapping("/keys/{keyName}/revoke")
    @PreAuthorize("hasRole('SECURITY_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> scheduleKeyRevocation(
            @PathVariable String keyName,
            Authentication authentication,
            HttpServletRequest request) {
        keyRevocationScheduler.scheduleRevocation(keyName);
        auditEventPublisher.publish(AuditEventType.KEY_REVOCATION,
                authentication.getName(), "vault:key:" + keyName,
                request.getRemoteAddr(), null,
                Map.of("keyName", keyName, "action", "revoke-scheduled"));
        log.warn("SECURITY: Key [{}] revocation scheduled by [{}]", keyName, authentication.getName());
        return ResponseEntity.accepted()
                .body(ApiResponse.success(null, "Key " + keyName + " scheduled for revocation within 60 seconds."));
    }

    /**
     * Rotate a Vault Transit key immediately.
     * Only accessible by Security_Admin role.
     */
    @PostMapping("/keys/{keyName}/rotate")
    @PreAuthorize("hasRole('SECURITY_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> rotateKey(
            @PathVariable String keyName,
            Authentication authentication,
            HttpServletRequest request) {
        vaultCryptoService.rotateKey(keyName);
        auditEventPublisher.publish(AuditEventType.KEY_REVOCATION,
                authentication.getName(), "vault:key:" + keyName,
                request.getRemoteAddr(), null,
                Map.of("keyName", keyName, "action", "rotate"));
        return ResponseEntity.ok(ApiResponse.success(null, "Key " + keyName + " rotated successfully."));
    }
}
