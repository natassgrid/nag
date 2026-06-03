package com.examplatform.identity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Processes pending key revocations within 60 seconds of a Security_Admin trigger.
 * Runs on a fixed 60-second schedule to ensure timely key destruction.
 *
 * Validates: Requirements 16.4, 16.5
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeyRevocationScheduler {

    private final VaultCryptoService vaultCryptoService;

    private final ConcurrentLinkedQueue<String> pendingRevocations = new ConcurrentLinkedQueue<>();

    /**
     * Runs every 60 seconds, processes all pending key revocations.
     * Ensures revocation happens within 60 seconds of the Security_Admin trigger.
     */
    @Scheduled(fixedDelay = 60_000)
    public void processPendingRevocations() {
        String keyName;
        while ((keyName = pendingRevocations.poll()) != null) {
            try {
                vaultCryptoService.revokeKey(keyName);
                log.warn("SECURITY: Key [{}] revoked by scheduled task", keyName);
            } catch (Exception e) {
                log.error("Failed to revoke key [{}]: {}", keyName, e.getMessage());
                // Do NOT re-queue — revocation failure is a security event that needs manual intervention
            }
        }
    }

    /**
     * Schedule a key for revocation. Will be processed within 60 seconds.
     *
     * @param keyName name of the Vault Transit key to revoke
     */
    public void scheduleRevocation(String keyName) {
        log.warn("SECURITY: Key [{}] scheduled for revocation within 60 seconds", keyName);
        pendingRevocations.offer(keyName);
    }

    /**
     * Returns the current number of pending revocations (for monitoring).
     *
     * @return pending revocation count
     */
    public int pendingCount() {
        return pendingRevocations.size();
    }
}
