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

package com.examplatform.papergenerator.kafka;

import com.examplatform.papergenerator.domain.BlueprintTemplate;
import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.dto.BlueprintRule;
import com.examplatform.papergenerator.dto.PaperGenerationRequest;
import com.examplatform.papergenerator.repository.BlueprintTemplateRepository;
import com.examplatform.papergenerator.repository.PaperRepository;
import com.examplatform.papergenerator.service.PaperAssemblyService;
import com.examplatform.shared.messaging.EventPublisher;
import com.examplatform.shared.messaging.GenericDomainEvent;
import com.examplatform.shared.tenant.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Async consumer for paper generation request jobs.
 * Listens on topic {@code exam.paper.events} and triggers blueprint-driven paper generation
 * workflows when a request is received.
 * Supports Kafka, RabbitMQ, and in-memory Spring events.
 *
 * Validates: Requirements 8.7 (Async Paper Assembly Dispatch), Issue #114
 */
@Slf4j
@Component
public class PaperGenerationConsumer {

    public static final String PAPER_EVENTS_TOPIC = "exam.paper.events";
    public static final String PAPER_EVENTS_DLQ_TOPIC = "exam.paper.events.dlq";

    public static final String EVENT_PAPER_GENERATION_REQUEST = "PAPER_GENERATION_REQUEST";
    public static final String EVENT_PAPER_GENERATION_COMPLETED = "PAPER_GENERATION_COMPLETED";
    public static final String EVENT_PAPER_GENERATION_FAILED = "PAPER_GENERATION_FAILED";

    private final PaperAssemblyService paperAssemblyService;
    private final PaperRepository paperRepository;
    private final BlueprintTemplateRepository blueprintTemplateRepository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final Semaphore concurrencyLimiter;

    @Autowired
    public PaperGenerationConsumer(
            PaperAssemblyService paperAssemblyService,
            PaperRepository paperRepository,
            BlueprintTemplateRepository blueprintTemplateRepository,
            EventPublisher eventPublisher,
            ObjectMapper objectMapper,
            @Value("${app.paper-generation.concurrency-limit:10}") int concurrencyLimit) {
        this.paperAssemblyService = paperAssemblyService;
        this.paperRepository = paperRepository;
        this.blueprintTemplateRepository = blueprintTemplateRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.concurrencyLimiter = new Semaphore(concurrencyLimit > 0 ? concurrencyLimit : 10);
    }

    /**
     * Consumes paper generation requests via Kafka.
     */
    @KafkaListener(topics = PAPER_EVENTS_TOPIC, groupId = "paper-generator")
    public void onKafkaPaperGenerationRequest(ConsumerRecord<String, String> record) {
        log.info("Paper generation request received via Kafka: key={}", record != null ? record.key() : null);
        String payload = record != null ? record.value() : null;
        String key = record != null ? record.key() : null;
        processPaperGenerationRequest(payload, key);
    }

    /**
     * Consumes paper generation requests via RabbitMQ.
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "paper.generation.queue", durable = "true"),
                    exchange = @Exchange(value = "exam.events", type = ExchangeTypes.TOPIC),
                    key = PAPER_EVENTS_TOPIC
            )
    )
    public void onRabbitPaperGenerationRequest(Object message) {
        log.info("Paper generation request received via RabbitMQ: {}", message);
        String payload = extractPayloadString(message);
        processPaperGenerationRequest(payload, null);
    }

    /**
     * Consumes paper generation requests via in-memory Spring ApplicationEvents.
     */
    @EventListener
    public void onSpringPaperGenerationRequest(GenericDomainEvent event) {
        if (!PAPER_EVENTS_TOPIC.equals(event.topic())) {
            return;
        }
        log.info("Paper generation request received via Spring in-memory event: key={}", event.key());
        String payload = extractPayloadString(event.payload());
        processPaperGenerationRequest(payload, event.key());
    }

    /**
     * Core processing method for incoming paper generation requests across all brokers.
     * Enforces concurrency limiting, idempotency checks, blueprint resolution,
     * paper assembly delegation, and event/DLQ publishing.
     *
     * @param payload the raw JSON payload
     * @param key     the routing/partition key
     */
    public void processPaperGenerationRequest(String payload, String key) {
        if (payload == null || payload.isBlank()) {
            log.debug("Received empty paper generation payload; skipping.");
            return;
        }

        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(payload);
        } catch (Exception e) {
            log.error("Failed to parse paper generation JSON payload: {}", e.getMessage());
            publishDlqEvent(null, null, "default", "Invalid JSON payload: " + e.getMessage(), payload);
            return;
        }

        if (!rootNode.isObject()) {
            return;
        }

