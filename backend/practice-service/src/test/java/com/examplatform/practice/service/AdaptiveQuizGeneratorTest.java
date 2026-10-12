// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.service;

import com.examplatform.practice.client.QuestionBankClient;
import com.examplatform.practice.domain.PracticeSet;
import com.examplatform.practice.dto.PracticeSetDto;
import com.examplatform.practice.repository.PracticeSetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdaptiveQuizGenerator 70/30 Pedagogical Ratio Unit Tests")
class AdaptiveQuizGeneratorTest {

    @Mock
    private QuestionBankClient questionBankClient;

    @Mock
    private PracticeSetRepository practiceSetRepository;

    @Mock
    private PracticeSetService practiceSetService;

    private ObjectMapper objectMapper;
    private AdaptiveQuizGenerator generator;

    private static final UUID CANDIDATE_ID = UUID.fromString("12345678-1234-1234-1234-1234567890ab");

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        generator = new AdaptiveQuizGenerator(questionBankClient, practiceSetRepository, practiceSetService, objectMapper);
    }

    @Test
    @DisplayName("Generates adaptive quiz with 70% Easy/Medium and 30% Hard questions")
    void testAdaptiveRatioCompliance() throws Exception {
        List<String> subtopics = List.of("Moment of Inertia", "Friction");

        // Request 20 questions: 14 Easy/Medium (70%) and 6 Hard (30%)
        List<UUID> easyIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        List<UUID> medIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        List<UUID> hardIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        when(questionBankClient.findQuestionsForAdaptiveQuiz(eq(subtopics), eq("EASY"), eq(14))).thenReturn(easyIds);
        when(questionBankClient.findQuestionsForAdaptiveQuiz(eq(subtopics), eq("MEDIUM"), eq(14))).thenReturn(medIds);
        when(questionBankClient.findQuestionsForAdaptiveQuiz(eq(subtopics), eq("HARD"), eq(6))).thenReturn(hardIds);

        when(practiceSetRepository.save(any(PracticeSet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(practiceSetService.toDto(any())).thenReturn(null);

        generator.generateAdaptiveSet(CANDIDATE_ID, subtopics, "JEE_MAIN", 20, false);

        ArgumentCaptor<PracticeSet> captor = ArgumentCaptor.forClass(PracticeSet.class);
        verify(practiceSetRepository).save(captor.capture());

        PracticeSet saved = captor.getValue();
        assertThat(saved.getSource()).isEqualTo("ADAPTIVE_QUIZ");
        assertThat(saved.isPublished()).isTrue();
        assertThat(saved.getTotalQuestions()).isEqualTo(20);

        List<String> savedIds = objectMapper.readValue(saved.getQuestionIds(), List.class);
        assertThat(savedIds).hasSize(20);
    }

    @Test
    @DisplayName("Generates Gatekeeper test with GATEKEEPER_TEST source and 45-minute duration")
    void testGatekeeperSetGeneration() {
        List<String> subtopics = List.of("Moment of Inertia");
        when(questionBankClient.findQuestionsForAdaptiveQuiz(any(), any(), any(Integer.class))).thenReturn(List.of(UUID.randomUUID()));
        when(practiceSetRepository.save(any(PracticeSet.class))).thenAnswer(inv -> inv.getArgument(0));

        generator.generateAdaptiveSet(CANDIDATE_ID, subtopics, "JEE_MAIN", 10, true);

        ArgumentCaptor<PracticeSet> captor = ArgumentCaptor.forClass(PracticeSet.class);
        verify(practiceSetRepository).save(captor.capture());

        PracticeSet saved = captor.getValue();
        assertThat(saved.getSource()).isEqualTo("GATEKEEPER_TEST");
        assertThat(saved.getDurationMinutes()).isEqualTo(45);
        assertThat(saved.getName()).contains("Gatekeeper Test");
    }
}
