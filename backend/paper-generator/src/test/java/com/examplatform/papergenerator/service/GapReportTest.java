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
import com.examplatform.papergenerator.dto.BlueprintRule;
import com.examplatform.papergenerator.dto.GapDetail;
import com.examplatform.papergenerator.dto.PaperGenerationRequest;
import com.examplatform.papergenerator.dto.QuestionSummary;
import com.examplatform.papergenerator.exception.InsufficientQuestionsException;
import com.examplatform.papergenerator.repository.PaperRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the gap report functionality in PaperAssemblyService.
 * Verifies that InsufficientQuestionsException is thrown with gap details
 * when a blueprint cannot be satisfied.
 *
 * Validates: Requirements 8.5
 */
@ExtendWith(MockitoExtension.class)
class GapReportTest {

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
    @DisplayName("Blueprint with insufficient questions throws InsufficientQuestionsException with gap details")
    void generatePaper_insufficientQuestions_throwsWithGapDetails() {
        // Given: blueprint requires 5 HARD Mathematics/Algebra questions but only 2 are available
        List<BlueprintRule> rules = List.of(
                BlueprintRule.builder()
                        .subject("Mathematics")
                        .topic("Algebra")
                        .difficulty("HARD")
                        .questionCount(5)
                        .build()
        );

        PaperGenerationRequest request = PaperGenerationRequest.builder()
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .blueprintRules(rules)
                .build();

        List<QuestionSummary> availableQuestions = List.of(
                QuestionSummary.builder()
                        .questionId(UUID.randomUUID())
                        .subject("Mathematics").topic("Algebra").difficulty("HARD")
                        .usageCount(0).build(),
                QuestionSummary.builder()
                        .questionId(UUID.randomUUID())
                        .subject("Mathematics").topic("Algebra").difficulty("HARD")
                        .usageCount(0).build()
        );

        when(questionBankClient.findAvailableQuestions("Mathematics", "Algebra", "HARD", null, TENANT_ID))
                .thenReturn(availableQuestions);

        // When/Then
        assertThatThrownBy(() -> paperAssemblyService.generatePaper(request, GENERATED_BY, TENANT_ID))
                .isInstanceOf(InsufficientQuestionsException.class)
                .satisfies(ex -> {
                    InsufficientQuestionsException insuffEx = (InsufficientQuestionsException) ex;
                    List<GapDetail> gaps = insuffEx.getGapDetails();
                    assertThat(gaps).hasSize(1);
                    GapDetail gap = gaps.get(0);
                    assertThat(gap.getSubject()).isEqualTo("Mathematics");
                    assertThat(gap.getTopic()).isEqualTo("Algebra");
                    assertThat(gap.getDifficulty()).isEqualTo("HARD");
                    assertThat(gap.getNeeded()).isEqualTo(5);
                    assertThat(gap.getAvailable()).isEqualTo(2);
                });
    }

    @Test
    @DisplayName("Blueprint with zero available questions throws with gap details showing 0 available")
    void generatePaper_zeroAvailableQuestions_throwsWithGapDetails() {
        // Given: blueprint requires questions but none are available
        List<BlueprintRule> rules = List.of(
                BlueprintRule.builder()
                        .subject("Physics")
                        .topic("Quantum")
                        .difficulty("HARD")
                        .questionCount(3)
                        .build()
        );

        PaperGenerationRequest request = PaperGenerationRequest.builder()
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .blueprintRules(rules)
                .build();

        when(questionBankClient.findAvailableQuestions("Physics", "Quantum", "HARD", null, TENANT_ID))
                .thenReturn(Collections.emptyList());

        // When/Then
        assertThatThrownBy(() -> paperAssemblyService.generatePaper(request, GENERATED_BY, TENANT_ID))
                .isInstanceOf(InsufficientQuestionsException.class)
                .satisfies(ex -> {
                    InsufficientQuestionsException insuffEx = (InsufficientQuestionsException) ex;
                    List<GapDetail> gaps = insuffEx.getGapDetails();
                    assertThat(gaps).hasSize(1);
                    assertThat(gaps.get(0).getNeeded()).isEqualTo(3);
                    assertThat(gaps.get(0).getAvailable()).isEqualTo(0);
                });
    }

    @Test
    @DisplayName("Multiple unsatisfied rules collect ALL gap details")
    void generatePaper_multipleUnsatisfiedRules_collectsAllGaps() {
        // Given: two rules, both with insufficient questions
        List<BlueprintRule> rules = List.of(
                BlueprintRule.builder()
                        .subject("Chemistry")
                        .topic("Organic")
                        .difficulty("MEDIUM")
                        .questionCount(4)
                        .build(),
                BlueprintRule.builder()
                        .subject("Chemistry")
                        .topic("Inorganic")
                        .difficulty("EASY")
                        .questionCount(3)
                        .build()
        );

        PaperGenerationRequest request = PaperGenerationRequest.builder()
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .blueprintRules(rules)
                .build();

        // Only 1 question available for Organic, 0 for Inorganic
        when(questionBankClient.findAvailableQuestions("Chemistry", "Organic", "MEDIUM", null, TENANT_ID))
                .thenReturn(List.of(QuestionSummary.builder()
                        .questionId(UUID.randomUUID())
                        .subject("Chemistry").topic("Organic").difficulty("MEDIUM")
                        .usageCount(0).build()));
        when(questionBankClient.findAvailableQuestions("Chemistry", "Inorganic", "EASY", null, TENANT_ID))
                .thenReturn(Collections.emptyList());

        // When/Then
        assertThatThrownBy(() -> paperAssemblyService.generatePaper(request, GENERATED_BY, TENANT_ID))
                .isInstanceOf(InsufficientQuestionsException.class)
                .satisfies(ex -> {
                    InsufficientQuestionsException insuffEx = (InsufficientQuestionsException) ex;
                    List<GapDetail> gaps = insuffEx.getGapDetails();
                    assertThat(gaps).hasSize(2);

                    // First gap: Organic needs 4, has 1
                    assertThat(gaps.get(0).getTopic()).isEqualTo("Organic");
                    assertThat(gaps.get(0).getNeeded()).isEqualTo(4);
                    assertThat(gaps.get(0).getAvailable()).isEqualTo(1);

                    // Second gap: Inorganic needs 3, has 0
                    assertThat(gaps.get(1).getTopic()).isEqualTo("Inorganic");
                    assertThat(gaps.get(1).getNeeded()).isEqualTo(3);
                    assertThat(gaps.get(1).getAvailable()).isEqualTo(0);
                });
    }
}