        // 1. Skip outbound/result/audit events to prevent loops
        if (rootNode.has("eventType")) {
            String eventType = rootNode.get("eventType").asText();
            if (EVENT_PAPER_GENERATION_COMPLETED.equalsIgnoreCase(eventType)
                    || EVENT_PAPER_GENERATION_FAILED.equalsIgnoreCase(eventType)
                    || "PAPER_GENERATED".equalsIgnoreCase(eventType)
                    || "PAPER_APPROVED".equalsIgnoreCase(eventType)
                    || "PAPER_ENCRYPTED".equalsIgnoreCase(eventType)
                    || "BLUEPRINT_DEFICIT_ALERT".equalsIgnoreCase(eventType)
                    || "BLUEPRINT_DEFICIT_DETECTED".equalsIgnoreCase(eventType)) {
                log.debug("Ignoring non-request event type: {}", eventType);
                return;
            }
            if (!EVENT_PAPER_GENERATION_REQUEST.equalsIgnoreCase(eventType)
                    && !"GENERATE_PAPER".equalsIgnoreCase(eventType)
                    && !"PAPER_GENERATE".equalsIgnoreCase(eventType)
                    && !rootNode.has("examId")) {
                log.debug("Ignoring unrecognized event type without examId: {}", eventType);
                return;
            }
        }

        // Skip direct Paper entity serialization events that lack eventType
        if (rootNode.has("paperRootHash") && (rootNode.has("paperDefinitionJson") || rootNode.has("manifestDigest"))) {
            if (!rootNode.has("eventType") || !"PAPER_GENERATION_REQUEST".equalsIgnoreCase(rootNode.get("eventType").asText())) {
                log.debug("Ignoring Paper entity serialization broadcast.");
                return;
            }
        }

        // 2. Validate essential request attributes
        if (!rootNode.has("examId") || rootNode.get("examId").isNull()) {
            log.debug("No examId found in message payload; skipping non-generation event.");
            return;
        }

