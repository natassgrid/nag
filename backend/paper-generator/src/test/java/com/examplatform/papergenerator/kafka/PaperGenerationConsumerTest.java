/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.papergenerator.kafka;

import com.examplatform.papergenerator.domain.BlueprintTemplate;
import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.dto.BlueprintRule;
import com.examplatform.papergenerator.dto.PaperGenerationRequest;
import com.examplatform.papergenerator.exception.InsufficientQuestionsException;
import com.examplatform.papergenerator.repository.BlueprintTemplateRepository;
import com.examplatform.papergenerator.repository.PaperRepository;
import com.examplatform.papergenerator.service.PaperAssemblyService;
import com.examplatform.shared.messaging.EventPublisher;
import com.examplatform.shared.messaging.GenericDomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test suite for {@link PaperGenerationConsumer}.
 * Verifies Kafka, RabbitMQ, and Spring event ingestion, blueprint template resolution,
 * idempotency skipping, failure handling with DLQ dispatch, and loop protection.
 *
 * Validates: Requirements 8.7 (Async Paper Assembly Dispatch), Issue #114
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaperGenerationConsumer — Unit Test Suite")
class PaperGenerationConsumerTest {

    @Mock
    private PaperAssemblyService paperAssemblyService;

    @Mock
    private PaperRepository paperRepository;

    @Mock
    private BlueprintTemplateRepository blueprintTemplateRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<PaperGenerationRequest> requestCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> eventPayloadCaptor;

    private ObjectMapper objectMapper;
    private PaperGenerationConsumer consumer;

