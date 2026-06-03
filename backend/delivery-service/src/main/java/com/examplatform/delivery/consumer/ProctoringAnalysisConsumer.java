package com.examplatform.delivery.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Kafka consumer that processes proctoring frames/snapshots for AI analysis.
 * Stub implementation: randomly flags some frames to simulate ML model detection.
 * Production would use an actual ML model for face detection, object detection, etc.
 *
 * Publishes audit events for detected anomalies:
 * - no-face-detected
 * - multiple-faces-detected
 * - prohibited-object-detected
 *
 * Validates: Requirements 11.3, 11.4, 11.5
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProctoringAnalysisConsumer {

    private static final String AUDIT_TOPIC = "exam.audit.events";
    private static final String[] DETECTION_TYPES = {
            "no-face-detected",
            "multiple-faces-detected",
            "prohibited-object-detected"
    };

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();

    /**
     * Consumes proctoring snapshot events and performs stub AI analysis.
     * In production, this would invoke an ML model for face/object detection.
     *
     * @param event the proctoring snapshot event from exam.proctoring.alerts
     */
    @KafkaListener(topics = "exam.proctoring.alerts", groupId = "delivery-proctoring")
    @SuppressWarnings("unchecked")
    public void analyze(Map<String, Object> event) {
        String sessionId = (String) event.get("sessionId");
        String candidateId = (String) event.get("candidateId");
        String snapshotRef = (String) event.get("snapshotRef");

        log.debug("Analyzing proctoring frame for session={}, snapshot={}", sessionId, snapshotRef);

        // Stub AI analysis: ~10% chance of flagging each detection type
        for (String detectionType : DETECTION_TYPES) {
            if (random.nextDouble() < 0.10) {
                publishAuditEvent(detectionType, sessionId, candidateId, snapshotRef);
            }
        }
    }

    private void publishAuditEvent(String detectionType, String sessionId,
                                   String candidateId, String snapshotRef) {
        try {
            Map<String, Object> auditEvent = new HashMap<>();
            auditEvent.put("eventType", detectionType);
            auditEvent.put("sessionId", sessionId);
            auditEvent.put("candidateId", candidateId);
            auditEvent.put("snapshotRef", snapshotRef);
            auditEvent.put("source", "ai-proctoring-analysis");
            auditEvent.put("confidence", 0.85 + random.nextDouble() * 0.15); // Stub confidence: 0.85–1.0
            auditEvent.put("occurredAt", Instant.now().toString());

            kafkaTemplate.send(AUDIT_TOPIC, sessionId, auditEvent);
            log.warn("AI proctoring alert: type={}, session={}, candidate={}",
                    detectionType, sessionId, candidateId);
        } catch (Exception e) {
            log.error("Failed to publish AI proctoring audit event [type={}, session={}]: {}",
                    detectionType, sessionId, e.getMessage());
        }
    }
}