        UUID examId = parseUUID(getFieldText(rootNode, "examId"));
        String shiftId = getFieldText(rootNode, "shiftId");
        String tenantId = getFieldText(rootNode, "tenantId");
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }

        UUID requestedBy = parseUUID(getFieldText(rootNode, "requestedBy", "generatedBy", "userId"));
        if (requestedBy == null) {
            requestedBy = UUID.fromString("00000000-0000-0000-0000-000000000001");
        }

        String name = getFieldText(rootNode, "name");
        Boolean isPractice = rootNode.has("isPractice") && rootNode.get("isPractice").asBoolean();
        Integer paragraphQuestionCount = rootNode.has("paragraphQuestionCount") && !rootNode.get("paragraphQuestionCount").isNull()
                ? rootNode.get("paragraphQuestionCount").asInt() : null;
        Integer subQuestionsPerPassage = rootNode.has("subQuestionsPerPassage") && !rootNode.get("subQuestionsPerPassage").isNull()
                ? rootNode.get("subQuestionsPerPassage").asInt() : null;

        boolean permitAcquired = false;
        try {
            // 3. Rate-limiting / DB connection protection
            permitAcquired = concurrencyLimiter.tryAcquire(30, TimeUnit.SECONDS);
            if (!permitAcquired) {
                throw new IllegalStateException("Paper generation concurrency limit reached; timed out waiting for worker slot");
            }

            // 4. Idempotency check: skip if paper already generated for this exam+shift
            if (examId != null && shiftId != null && !shiftId.isBlank()) {
                List<Paper> existing = paperRepository.findByExamIdAndShiftIdAndTenantId(examId, shiftId, tenantId);
                if (!existing.isEmpty()) {
                    Paper existingPaper = existing.getFirst();
                    log.info("Paper already exists for examId={}, shiftId={}, tenantId={} (paperId={}) — skipping duplicate generation (Idempotent)",
                            examId, shiftId, tenantId, existingPaper.getId());
                    publishPaperGenerationCompleted(existingPaper, tenantId);
                    return;
                }
            }

            // 5. Blueprint rules resolution (from direct rules list or blueprintTemplateId)
            List<BlueprintRule> blueprintRules = null;
            if (rootNode.has("blueprintRules") && rootNode.get("blueprintRules").isArray()) {
                blueprintRules = objectMapper.convertValue(
                        rootNode.get("blueprintRules"), new TypeReference<List<BlueprintRule>>() {});
            }

            if ((blueprintRules == null || blueprintRules.isEmpty()) && rootNode.has("blueprintTemplateId")) {
                UUID templateId = parseUUID(getFieldText(rootNode, "blueprintTemplateId", "templateId"));
                if (templateId != null) {
                    Optional<BlueprintTemplate> templateOpt = blueprintTemplateRepository.findById(templateId);
                    if (templateOpt.isPresent()) {
                        String rulesJson = templateOpt.get().getRulesJson();
                        if (rulesJson != null && !rulesJson.isBlank()) {
                            blueprintRules = objectMapper.readValue(rulesJson, new TypeReference<List<BlueprintRule>>() {});
                        }
                    } else {
                        throw new IllegalArgumentException("Blueprint template not found for id: " + templateId);
                    }
                }
            }

            if (blueprintRules == null || blueprintRules.isEmpty()) {
                throw new IllegalArgumentException("Cannot generate paper: No blueprint rules provided or resolved from template");
            }

            // 6. Build request and execute assembly
            PaperGenerationRequest request = PaperGenerationRequest.builder()
                    .name(name)
                    .examId(examId)
                    .shiftId(shiftId)
                    .isPractice(isPractice)
                    .paragraphQuestionCount(paragraphQuestionCount)
                    .subQuestionsPerPassage(subQuestionsPerPassage)
                    .blueprintRules(blueprintRules)
                    .build();

            TenantContext.set(tenantId);
            Paper paper = paperAssemblyService.generatePaper(request, requestedBy, tenantId);

            // 7. Publish completion event
            publishPaperGenerationCompleted(paper, tenantId);
            log.info("Async paper generation completed successfully for paperId={}, examId={}, shiftId={}",
                    paper.getId(), examId, shiftId);

        } catch (Exception e) {
            log.error("Paper generation failed for examId={}, shiftId={}, tenantId={}: {}",
                    examId, shiftId, tenantId, e.getMessage(), e);

            publishPaperGenerationFailed(examId, shiftId, tenantId, e.getMessage(), e.getClass().getSimpleName());
            publishDlqEvent(examId, shiftId, tenantId, e.getMessage(), payload);
        } finally {
            if (permitAcquired) {
                concurrencyLimiter.release();
            }
            TenantContext.clear();
        }
    }

    private void publishPaperGenerationCompleted(Paper paper, String tenantId) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventType", EVENT_PAPER_GENERATION_COMPLETED);
            event.put("paperId", paper.getId() != null ? paper.getId().toString() : "");
            event.put("examId", paper.getExamId() != null ? paper.getExamId().toString() : "");
            event.put("shiftId", paper.getShiftId() != null ? paper.getShiftId() : "");
            event.put("name", paper.getName() != null ? paper.getName() : "");
            event.put("variant", paper.getVariant() != null ? paper.getVariant() : "SET-A");
            event.put("status", paper.getStatus() != null ? paper.getStatus() : "DRAFT");
            event.put("paperRootHash", paper.getPaperRootHash() != null ? paper.getPaperRootHash() : "");
            event.put("manifestDigest", paper.getManifestDigest() != null ? paper.getManifestDigest() : "");
            event.put("difficultyScore", paper.getDifficultyScore());
            event.put("isPractice", paper.isPractice());
            event.put("tenantId", tenantId);
            event.put("occurredAt", Instant.now().toString());

            String key = paper.getId() != null ? paper.getId().toString() : (paper.getExamId() != null ? paper.getExamId().toString() : "GLOBAL");
            eventPublisher.publish(PAPER_EVENTS_TOPIC, key, event);
        } catch (Exception e) {
            log.error("Failed to publish PAPER_GENERATION_COMPLETED event for paper {}: {}", paper.getId(), e.getMessage());
        }
    }

    private void publishPaperGenerationFailed(
            UUID examId, String shiftId, String tenantId, String errorMessage, String errorClass) {
        try {
            Map<String, Object> failEvent = new LinkedHashMap<>();
            failEvent.put("eventType", EVENT_PAPER_GENERATION_FAILED);
            failEvent.put("examId", examId != null ? examId.toString() : "");
            failEvent.put("shiftId", shiftId != null ? shiftId : "");
            failEvent.put("tenantId", tenantId);
            failEvent.put("errorMessage", errorMessage != null ? errorMessage : "Unknown error");
            failEvent.put("errorClass", errorClass != null ? errorClass : "Exception");
            failEvent.put("occurredAt", Instant.now().toString());

            String key = examId != null ? examId.toString() : "GLOBAL";
            eventPublisher.publish(PAPER_EVENTS_TOPIC, key, failEvent);
        } catch (Exception e) {
            log.error("Failed to publish PAPER_GENERATION_FAILED event: {}", e.getMessage());
        }
    }

    private void publishDlqEvent(
            UUID examId, String shiftId, String tenantId, String errorReason, String originalPayload) {
        try {
            Map<String, Object> dlqEvent = new LinkedHashMap<>();
            dlqEvent.put("deadLetterReason", errorReason);
            dlqEvent.put("examId", examId != null ? examId.toString() : "");
            dlqEvent.put("shiftId", shiftId != null ? shiftId : "");
            dlqEvent.put("tenantId", tenantId);
            dlqEvent.put("originalPayload", originalPayload);
            dlqEvent.put("failedAt", Instant.now().toString());

            String key = examId != null ? examId.toString() : "DLQ";
            eventPublisher.publish(PAPER_EVENTS_DLQ_TOPIC, key, dlqEvent);
        } catch (Exception e) {
            log.error("Failed to publish DLQ event to {}: {}", PAPER_EVENTS_DLQ_TOPIC, e.getMessage());
        }
    }

    private String extractPayloadString(Object message) {
        if (message == null) return null;
        if (message instanceof ConsumerRecord<?, ?> record) {
            Object val = record.value();
            return val != null ? val.toString() : null;
        }
        if (message instanceof Message amqpMsg) {
            return new String(amqpMsg.getBody(), StandardCharsets.UTF_8);
        }
        if (message instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (message instanceof String s) {
            return s;
        }
        try {
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            return message.toString();
        }
    }

    private String getFieldText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName) && !node.get(fieldName).isNull()) {
                return node.get(fieldName).asText();
            }
        }
        return null;
    }

    private UUID parseUUID(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
