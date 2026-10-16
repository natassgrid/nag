// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.service;

import com.examplatform.recommendation.domain.LearnerProfile;
import com.examplatform.recommendation.domain.LearnerSubtopicPerformance;
import com.examplatform.recommendation.domain.Recommendation;
import com.examplatform.recommendation.dto.RecommendationResponse;
import com.examplatform.recommendation.dto.WeakTopicItemDto;
import com.examplatform.recommendation.repository.LearnerSubtopicPerformanceRepository;
import com.examplatform.recommendation.repository.RecommendationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationAiService Tests")
class RecommendationAiServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private LearnerSubtopicPerformanceRepository subtopicPerformanceRepository;

    private ObjectMapper objectMapper;
    private RecommendationAiService recommendationAiService;

    private static final UUID CANDIDATE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        recommendationAiService = new RecommendationAiService(
                chatClient,
                recommendationRepository,
                objectMapper,
                subtopicPerformanceRepository
        );
    }

    @Test
    @DisplayName("sanitizeJson extracts JSON between first { and last } even when wrapped in markdown code blocks")
    void testSanitizeJson() {
        String wrapped = "```json\n{\"weakTopicRecommendations\": [], \"studyPlanItems\": [], \"motivationalMessage\": \"Hello\"}\n```";
        String sanitized = recommendationAiService.sanitizeJson(wrapped);
        assertThat(sanitized).startsWith("{").endsWith("}").contains("\"motivationalMessage\": \"Hello\"");

        String preamble = "Sure! Here is the JSON:\n{\"status\": \"ok\"}\nHope this helps!";
        String sanitizedPreamble = recommendationAiService.sanitizeJson(preamble);
        assertThat(sanitizedPreamble).isEqualTo("{\"status\": \"ok\"}");
    }

    @Test
    @DisplayName("buildDeterministicRecommendation groups subtopics with < 75% accuracy and creates 7-day study plan")
    void testBuildDeterministicRecommendationWithSubtopics() throws Exception {
        LearnerProfile profile = LearnerProfile.builder()
                .candidateId(CANDIDATE_ID)
                .totalPracticeSessions(3)
                .overallAccuracy(0.55)
                .build();

        LearnerSubtopicPerformance perf1 = LearnerSubtopicPerformance.builder()
                .candidateId(CANDIDATE_ID)
                .topicName("Rotational Motion")
                .subtopicName("Moment of Inertia")
                .subjectName("Physics")
                .avgAccuracyPercent(45.0)
                .build();

        LearnerSubtopicPerformance perf2 = LearnerSubtopicPerformance.builder()
                .candidateId(CANDIDATE_ID)
                .topicName("Rotational Motion")
                .subtopicName("Angular Momentum")
                .subjectName("Physics")
                .avgAccuracyPercent(55.0)
                .build();

        when(subtopicPerformanceRepository.findByCandidateId(CANDIDATE_ID)).thenReturn(List.of(perf1, perf2));
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));

        Recommendation rec = recommendationAiService.buildDeterministicRecommendation(profile, UUID.randomUUID(), "JEE_MAIN");

        assertThat(rec.getStatus()).isEqualTo("GENERATED");
        assertThat(rec.getModelUsed()).isEqualTo("deterministic-coach-v1");

        List<WeakTopicItemDto> weak = objectMapper.readValue(rec.getWeakTopicRecommendations(), new TypeReference<>() {});
        assertThat(weak).hasSize(1);
        assertThat(weak.get(0).topicName()).isEqualTo("Rotational Motion");
        assertThat(weak.get(0).currentAccuracy()).isEqualTo(0.50);
        assertThat(weak.get(0).subtopicsToRevise()).contains("Moment of Inertia", "Angular Momentum");

        List<RecommendationResponse.StudyPlanDay> plan = objectMapper.readValue(rec.getStudyPlanItems(), new TypeReference<>() {});
        assertThat(plan).hasSize(7);
        assertThat(plan.get(0).day()).isEqualTo(1);
        assertThat(plan.get(0).focusTopic()).isEqualTo("Rotational Motion");
        assertThat(plan.get(5).day()).isEqualTo(6);
        assertThat(plan.get(5).focusTopic()).contains("Adaptive Application");
        assertThat(plan.get(6).day()).isEqualTo(7);
        assertThat(plan.get(6).focusTopic()).contains("Gatekeeper Verification");
    }

    @Test
    @DisplayName("buildDeterministicRecommendation synthesizes foundation topics for cold start (0 sessions)")
    void testBuildDeterministicRecommendationColdStart() throws Exception {
        LearnerProfile profile = LearnerProfile.builder()
                .candidateId(CANDIDATE_ID)
                .totalPracticeSessions(0)
                .overallAccuracy(0.0)
                .build();

        when(subtopicPerformanceRepository.findByCandidateId(CANDIDATE_ID)).thenReturn(List.of());
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));

        Recommendation rec = recommendationAiService.buildDeterministicRecommendation(profile, null, "NEET");

        assertThat(rec.getStatus()).isEqualTo("GENERATED");
        assertThat(rec.getMotivationalMessage()).contains("Welcome to your AI Study Coach! We have prepared a 7-Day Foundation Study Plan for NEET");

        List<WeakTopicItemDto> weak = objectMapper.readValue(rec.getWeakTopicRecommendations(), new TypeReference<>() {});
        assertThat(weak).extracting(WeakTopicItemDto::topicName).contains("Biology", "Organic Chemistry", "Physics");
    }

    @Test
    @DisplayName("generateOnDemand falls back to deterministic synthesis when ChatClient throws")
    void testGenerateOnDemandFallbackOnChatClientException() throws Exception {
        LearnerProfile profile = LearnerProfile.builder()
                .candidateId(CANDIDATE_ID)
                .totalPracticeSessions(2)
                .overallAccuracy(0.82)
                .build();

        when(chatClient.prompt()).thenThrow(new RuntimeException("OpenAI connection timed out"));
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));

        Recommendation rec = recommendationAiService.generateOnDemand(profile, "JEE_MAIN");

        assertThat(rec.getStatus()).isEqualTo("GENERATED");
        assertThat(rec.getModelUsed()).isEqualTo("deterministic-coach-v1");
        assertThat(rec.getMotivationalMessage()).contains("Outstanding performance!");
    }
}
