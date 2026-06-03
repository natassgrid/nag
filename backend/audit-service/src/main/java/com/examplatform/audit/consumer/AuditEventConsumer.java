package com.examplatform.audit.consumer;

import com.examplatform.audit.service.AuditIngestionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens for audit events on the
 * {@code exam.audit.events} topic. Deserializes incoming JSON messages,
 * extracts audit fields, and delegates to {@link AuditIngestionService}
 * for SHA-256 hashing, HSM signing, and immutable persistence.
 *
 * Validates: Requirements 15.1, 15.2
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditIngestionService auditIngestionService;
    private final ObjectMapper objectMapper;

    /**
     * Listener for audit events. Deserializes the JSON payload, extracts
     * event metadata fields, and invokes ingestion (hash + sign + persist).
     *
     * @param message the raw event payload from Kafka
     */
    @KafkaListener(topics = "exam.audit.events", groupId = "audit-service")
    public void onAuditEvent(String message) {
        log.debug("Received audit event: {}", message);
        try {
            JsonNode root = objectMapper.readTree(message);

            String eventType = getTextOrDefault(root, "eventType", "UNKNOWN");
            String actorId = getTextOrDefault(root, "actorId", "00000000-0000-0000-0000-000000000000");
            String resource = getTextOrDefault(root, "resource", "unknown");
            String ipAddress = getTextOrNull(root, "ipAddress");
            String deviceFingerprint = getTextOrNull(root, "deviceFingerprint");
            String tenantId = getTextOrDefault(root, "tenantId", "default");

            auditIngestionService.ingest(message, eventType, actorId, resource,
                    ipAddress, deviceFingerprint, tenantId);

            log.info("Successfully ingested audit event type=[{}] actor=[{}]", eventType, actorId);
        } catch (Exception e) {
            log.error("Failed to process audit event: {}", e.getMessage(), e);
            // In production, this would be sent to a dead-letter topic
        }
    }

    private String getTextOrDefault(JsonNode root, String field, String defaultValue) {
        JsonNode node = root.get(field);
        return (node != null && !node.isNull()) ? node.asText() : defaultValue;
    }

    private String getTextOrNull(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return (node != null && !node.isNull()) ? node.asText() : null;
    }
}
