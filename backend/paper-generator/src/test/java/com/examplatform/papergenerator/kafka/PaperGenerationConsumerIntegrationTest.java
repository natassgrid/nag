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

import com.examplatform.papergenerator.client.QuestionBankClient;
import com.examplatform.papergenerator.domain.BlueprintTemplate;
import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.dto.BlueprintRule;
import com.examplatform.papergenerator.dto.QuestionSummary;
import com.examplatform.papergenerator.repository.BlueprintTemplateRepository;
import com.examplatform.papergenerator.repository.PaperRepository;
import com.examplatform.papergenerator.support.AbstractIntegrationTest;
import com.examplatform.shared.messaging.EventPublisher;
import com.examplatform.shared.messaging.GenericDomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test suite for {@link PaperGenerationConsumer} verifying end-to-end
 * event consumption, template resolution, database persistence, idempotency, and event publishing.
 *
 * Validates: Requirements 8.7 (Async Paper Assembly Dispatch), Issue #114
 */
@DisplayName("PaperGenerationConsumer — End-to-End Integration Test Suite")
class PaperGenerationConsumerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PaperGenerationConsumer paperGenerationConsumer;

    @Autowired
    private PaperRepository paperRepository;

    @Autowired
    private BlueprintTemplateRepository blueprintTemplateRepository;

    @MockitoBean
    private QuestionBankClient questionBankClient;

    @MockitoBean
    private EventPublisher eventPublisher;

    private static final String TENANT_ID = "tenant-e2e";
    private static final UUID EXAM_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final String SHIFT_ID = "SHIFT-E2E-01";

    @Test
    @DisplayName("Should successfully consume event, generate paper into database, and publish completed event")
    void consumesEventAndPersistsPaper() throws Exception {
        if (!testcontainersAvailable) return;

        // Given
        UUID q1 = UUID.randomUUID();
        UUID q2 = UUID.randomUUID();

        when(questionBankClient.findAvailableQuestions(eq("Mathematics"), eq("Algebra"), eq("EASY"), any(), eq(TENANT_ID)))
                .thenReturn(List.of(
                        QuestionSummary.builder().questionId(q1).subject("Mathematics").topic("Algebra").difficulty("EASY").build(),
                        QuestionSummary.builder().questionId(q2).subject("Mathematics").topic("Algebra").difficulty("EASY").build()
                ));

        Map<String, Object> requestPayload = Map.of(
                "eventType", "PAPER_GENERATION_REQUEST",
                "examId", EXAM_ID.toString(),
                "shiftId", SHIFT_ID,
                "tenantId", TENANT_ID,
                "name", "E2E Math Paper",
                "blueprintRules", List.of(
                        Map.of("subject", "Mathematics", "topic", "Algebra", "difficulty", "EASY", "questionCount", 2)
                )
        );
        String json = objectMapper.writeValueAsString(requestPayload);

        // When - simulate event arriving via in-memory / broker listener
        paperGenerationConsumer.onSpringPaperGenerationRequest(
                new GenericDomainEvent(PaperGenerationConsumer.PAPER_EVENTS_TOPIC, EXAM_ID.toString(), json));

        // Then - verify paper saved in database
        List<Paper> savedPapers = paperRepository.findByExamIdAndShiftIdAndTenantId(EXAM_ID, SHIFT_ID, TENANT_ID);
        assertThat(savedPapers).hasSize(1);
        Paper paper = savedPapers.getFirst();
        assertThat(paper.getName()).isEqualTo("E2E Math Paper");
        assertThat(paper.getStatus()).isEqualTo("DRAFT");
        assertThat(paper.getPaperRootHash()).isNotNull();
        assertThat(paper.getManifestDigest()).isNotNull();

        // Verify completion event was published
        verify(eventPublisher, atLeastOnce()).publish(
                eq(PaperGenerationConsumer.PAPER_EVENTS_TOPIC),
                eq(paper.getId().toString()),
                any()
        );
    }

    @Test
    @DisplayName("Should resolve blueprint template and persist paper when blueprintTemplateId is provided")
    void resolvesTemplateAndGeneratesPaper() throws Exception {
        if (!testcontainersAvailable) return;

        UUID templateExamId = UUID.randomUUID();
        String shiftId = "SHIFT-TEMPLATE-01";

        List<BlueprintRule> templateRules = List.of(
                BlueprintRule.builder().subject("Physics").topic("Optics").difficulty("MEDIUM").questionCount(1).build()
        );

        BlueprintTemplate template = BlueprintTemplate.builder()
                .name("Optics Template " + UUID.randomUUID())
                .examId(templateExamId)
                .rulesJson(objectMapper.writeValueAsString(templateRules))
                .createdBy(UUID.randomUUID())
                .build();
        template.setTenantId(TENANT_ID);
        BlueprintTemplate savedTemplate = blueprintTemplateRepository.save(template);

        UUID qId = UUID.randomUUID();
        when(questionBankClient.findAvailableQuestions(eq("Physics"), eq("Optics"), eq("MEDIUM"), any(), eq(TENANT_ID)))
                .thenReturn(List.of(
                        QuestionSummary.builder().questionId(qId).subject("Physics").topic("Optics").difficulty("MEDIUM").build()
                ));

        Map<String, Object> requestPayload = Map.of(
                "eventType", "PAPER_GENERATION_REQUEST",
                "examId", templateExamId.toString(),
                "shiftId", shiftId,
                "tenantId", TENANT_ID,
                "blueprintTemplateId", savedTemplate.getId().toString()
        );
        String json = objectMapper.writeValueAsString(requestPayload);

        paperGenerationConsumer.processPaperGenerationRequest(json, templateExamId.toString());

        List<Paper> papers = paperRepository.findByExamIdAndShiftIdAndTenantId(templateExamId, shiftId, TENANT_ID);
        assertThat(papers).hasSize(1);
        assertThat(papers.getFirst().getPaperDefinitionJson()).contains(qId.toString());
    }

    @Test
    @DisplayName("Idempotency: Repeated generation request for existing paper does not duplicate database rows")
    void idempotencyPreventsDuplicatePaperRows() throws Exception {
        if (!testcontainersAvailable) return;

        UUID examId = UUID.randomUUID();
        String shiftId = "SHIFT-IDEMPOTENT-01";

        UUID qId = UUID.randomUUID();
        when(questionBankClient.findAvailableQuestions(anyString(), anyString(), anyString(), any(), eq(TENANT_ID)))
                .thenReturn(List.of(
                        QuestionSummary.builder().questionId(qId).subject("Chemistry").topic("Acids").difficulty("EASY").build()
                ));

        Map<String, Object> requestPayload = Map.of(
                "eventType", "PAPER_GENERATION_REQUEST",
                "examId", examId.toString(),
                "shiftId", shiftId,
                "tenantId", TENANT_ID,
                "blueprintRules", List.of(
                        Map.of("subject", "Chemistry", "topic", "Acids", "difficulty", "EASY", "questionCount", 1)
                )
        );
        String json = objectMapper.writeValueAsString(requestPayload);

        // 1st request -> generates paper
        paperGenerationConsumer.processPaperGenerationRequest(json, examId.toString());
        assertThat(paperRepository.findByExamIdAndShiftIdAndTenantId(examId, shiftId, TENANT_ID)).hasSize(1);

        // 2nd request -> idempotently skips generation
        paperGenerationConsumer.processPaperGenerationRequest(json, examId.toString());
        assertThat(paperRepository.findByExamIdAndShiftIdAndTenantId(examId, shiftId, TENANT_ID)).hasSize(1);
    }
}
