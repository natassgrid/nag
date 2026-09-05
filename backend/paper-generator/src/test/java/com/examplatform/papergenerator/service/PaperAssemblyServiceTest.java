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

package com.examplatform.papergenerator.service;

import com.examplatform.papergenerator.client.QuestionBankClient;
import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.dto.BlueprintRule;
import com.examplatform.papergenerator.dto.PaperGenerationRequest;
import com.examplatform.papergenerator.dto.QuestionSummary;
import com.examplatform.papergenerator.repository.PaperRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PaperAssemblyService.
 * Validates blueprint-driven paper assembly logic including question selection,
 * reuse policy enforcement, difficulty scoring, and paper persistence.
 *
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4
 */
@ExtendWith(MockitoExtension.class)
class PaperAssemblyServiceTest {

    @Mock
    private QuestionBankClient questionBankClient;

    @Mock
    private PaperRepository paperRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ExaminationLookupService examinationLookupService;

    private PaperAssemblyService paperAssemblyService;

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final String SHIFT_ID = "SHIFT-001";
    private static final UUID GENERATED_BY = UUID.randomUUID();
    private static final String TENANT_ID = "tenant-001";

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        paperAssemblyService = new PaperAssemblyService(
                questionBankClient, paperRepository, kafkaTemplate, objectMapper, examinationLookupService);
    }

    @Test
    @DisplayName("Should generate paper with correct question count matching blueprint")
    void generatePaper_withBlueprint_selectsCorrectQuestionCount() {
        // Given
        List<BlueprintRule> rules = List.of(
                BlueprintRule.builder()
                        .subject("Mathematics")
                        .topic("Algebra")
                        .difficulty("EASY")
                        .cognitiveLevel("KNOWLEDGE")
                        .questionCount(2)
                        .build(),
                BlueprintRule.builder()
                        .subject("Mathematics")
                        .topic("Calculus")
                        .difficulty("MEDIUM")
                        .cognitiveLevel("APPLY")
                        .questionCount(1)
                        .build()
        );

        PaperGenerationRequest request = PaperGenerationRequest.builder()
                .name("Sample Math Paper")
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .blueprintRules(rules)
                .build();

        UUID q1 = UUID.randomUUID();
        UUID q2 = UUID.randomUUID();
        UUID q3 = UUID.randomUUID();

        when(questionBankClient.findAvailableQuestions("Mathematics", "Algebra", "EASY", "KNOWLEDGE", TENANT_ID))
                .thenReturn(List.of(
                        createQuestionSummary(q1, "Mathematics", "Algebra", "EASY", "KNOWLEDGE", 0, null, null),
                        createQuestionSummary(q2, "Mathematics", "Algebra", "EASY", "KNOWLEDGE", 0, null, null)
                ));

        when(questionBankClient.findAvailableQuestions("Mathematics", "Calculus", "MEDIUM", "APPLY", TENANT_ID))
                .thenReturn(List.of(
                        createQuestionSummary(q3, "Mathematics", "Calculus", "MEDIUM", "APPLY", 0, null, null)
                ));

        when(paperRepository.save(any(Paper.class))).thenAnswer(invocation -> {
            Paper p = invocation.getArgument(0);
            ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
            return p;
        });

        // When
        Paper result = paperAssemblyService.generatePaper(request, GENERATED_BY, TENANT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Sample Math Paper");
        assertThat(result.getExamId()).isEqualTo(EXAM_ID);
        assertThat(result.getShiftId()).isEqualTo(SHIFT_ID);
        assertThat(result.getStatus()).isEqualTo("DRAFT");
        assertThat(result.getDifficultyScore()).isGreaterThan(0.0);

        ArgumentCaptor<Paper> captor = ArgumentCaptor.forClass(Paper.class);
        verify(paperRepository).save(captor.capture());
        Paper saved = captor.getValue();
        assertThat(saved.getPaperDefinitionJson()).contains(q1.toString());
        assertThat(saved.getPaperDefinitionJson()).contains(q2.toString());
        assertThat(saved.getPaperDefinitionJson()).contains(q3.toString());
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    @DisplayName("Should enforce NEVER reuse policy by excluding previously used questions")
    void isEligibleForReuse_neverPolicy_rejectsUsedQuestions() {
        QuestionSummary usedQuestion = createQuestionSummary(
                UUID.randomUUID(), "Physics", "Mechanics", "HARD", "ANALYZE", 1, Instant.now(), "NEVER");
        QuestionSummary unusedQuestion = createQuestionSummary(
                UUID.randomUUID(), "Physics", "Mechanics", "HARD", "ANALYZE", 0, null, "NEVER");

        assertThat(paperAssemblyService.isEligibleForReuse(usedQuestion)).isFalse();
        assertThat(paperAssemblyService.isEligibleForReuse(unusedQuestion)).isTrue();
    }

    @Test
    @DisplayName("Should enforce 1_YEAR reuse policy by excluding questions used within 365 days")
    void isEligibleForReuse_oneYearPolicy_checksTimeWindow() {
        Instant sixMonthsAgo = Instant.now().minus(180, ChronoUnit.DAYS);
        Instant twoYearsAgo = Instant.now().minus(730, ChronoUnit.DAYS);

        QuestionSummary recentlyUsed = createQuestionSummary(
                UUID.randomUUID(), "Chemistry", "Organic", "MEDIUM", "APPLY", 2, sixMonthsAgo, "1_YEAR");
        QuestionSummary oldUsed = createQuestionSummary(
                UUID.randomUUID(), "Chemistry", "Organic", "MEDIUM", "APPLY", 1, twoYearsAgo, "1_YEAR");
        QuestionSummary neverUsed = createQuestionSummary(
                UUID.randomUUID(), "Chemistry", "Organic", "MEDIUM", "APPLY", 0, null, "1_YEAR");

        assertThat(paperAssemblyService.isEligibleForReuse(recentlyUsed)).isFalse();
        assertThat(paperAssemblyService.isEligibleForReuse(oldUsed)).isTrue();
        assertThat(paperAssemblyService.isEligibleForReuse(neverUsed)).isTrue();
    }

    @Test
    @DisplayName("Should enforce 2_YEARS reuse policy by excluding questions used within 730 days")
    void isEligibleForReuse_twoYearsPolicy_checksTimeWindow() {
        Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);
        Instant threeYearsAgo = Instant.now().minus(1095, ChronoUnit.DAYS);

        QuestionSummary usedOneYearAgo = createQuestionSummary(
                UUID.randomUUID(), "Biology", "Genetics", "HARD", "EVALUATE", 1, oneYearAgo, "2_YEARS");
        QuestionSummary usedThreeYearsAgo = createQuestionSummary(
                UUID.randomUUID(), "Biology", "Genetics", "HARD", "EVALUATE", 1, threeYearsAgo, "2_YEARS");

        assertThat(paperAssemblyService.isEligibleForReuse(usedOneYearAgo)).isFalse();
        assertThat(paperAssemblyService.isEligibleForReuse(usedThreeYearsAgo)).isTrue();
    }

    @Test
    @DisplayName("Should compute difficulty score accurately based on EASY=1.0, MEDIUM=2.0, HARD=3.0")
    void computeDifficultyScore_calculatesAverageCorrectly() {
        List<QuestionSummary> questions = List.of(
                createQuestionSummary(UUID.randomUUID(), "S", "T", "EASY", "K", 0, null, null),
                createQuestionSummary(UUID.randomUUID(), "S", "T", "MEDIUM", "K", 0, null, null),
                createQuestionSummary(UUID.randomUUID(), "S", "T", "HARD", "K", 0, null, null)
        );

        double score = paperAssemblyService.computeDifficultyScore(questions);
        // (1.0 + 2.0 + 3.0) / 3 = 2.0
        assertThat(score).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should publish paper generation event to Kafka")
    void generatePaper_publishesKafkaEvent() {
        List<BlueprintRule> rules = List.of(
                BlueprintRule.builder()
                        .subject("Math")
                        .topic("Algebra")
                        .difficulty("EASY")
                        .cognitiveLevel("KNOWLEDGE")
                        .questionCount(1)
                        .build()
        );

        PaperGenerationRequest request = PaperGenerationRequest.builder()
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .blueprintRules(rules)
                .build();

        UUID qId = UUID.randomUUID();
        when(questionBankClient.findAvailableQuestions(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(List.of(createQuestionSummary(qId, "Math", "Algebra", "EASY", "KNOWLEDGE", 0, null, null)));

        when(paperRepository.save(any(Paper.class))).thenAnswer(inv -> {
            Paper p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
            return p;
        });

        paperAssemblyService.generatePaper(request, GENERATED_BY, TENANT_ID);

        verify(kafkaTemplate, atLeast(1)).send(anyString(), anyString(), any());
    }

    private QuestionSummary createQuestionSummary(
            UUID id, String subject, String topic, String difficulty,
            String cognitiveLevel, int usageCount, Instant lastUsedAt, String reusePolicy) {
        return QuestionSummary.builder()
                .questionId(id)
                .subject(subject)
                .topic(topic)
                .difficulty(difficulty)
                .cognitiveLevel(cognitiveLevel)
                .usageCount(usageCount)
                .lastUsedAt(lastUsedAt)
                .reusePolicy(reusePolicy)
                .build();
    }
}
