/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.delivery.consumer;

import com.examplatform.shared.event.ProctoringEvents;
import com.examplatform.shared.messaging.EventPublisher;
import com.examplatform.shared.messaging.GenericDomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;

/**
 * Consumer that processes proctoring frames/snapshots for AI analysis.
 * Supports Kafka (microservices mode), RabbitMQ (macro mode), and in-memory Spring events (monolith mode).
 * Stub implementation: randomly flags some frames to simulate ML model detection.
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
public class ProctoringAnalysisConsumer {

    public static final String PROCTORING_TOPIC = "exam.proctoring.alerts";
    private static final String AUDIT_TOPIC = "exam.audit.events";
    private static final String[] DETECTION_TYPES = {
            "no-face-detected",
            "multiple-faces-detected",
            "prohibited-object-detected"
    };

    private final EventPublisher eventPublisher;
    private final Random random;
    private final ObjectMapper objectMapper;

    @Autowired
    public ProctoringAnalysisConsumer(EventPublisher eventPublisher) {
        this(eventPublisher, new Random(), new ObjectMapper());
    }

    public ProctoringAnalysisConsumer(EventPublisher eventPublisher, Random random) {
        this(eventPublisher, random, new ObjectMapper());
    }

    public ProctoringAnalysisConsumer(EventPublisher eventPublisher, Random random, ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.random = random;
        this.objectMapper = objectMapper;
    }

    /**
     * Consumes proctoring snapshot events via Kafka and performs stub AI analysis.
     */
    @KafkaListener(topics = PROCTORING_TOPIC, groupId = "delivery-proctoring")
    public void analyze(Object event) {
        ProctoringEvents.SnapshotCaptured snapshot = parseSnapshot(event);
        if (snapshot != null) {
            processEvent(snapshot);
        }
    }

    /**
     * Consumes proctoring snapshot events via RabbitMQ and performs stub AI analysis.
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "proctoring.alerts.queue", durable = "true"),
                    exchange = @Exchange(value = "exam.events", type = ExchangeTypes.TOPIC),
                    key = PROCTORING_TOPIC
            )
    )
    public void analyzeRabbit(Object message) {
        ProctoringEvents.SnapshotCaptured snapshot = parseSnapshot(message);
        if (snapshot != null) {
            processEvent(snapshot);
        }
    }

    /**
     * Consumes proctoring snapshot events via Spring in-memory events (monolith mode).
     */
    @EventListener
    public void onSpringProctoringAlert(GenericDomainEvent event) {
        if (PROCTORING_TOPIC.equals(event.topic())) {
            ProctoringEvents.SnapshotCaptured snapshot = parseSnapshot(event.payload());
            if (snapshot != null) {
                processEvent(snapshot);
            }
        }
    }

    public void processEvent(ProctoringEvents.SnapshotCaptured event) {
        if (event == null) return;
        String sessionId = event.sessionId();
        String candidateId = event.candidateId();
        String snapshotRef = event.snapshotRef();

        log.debug("Analyzing proctoring frame for session={}, snapshot={}", sessionId, snapshotRef);

        // Stub AI analysis: ~10% chance of flagging each detection type
        for (String detectionType : DETECTION_TYPES) {
            if (random.nextDouble() < 0.10) {
                publishAuditEvent(detectionType, sessionId, candidateId, snapshotRef);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void processEvent(Map<String, Object> event) {
        if (event == null) return;
        String sessionId = (String) event.get("sessionId");
        String candidateId = (String) event.get("candidateId");
        String snapshotRef = (String) event.get("snapshotRef");
        String tenantId = (String) event.get("tenantId");
        int imageSize = event.get("imageSize") instanceof Number n ? n.intValue() : 0;

        processEvent(new ProctoringEvents.SnapshotCaptured(
                (String) event.getOrDefault("eventType", "SNAPSHOT_CAPTURED"),
                sessionId, candidateId, snapshotRef, tenantId,
                (String) event.get("capturedAt"), imageSize));
    }

    private ProctoringEvents.SnapshotCaptured parseSnapshot(Object payload) {
        if (payload == null) return null;
        try {
            if (payload instanceof ProctoringEvents.SnapshotCaptured sc) {
                return sc;
            }
            if (payload instanceof Map<?, ?> map) {
                return objectMapper.convertValue(map, ProctoringEvents.SnapshotCaptured.class);
            }
            if (payload instanceof Message amqpMsg) {
                return objectMapper.readValue(amqpMsg.getBody(), ProctoringEvents.SnapshotCaptured.class);
            }
            if (payload instanceof byte[] bytes) {
                return objectMapper.readValue(bytes, ProctoringEvents.SnapshotCaptured.class);
            }
            if (payload instanceof String s) {
                return objectMapper.readValue(s, ProctoringEvents.SnapshotCaptured.class);
            }
            return objectMapper.convertValue(payload, ProctoringEvents.SnapshotCaptured.class);
        } catch (Exception e) {
            log.error("Failed to parse proctoring snapshot event: {}", e.getMessage());
            return null;
        }
    }

    private void publishAuditEvent(String detectionType, String sessionId,
                                   String candidateId, String snapshotRef) {
        try {
            double confidence = 0.85 + random.nextDouble() * 0.15; // Stub confidence: 0.85–1.0
            ProctoringEvents.AuditAlert auditEvent = ProctoringEvents.AuditAlert.of(
                    detectionType, sessionId, candidateId, snapshotRef,
                    "ai-proctoring-analysis", confidence
            );

            eventPublisher.publish(AUDIT_TOPIC, sessionId, auditEvent);
            log.warn("AI proctoring alert: type={}, session={}, candidate={}",
                    detectionType, sessionId, candidateId);
        } catch (Exception e) {
            log.error("Failed to publish AI proctoring audit event [type={}, session={}]: {}",
                    detectionType, sessionId, e.getMessage());
        }
    }
}
