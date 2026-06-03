package com.examplatform.audit.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens for audit events on the
 * {@code exam.audit.events} topic. Persists immutable audit records
 * into the partitioned audit_event table.
 */
@Slf4j
@Component
public class AuditEventConsumer {

    /**
     * Stub listener for audit events. Will be implemented to deserialize
     * audit event payloads, compute payload hashes, and persist to the
     * immutable audit trail.
     *
     * @param message the raw event payload
     */
    @KafkaListener(topics = "exam.audit.events", groupId = "audit-service")
    public void onAuditEvent(String message) {
        log.info("Received audit event: {}", message);
        // TODO: Deserialize, compute payload hash, verify HSM signature, persist
    }
}
