package com.examplatform.audit.service;

import com.examplatform.audit.domain.AuditEvent;
import com.examplatform.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.UUID;

/**
 * Service responsible for ingesting raw audit event payloads from Kafka.
 * Computes SHA-256 hash of the payload, signs with HSM ECDSA P-256 key,
 * and persists the immutable AuditEvent record.
 *
 * Validates: Requirements 15.2, 15.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuditIngestionService {

    private static final String SIGNING_KEY_ID = "audit-signing-key-ecdsa-p256";

    private final AuditEventRepository auditEventRepository;
    private final VaultCryptoService vaultCryptoService;

    /**
     * Ingest a raw audit event payload from Kafka:
     * 1. Compute SHA-256 hash of the payload
     * 2. Sign the hash with the HSM ECDSA P-256 key
     * 3. Persist the AuditEvent with payloadHash, hsmSignature, signingKeyId
     *
     * @param eventPayload      raw JSON payload
     * @param eventType         type of audit event
     * @param actorId           ID of the actor who triggered the event
     * @param resource          resource being acted upon
     * @param ipAddress         IP address of the actor (nullable)
     * @param deviceFingerprint device fingerprint (nullable)
     * @param tenantId          tenant identifier
     * @return the persisted AuditEvent
     */
    public AuditEvent ingest(String eventPayload, String eventType, String actorId,
                             String resource, String ipAddress, String deviceFingerprint,
                             String tenantId) {
        // 1. Compute SHA-256 of payload
        String payloadHash = sha256(eventPayload);

        // 2. Sign with Vault Transit (ECDSA P-256)
        String hsmSignature = vaultCryptoService.sign(SIGNING_KEY_ID, payloadHash);

        // 3. Persist
        AuditEvent event = AuditEvent.builder()
                .eventType(eventType)
                .actorId(UUID.fromString(actorId))
                .resource(resource)
                .ipAddress(ipAddress)
                .deviceFingerprint(deviceFingerprint)
                .occurredAt(Instant.now())
                .tenantId(tenantId)
                .payloadHash(payloadHash)
                .hsmSignature(hsmSignature)
                .signingKeyId(SIGNING_KEY_ID)
                .eventPayload(eventPayload)
                .build();

        AuditEvent saved = auditEventRepository.save(event);
        log.info("Persisted audit event [{}] type=[{}] actor=[{}]", saved.getId(), eventType, actorId);
        return saved;
    }

    /**
     * Reject UPDATE/DELETE attempts by creating a TAMPER_ATTEMPT audit event.
     * Called when any PUT or DELETE request targets an existing audit record.
     *
     * @param targetEventId the audit event ID that was targeted
     * @param actorId       the actor attempting the modification
     * @param tenantId      tenant identifier
     * @return the tamper-attempt audit event
     */
    public AuditEvent recordTamperAttempt(UUID targetEventId, String actorId, String tenantId) {
        String payload = String.format(
                "{\"targetEventId\":\"%s\",\"actorId\":\"%s\",\"type\":\"TAMPER_ATTEMPT\"}",
                targetEventId, actorId);
        log.warn("SECURITY: Tamper attempt detected on event [{}] by actor [{}]", targetEventId, actorId);
        return ingest(payload, "TAMPER_ATTEMPT", actorId,
                "audit:event:" + targetEventId, null, null, tenantId);
    }

    /**
     * Compute SHA-256 hex digest of the input string.
     *
     * @param input the string to hash
     * @return lowercase hex-encoded SHA-256 hash
     */
    String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