    private static final UUID EXAM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String SHIFT_ID = "SHIFT-MORNING-01";
    private static final String TENANT_ID = "tenant-001";
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        consumer = new PaperGenerationConsumer(
                paperAssemblyService,
                paperRepository,
                blueprintTemplateRepository,
                eventPublisher,
                objectMapper,
                5
        );
    }

    private Paper createMockPaper(UUID paperId, UUID examId, String shiftId) {
        Paper paper = Paper.builder()
                .name("General Science Paper")
                .examId(examId)
                .shiftId(shiftId)
                .status("DRAFT")
                .variant("SET-A")
                .paperRootHash("a1b2c3d4e5f6")
                .manifestDigest("f6e5d4c3b2a1")
                .difficultyScore(2.1)
                .isPractice(false)
                .build();
        ReflectionTestUtils.setField(paper, "id", paperId);
        return paper;
    }

    @Nested
    @DisplayName("1. Event Ingestion Protocols (Kafka, RabbitMQ, Spring in-memory)")
    class EventIngestionTests {

        @Test
        @DisplayName("onKafkaPaperGenerationRequest parses ConsumerRecord and dispatches to assembly service")
        void kafkaEventDispatchesAssembly() throws Exception {
            // Given
            Map<String, Object> payload = Map.of(
                    "eventType", "PAPER_GENERATION_REQUEST",
                    "examId", EXAM_ID.toString(),
                    "shiftId", SHIFT_ID,
                    "tenantId", TENANT_ID,
                    "requestedBy", USER_ID.toString(),
                    "blueprintRules", List.of(
                            Map.of("subject", "Physics", "topic", "Mechanics", "difficulty", "EASY", "questionCount", 5)
                    )
            );
            String json = objectMapper.writeValueAsString(payload);
            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                    PaperGenerationConsumer.PAPER_EVENTS_TOPIC, 0, 0, EXAM_ID.toString(), json);

            Paper mockPaper = createMockPaper(UUID.randomUUID(), EXAM_ID, SHIFT_ID);
            when(paperRepository.findByExamIdAndShiftIdAndTenantId(EXAM_ID, SHIFT_ID, TENANT_ID))
                    .thenReturn(List.of());
            when(paperAssemblyService.generatePaper(any(), eq(USER_ID), eq(TENANT_ID)))
                    .thenReturn(mockPaper);

            // When
            consumer.onKafkaPaperGenerationRequest(record);

            // Then
            verify(paperAssemblyService).generatePaper(requestCaptor.capture(), eq(USER_ID), eq(TENANT_ID));
            PaperGenerationRequest captured = requestCaptor.getValue();
            assertThat(captured.getExamId()).isEqualTo(EXAM_ID);
            assertThat(captured.getShiftId()).isEqualTo(SHIFT_ID);
            assertThat(captured.getBlueprintRules()).hasSize(1);
            assertThat(captured.getBlueprintRules().getFirst().getSubject()).isEqualTo("Physics");

            verify(eventPublisher).publish(
                    eq(PaperGenerationConsumer.PAPER_EVENTS_TOPIC),
                    any(),
                    eventPayloadCaptor.capture()
            );
            Map<String, Object> event = eventPayloadCaptor.getValue();
            assertThat(event.get("eventType")).isEqualTo("PAPER_GENERATION_COMPLETED");
            assertThat(event.get("examId")).isEqualTo(EXAM_ID.toString());
            assertThat(event.get("shiftId")).isEqualTo(SHIFT_ID);
        }

        @Test
        @DisplayName("onRabbitPaperGenerationRequest parses string message and triggers paper generation")
        void rabbitEventDispatchesAssembly() throws Exception {
            Map<String, Object> payload = Map.of(
                    "eventType", "PAPER_GENERATION_REQUEST",
                    "examId", EXAM_ID.toString(),
                    "shiftId", SHIFT_ID,
                    "tenantId", TENANT_ID,
                    "blueprintRules", List.of(
                            Map.of("subject", "Chemistry", "topic", "Organic", "difficulty", "HARD", "questionCount", 3)
                    )
            );
            String json = objectMapper.writeValueAsString(payload);

            Paper mockPaper = createMockPaper(UUID.randomUUID(), EXAM_ID, SHIFT_ID);
            when(paperRepository.findByExamIdAndShiftIdAndTenantId(EXAM_ID, SHIFT_ID, TENANT_ID))
                    .thenReturn(List.of());
            when(paperAssemblyService.generatePaper(any(), any(), eq(TENANT_ID)))
                    .thenReturn(mockPaper);

            consumer.onRabbitPaperGenerationRequest(json);

            verify(paperAssemblyService).generatePaper(any(), any(), eq(TENANT_ID));
        }

        @Test
        @DisplayName("onSpringPaperGenerationRequest listens to GenericDomainEvent and triggers generation")
        void springInMemoryEventDispatchesAssembly() throws Exception {
            Map<String, Object> payload = Map.of(
                    "eventType", "PAPER_GENERATION_REQUEST",
                    "examId", EXAM_ID.toString(),
                    "shiftId", SHIFT_ID,
                    "tenantId", TENANT_ID,
                    "blueprintRules", List.of(
                            Map.of("subject", "Mathematics", "topic", "Algebra", "difficulty", "MEDIUM", "questionCount", 4)
                    )
            );
            String json = objectMapper.writeValueAsString(payload);
            GenericDomainEvent domainEvent = new GenericDomainEvent(
                    PaperGenerationConsumer.PAPER_EVENTS_TOPIC, EXAM_ID.toString(), json);

            Paper mockPaper = createMockPaper(UUID.randomUUID(), EXAM_ID, SHIFT_ID);
            when(paperRepository.findByExamIdAndShiftIdAndTenantId(EXAM_ID, SHIFT_ID, TENANT_ID))
                    .thenReturn(List.of());
            when(paperAssemblyService.generatePaper(any(), any(), eq(TENANT_ID)))
                    .thenReturn(mockPaper);

            consumer.onSpringPaperGenerationRequest(domainEvent);

            verify(paperAssemblyService).generatePaper(any(), any(), eq(TENANT_ID));
        }
    }

    @Nested
    @DisplayName("2. Blueprint Template ID Resolution")
    class BlueprintTemplateResolutionTests {

        @Test
        @DisplayName("Should resolve blueprint rules from blueprintTemplateId when direct rules are omitted")
        void resolvesRulesFromTemplateId() throws Exception {
            UUID templateId = UUID.randomUUID();
            List<BlueprintRule> templateRules = List.of(
                    BlueprintRule.builder().subject("Biology").topic("Genetics").difficulty("MEDIUM").questionCount(10).build()
            );
            BlueprintTemplate template = BlueprintTemplate.builder()
                    .name("Biology Standard Blueprint")
                    .rulesJson(objectMapper.writeValueAsString(templateRules))
                    .build();

            when(blueprintTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
            when(paperRepository.findByExamIdAndShiftIdAndTenantId(EXAM_ID, SHIFT_ID, TENANT_ID)).thenReturn(List.of());

            Paper mockPaper = createMockPaper(UUID.randomUUID(), EXAM_ID, SHIFT_ID);
            when(paperAssemblyService.generatePaper(any(), any(), eq(TENANT_ID))).thenReturn(mockPaper);

            Map<String, Object> payload = Map.of(
                    "eventType", "PAPER_GENERATION_REQUEST",
                    "examId", EXAM_ID.toString(),
                    "shiftId", SHIFT_ID,
                    "tenantId", TENANT_ID,
                    "blueprintTemplateId", templateId.toString()
            );

            consumer.processPaperGenerationRequest(objectMapper.writeValueAsString(payload), null);

            verify(blueprintTemplateRepository).findById(templateId);
            verify(paperAssemblyService).generatePaper(requestCaptor.capture(), any(), eq(TENANT_ID));
            assertThat(requestCaptor.getValue().getBlueprintRules()).hasSize(1);
            assertThat(requestCaptor.getValue().getBlueprintRules().getFirst().getSubject()).isEqualTo("Biology");
        }
    }

    @Nested
    @DisplayName("3. Idempotency Check")
    class IdempotencyTests {

        @Test
        @DisplayName("Should skip paper generation if paper already exists for same examId and shiftId")
        void skipsDuplicateGenerationWhenPaperExists() throws Exception {
            Paper existingPaper = createMockPaper(UUID.randomUUID(), EXAM_ID, SHIFT_ID);
            when(paperRepository.findByExamIdAndShiftIdAndTenantId(EXAM_ID, SHIFT_ID, TENANT_ID))
                    .thenReturn(List.of(existingPaper));

            Map<String, Object> payload = Map.of(
                    "eventType", "PAPER_GENERATION_REQUEST",
                    "examId", EXAM_ID.toString(),
                    "shiftId", SHIFT_ID,
                    "tenantId", TENANT_ID,
                    "blueprintRules", List.of(
                            Map.of("subject", "Math", "topic", "Calculus", "difficulty", "EASY", "questionCount", 2)
                    )
            );

            consumer.processPaperGenerationRequest(objectMapper.writeValueAsString(payload), null);

            // Verify generation was NOT invoked
            verify(paperAssemblyService, never()).generatePaper(any(), any(), any());

            // Verify PAPER_GENERATION_COMPLETED was still published for idempotency notification
            verify(eventPublisher).publish(
                    eq(PaperGenerationConsumer.PAPER_EVENTS_TOPIC),
                    any(),
                    eventPayloadCaptor.capture()
            );
            Map<String, Object> event = eventPayloadCaptor.getValue();
            assertThat(event.get("eventType")).isEqualTo("PAPER_GENERATION_COMPLETED");
        }
    }

    @Nested
    @DisplayName("4. Error Handling & Dead Letter Queue (DLQ)")
    class ErrorAndDlqTests {

        @Test
        @DisplayName("Should publish PAPER_GENERATION_FAILED and DLQ event on InsufficientQuestionsException")
        void publishesFailedAndDlqOnQuestionDeficit() throws Exception {
            when(paperRepository.findByExamIdAndShiftIdAndTenantId(EXAM_ID, SHIFT_ID, TENANT_ID)).thenReturn(List.of());
            when(paperAssemblyService.generatePaper(any(), any(), eq(TENANT_ID)))
                    .thenThrow(new InsufficientQuestionsException("Blueprint cannot be satisfied", List.of()));

            Map<String, Object> payload = Map.of(
                    "eventType", "PAPER_GENERATION_REQUEST",
                    "examId", EXAM_ID.toString(),
                    "shiftId", SHIFT_ID,
                    "tenantId", TENANT_ID,
                    "blueprintRules", List.of(
                            Map.of("subject", "Physics", "topic", "Optics", "difficulty", "HARD", "questionCount", 50)
                    )
            );

            consumer.processPaperGenerationRequest(objectMapper.writeValueAsString(payload), null);

            // Verify PAPER_GENERATION_FAILED on main topic
            verify(eventPublisher).publish(
                    eq(PaperGenerationConsumer.PAPER_EVENTS_TOPIC),
                    eq(EXAM_ID.toString()),
                    eventPayloadCaptor.capture()
            );
            assertThat(eventPayloadCaptor.getValue().get("eventType")).isEqualTo("PAPER_GENERATION_FAILED");

            // Verify DLQ event published
            verify(eventPublisher).publish(
                    eq(PaperGenerationConsumer.PAPER_EVENTS_DLQ_TOPIC),
                    eq(EXAM_ID.toString()),
                    any()
            );
        }

        @Test
        @DisplayName("Should publish DLQ event when invalid template ID is supplied")
        void publishesDlqOnMissingTemplate() throws Exception {
            UUID unknownTemplateId = UUID.randomUUID();
            when(paperRepository.findByExamIdAndShiftIdAndTenantId(EXAM_ID, SHIFT_ID, TENANT_ID)).thenReturn(List.of());
            when(blueprintTemplateRepository.findById(unknownTemplateId)).thenReturn(Optional.empty());

            Map<String, Object> payload = Map.of(
                    "eventType", "PAPER_GENERATION_REQUEST",
                    "examId", EXAM_ID.toString(),
                    "shiftId", SHIFT_ID,
                    "tenantId", TENANT_ID,
                    "blueprintTemplateId", unknownTemplateId.toString()
            );

            consumer.processPaperGenerationRequest(objectMapper.writeValueAsString(payload), null);

            verify(eventPublisher).publish(
                    eq(PaperGenerationConsumer.PAPER_EVENTS_DLQ_TOPIC),
                    eq(EXAM_ID.toString()),
                    any()
            );
        }
    }

    @Nested
    @DisplayName("5. Loop Prevention & Non-Request Filtering")
    class LoopPreventionTests {

        @Test
        @DisplayName("Should silently ignore PAPER_GENERATION_COMPLETED and PAPER_GENERATION_FAILED events")
        void ignoresCompletionEvents() throws Exception {
            Map<String, Object> completedEvent = Map.of(
                    "eventType", "PAPER_GENERATION_COMPLETED",
                    "paperId", UUID.randomUUID().toString(),
                    "examId", EXAM_ID.toString(),
                    "shiftId", SHIFT_ID
            );

            Map<String, Object> failedEvent = Map.of(
                    "eventType", "PAPER_GENERATION_FAILED",
                    "examId", EXAM_ID.toString(),
                    "shiftId", SHIFT_ID
            );

            consumer.processPaperGenerationRequest(objectMapper.writeValueAsString(completedEvent), null);
            consumer.processPaperGenerationRequest(objectMapper.writeValueAsString(failedEvent), null);

            verify(paperAssemblyService, never()).generatePaper(any(), any(), any());
            verify(eventPublisher, never()).publish(any(), any(), any());
        }

        @Test
        @DisplayName("Should silently ignore serialized Paper entity events")
        void ignoresPaperEntityEvents() throws Exception {
            Map<String, Object> paperEntity = Map.of(
                    "id", UUID.randomUUID().toString(),
                    "paperRootHash", "abcdef123456",
                    "manifestDigest", "123456abcdef",
                    "paperDefinitionJson", "{\"questionIds\":[]}"
            );

            consumer.processPaperGenerationRequest(objectMapper.writeValueAsString(paperEntity), null);

            verify(paperAssemblyService, never()).generatePaper(any(), any(), any());
        }
    }
}
