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
                questionBankClient, paperRepository, kafkaTemplate, objectMapper);
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
                        .questionCount(3)
                        .build(),
                BlueprintRule.builder()
                        .subject("Mathematics")
                        .topic("Calculus")
                        .difficulty("MEDIUM")
                        .questionCount(2)
                        .build()
        );

        PaperGenerationRequest request = PaperGenerationRequest.builder()
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .blueprintRules(rules)
                .build();

        List<QuestionSummary> algebraQuestions = List.of(
                buildQuestion("Algebra", "EASY", 0, null, null),
                buildQuestion("Algebra", "EASY", 0, null, null),
                buildQuestion("Algebra", "EASY", 0, null, null),
                buildQuestion("Algebra", "EASY", 0, null, null)
        );

        List<QuestionSummary> calculusQuestions = List.of(
                buildQuestion("Calculus", "MEDIUM", 0, null, null),
                buildQuestion("Calculus", "MEDIUM", 0, null, null),
                buildQuestion("Calculus", "MEDIUM", 0, null, null)
        );

        when(questionBankClient.findAvailableQuestions("Mathematics", "Algebra", "EASY", null, TENANT_ID))
                .thenReturn(algebraQuestions);
        when(questionBankClient.findAvailableQuestions("Mathematics", "Calculus", "MEDIUM", null, TENANT_ID))
                .thenReturn(calculusQuestions);
        when(paperRepository.save(any(Paper.class))).thenAnswer(invocation -> {
            Paper paper = invocation.getArgument(0);
            setEntityId(paper, UUID.randomUUID());
            return paper;
        });

        // When
        Paper result = paperAssemblyService.generatePaper(request, GENERATED_BY, TENANT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPaperDefinitionJson()).isNotNull();
        // Paper definition should contain exactly 5 question IDs (3 + 2)
        assertThat(result.getPaperDefinitionJson()).contains("[");
        // Count UUID patterns in the JSON
        String json = result.getPaperDefinitionJson();
        long questionCount = json.chars().filter(ch -> ch == ',').count() + 1;
        assertThat(questionCount).isEqualTo(5);
    }

    @Test
    @DisplayName("Should compute difficulty score correctly as average of difficulty weights")
    void generatePaper_computesDifficultyScore_correctly() {
        // Given: 2 EASY (weight=1) + 1 HARD (weight=3) → average = (1+1+3)/3 = 1.667
        List<BlueprintRule> rules = List.of(
                BlueprintRule.builder()
                        .subject("Physics")
                        .topic("Mechanics")
                        .difficulty("EASY")
                        .questionCount(2)
                        .build(),
                BlueprintRule.builder()
                        .subject("Physics")
                        .topic("Thermodynamics")
                        .difficulty("HARD")
                        .questionCount(1)
                        .build()
        );

        PaperGenerationRequest request = PaperGenerationRequest.builder()
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .blueprintRules(rules)
                .build();

        List<QuestionSummary> easyQuestions = List.of(
                buildQuestion("Mechanics", "EASY", 0, null, null),
                buildQuestion("Mechanics", "EASY", 0, null, null)
        );

        List<QuestionSummary> hardQuestions = List.of(
                buildQuestion("Thermodynamics", "HARD", 0, null, null)
        );

        when(questionBankClient.findAvailableQuestions("Physics", "Mechanics", "EASY", null, TENANT_ID))
                .thenReturn(easyQuestions);
        when(questionBankClient.findAvailableQuestions("Physics", "Thermodynamics", "HARD", null, TENANT_ID))
                .thenReturn(hardQuestions);
        when(paperRepository.save(any(Paper.class))).thenAnswer(invocation -> {
            Paper paper = invocation.getArgument(0);
            setEntityId(paper, UUID.randomUUID());
            return paper;
        });

        // When
        Paper result = paperAssemblyService.generatePaper(request, GENERATED_BY, TENANT_ID);

        // Then: (1+1+3)/3 = 1.667
        assertThat(result.getDifficultyScore()).isCloseTo(5.0 / 3.0, org.assertj.core.api.Assertions.within(0.001));
    }

    @Test
    @DisplayName("Should enforce NEVER reuse policy by excluding used questions")
    void generatePaper_enforcesNeverReusePolicy_excludesUsedQuestions() {
        // Given: 4 questions in bank, 2 have NEVER policy with usageCount > 0
        List<BlueprintRule> rules = List.of(
                BlueprintRule.builder()
                        .subject("Chemistry")
                        .topic("Organic")
                        .difficulty("MEDIUM")
                        .questionCount(2)
                        .build()
        );

        PaperGenerationRequest request = PaperGenerationRequest.builder()
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .blueprintRules(rules)
                .build();

        UUID usedQuestionId1 = UUID.randomUUID();
        UUID usedQuestionId2 = UUID.randomUUID();
        UUID freshQuestionId1 = UUID.randomUUID();
        UUID freshQuestionId2 = UUID.randomUUID();

        List<QuestionSummary> questions = List.of(
                QuestionSummary.builder()
                        .questionId(usedQuestionId1)
                        .subject("Chemistry").topic("Organic").difficulty("MEDIUM")
                        .usageCount(3).reusePolicy("NEVER").build(),
                QuestionSummary.builder()
                        .questionId(usedQuestionId2)
                        .subject("Chemistry").topic("Organic").difficulty("MEDIUM")
                        .usageCount(1).reusePolicy("NEVER").build(),
                QuestionSummary.builder()
                        .questionId(freshQuestionId1)
                        .subject("Chemistry").topic("Organic").difficulty("MEDIUM")
                        .usageCount(0).reusePolicy("NEVER").build(),
                QuestionSummary.builder()
                        .questionId(freshQuestionId2)
                        .subject("Chemistry").topic("Organic").difficulty("MEDIUM")
                        .usageCount(0).reusePolicy("NEVER").build()
        );

        when(questionBankClient.findAvailableQuestions("Chemistry", "Organic", "MEDIUM", null, TENANT_ID))
                .thenReturn(questions);
        when(paperRepository.save(any(Paper.class))).thenAnswer(invocation -> {
            Paper paper = invocation.getArgument(0);
            setEntityId(paper, UUID.randomUUID());
            return paper;
        });

        // When
        Paper result = paperAssemblyService.generatePaper(request, GENERATED_BY, TENANT_ID);

        // Then: only fresh questions (usageCount=0) should be selected
        assertThat(result.getPaperDefinitionJson()).contains(freshQuestionId1.toString());
        assertThat(result.getPaperDefinitionJson()).contains(freshQuestionId2.toString());
        assertThat(result.getPaperDefinitionJson()).doesNotContain(usedQuestionId1.toString());
        assertThat(result.getPaperDefinitionJson()).doesNotContain(usedQuestionId2.toString());
    }

    @Test
    @DisplayName("Should store paper in DRAFT status")
    void generatePaper_storesPaperInDraftStatus() {
        // Given
        List<BlueprintRule> rules = List.of(
                BlueprintRule.builder()
                        .subject("Biology")
                        .topic("Genetics")
                        .difficulty("MEDIUM")
                        .questionCount(1)
                        .build()
        );

        PaperGenerationRequest request = PaperGenerationRequest.builder()
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .blueprintRules(rules)
                .build();

        List<QuestionSummary> questions = List.of(
                buildQuestion("Genetics", "MEDIUM", 0, null, null)
        );

        when(questionBankClient.findAvailableQuestions("Biology", "Genetics", "MEDIUM", null, TENANT_ID))
                .thenReturn(questions);

        ArgumentCaptor<Paper> paperCaptor = ArgumentCaptor.forClass(Paper.class);
        when(paperRepository.save(paperCaptor.capture())).thenAnswer(invocation -> {
            Paper paper = invocation.getArgument(0);
            setEntityId(paper, UUID.randomUUID());
            return paper;
        });

        // When
        Paper result = paperAssemblyService.generatePaper(request, GENERATED_BY, TENANT_ID);

        // Then
        Paper savedPaper = paperCaptor.getValue();
        assertThat(savedPaper.getStatus()).isEqualTo("DRAFT");
        assertThat(savedPaper.getExamId()).isEqualTo(EXAM_ID);
        assertThat(savedPaper.getShiftId()).isEqualTo(SHIFT_ID);
        assertThat(savedPaper.getGeneratedBy()).isEqualTo(GENERATED_BY);
        assertThat(savedPaper.getTenantId()).isEqualTo(TENANT_ID);

        // Verify Kafka events were published (paper event + audit event)
        verify(kafkaTemplate, atLeast(1)).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should compute difficulty score correctly using internal method")
    void computeDifficultyScore_withMixedDifficulties_returnsCorrectAverage() {
        // Given: EASY(1) + MEDIUM(2) + HARD(3) → average = 2.0
        List<QuestionSummary> questions = List.of(
                buildQuestion("Topic1", "EASY", 0, null, null),
                buildQuestion("Topic2", "MEDIUM", 0, null, null),
                buildQuestion("Topic3", "HARD", 0, null, null)
        );

        // When
        double score = paperAssemblyService.computeDifficultyScore(questions);

        // Then
        assertThat(score).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should return true for eligible question with NEVER policy and zero usage")
    void isEligibleForReuse_neverPolicyZeroUsage_returnsTrue() {
        QuestionSummary question = QuestionSummary.builder()
                .questionId(UUID.randomUUID())
                .usageCount(0)
                .reusePolicy("NEVER")
                .build();

        assertThat(paperAssemblyService.isEligibleForReuse(question)).isTrue();
    }

    @Test
    @DisplayName("Should return false for question with NEVER policy and non-zero usage")
    void isEligibleForReuse_neverPolicyWithUsage_returnsFalse() {
        QuestionSummary question = QuestionSummary.builder()
                .questionId(UUID.randomUUID())
                .usageCount(2)
                .reusePolicy("NEVER")
                .build();

        assertThat(paperAssemblyService.isEligibleForReuse(question)).isFalse();
    }

    @Test
    @DisplayName("Should return true for 1_YEAR policy when lastUsedAt is beyond window")
    void isEligibleForReuse_oneYearPolicyBeyondWindow_returnsTrue() {
        QuestionSummary question = QuestionSummary.builder()
                .questionId(UUID.randomUUID())
                .usageCount(1)
                .reusePolicy("1_YEAR")
                .lastUsedAt(Instant.now().minus(400, ChronoUnit.DAYS))
                .build();

        assertThat(paperAssemblyService.isEligibleForReuse(question)).isTrue();
    }

    @Test
    @DisplayName("Should return false for 1_YEAR policy when lastUsedAt is within window")
    void isEligibleForReuse_oneYearPolicyWithinWindow_returnsFalse() {
        QuestionSummary question = QuestionSummary.builder()
                .questionId(UUID.randomUUID())
                .usageCount(1)
                .reusePolicy("1_YEAR")
                .lastUsedAt(Instant.now().minus(100, ChronoUnit.DAYS))
                .build();

        assertThat(paperAssemblyService.isEligibleForReuse(question)).isFalse();
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    private QuestionSummary buildQuestion(String topic, String difficulty, int usageCount,
                                          Instant lastUsedAt, String reusePolicy) {
        return QuestionSummary.builder()
                .questionId(UUID.randomUUID())
                .subject("TestSubject")
                .topic(topic)
                .difficulty(difficulty)
                .usageCount(usageCount)
                .lastUsedAt(lastUsedAt)
                .reusePolicy(reusePolicy)
                .build();
    }

    private void setEntityId(Paper paper, UUID id) {
        try {
            var idField = paper.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(paper, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set entity ID via reflection", e);
        }
    }
}
