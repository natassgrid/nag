// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.service;

import com.examplatform.recommendation.domain.LearnerProfile;
import com.examplatform.recommendation.domain.Recommendation;
import com.examplatform.recommendation.dto.RecommendationResponse;
import com.examplatform.recommendation.repository.LearnerProfileRepository;
import com.examplatform.recommendation.repository.RecommendationRepository;
import com.examplatform.shared.event.PracticeSessionCompletedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService Tests")
class RecommendationServiceTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private RecommendationAiService recommendationAiService;

    @Mock
    private LearnerProfileRepository learnerProfileRepository;

    private ObjectMapper objectMapper;
    private RecommendationService recommendationService;

    private static final UUID CANDIDATE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        recommendationService = new RecommendationService(
                recommendationRepository,
                objectMapper,
                recommendationAiService,
                learnerProfileRepository
        );
    }

    @Test
    @DisplayName("updateStudyPlanDay updates matching day's completed flag and saves JSON")
    void testUpdateStudyPlanDaySuccess() {
        String initialJson = """
                [
                  {"day": 1, "topicName": "Calculus", "estimatedMinutes": 45, "completed": false},
                  {"day": 2, "topicName": "Algebra", "estimatedMinutes": 30, "completed": false}
                ]
                """;

        Recommendation rec = Recommendation.builder()
                .candidateId(CANDIDATE_ID)
                .status("ACTIVE")
                .studyPlanItems(initialJson)
                .generatedAt(Instant.now())
                .build();

        when(recommendationRepository.findTopByCandidateIdOrderByCreatedAtDesc(CANDIDATE_ID))
                .thenReturn(Optional.of(rec));
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));

        RecommendationResponse response = recommendationService.updateStudyPlanDay(CANDIDATE_ID, 1, true);

        assertThat(response).isNotNull();
        assertThat(response.studyPlan()).hasSize(2);
        assertThat(response.studyPlan().get(0).day()).isEqualTo(1);
        assertThat(response.studyPlan().get(0).completed()).isTrue();
        assertThat(response.studyPlan().get(1).day()).isEqualTo(2);
        assertThat(response.studyPlan().get(1).completed()).isFalse();

        ArgumentCaptor<Recommendation> captor = ArgumentCaptor.forClass(Recommendation.class);
        verify(recommendationRepository).save(captor.capture());
        assertThat(captor.getValue().getStudyPlanItems()).contains("\"completed\":true");
    }

    @Test
    @DisplayName("updateStudyPlanDay throws NOT_FOUND when no recommendation exists")
    void testUpdateStudyPlanDayNotFound() {
        when(recommendationRepository.findTopByCandidateIdOrderByCreatedAtDesc(CANDIDATE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> recommendationService.updateStudyPlanDay(CANDIDATE_ID, 1, true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No recommendation found");
    }

    @Test
    @DisplayName("updateStudyPlanDay throws NOT_FOUND when day does not exist in study plan")
    void testUpdateStudyPlanDayInvalidDay() {
        String initialJson = "[{\"day\": 1, \"topicName\": \"Calculus\", \"estimatedMinutes\": 45}]";
        Recommendation rec = Recommendation.builder()
                .candidateId(CANDIDATE_ID)
                .studyPlanItems(initialJson)
                .build();

        when(recommendationRepository.findTopByCandidateIdOrderByCreatedAtDesc(CANDIDATE_ID))
                .thenReturn(Optional.of(rec));

        assertThatThrownBy(() -> recommendationService.updateStudyPlanDay(CANDIDATE_ID, 99, true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Study plan day 99 not found");
    }

    @Test
    @DisplayName("generateRecommendationsAsync looks up profile and invokes recommendationAiService")
    void testGenerateRecommendationsAsync() {
        UUID sessionId = UUID.randomUUID();
        PracticeSessionCompletedEvent event = new PracticeSessionCompletedEvent(
                sessionId, CANDIDATE_ID, UUID.randomUUID(), 5, 2, 0, 5, 7, "{}", "[]"
        );
        LearnerProfile profile = LearnerProfile.builder().candidateId(CANDIDATE_ID).build();
        when(learnerProfileRepository.findByCandidateId(CANDIDATE_ID)).thenReturn(Optional.of(profile));

        recommendationService.generateRecommendationsAsync(event);

        verify(recommendationAiService).generateAndSave(profile, sessionId);
    }
}
