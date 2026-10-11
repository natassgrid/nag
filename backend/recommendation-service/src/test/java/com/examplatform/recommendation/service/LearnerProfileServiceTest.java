// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.service;

import com.examplatform.recommendation.domain.LearnerProfile;
import com.examplatform.recommendation.domain.LearnerSubtopicPerformance;
import com.examplatform.recommendation.dto.LearnerProfileDto;
import com.examplatform.recommendation.event.PracticeSessionCompletedEvent;
import com.examplatform.recommendation.repository.LearnerProfileRepository;
import com.examplatform.recommendation.repository.LearnerSubtopicPerformanceRepository;
import com.examplatform.shared.dto.SubtopicPerformanceDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LearnerProfileServiceTest {

    @Mock
    private LearnerProfileRepository profileRepository;

    @Mock
    private LearnerSubtopicPerformanceRepository subtopicRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private LearnerProfileService learnerProfileService;

    private UUID candidateId;

    @BeforeEach
    void setUp() {
        candidateId = UUID.randomUUID();
    }

    @Test
    @DisplayName("updateProfile - persists subtopic performance and updates rolling metrics")
    void testUpdateProfileWithSubtopics() throws Exception {
        LearnerProfile existingProfile = LearnerProfile.builder()
                .candidateId(candidateId)
                .totalPracticeSessions(1)
                .totalQuestionsAttempted(5)
                .overallAccuracy(0.6)
                .build();
        existingProfile.setTenantId("test-tenant");

        when(profileRepository.findByCandidateId(candidateId)).thenReturn(Optional.of(existingProfile));
        when(profileRepository.save(any(LearnerProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtopicPerformanceDto subtopicDto = new SubtopicPerformanceDto(
                100L, "Physics",
                10L, "Rotational Mechanics",
                501L, "Moment of Inertia",
                4, 3, 1, 2, 1,
                180000L, 60000L, 60000L,
                33.33, 25.0, 1.0
        );

        String subtopicJson = objectMapper.writeValueAsString(List.of(subtopicDto));
        String topicJson = "{\"Rotational Mechanics\":{\"correct\":1,\"total\":3}}";

        PracticeSessionCompletedEvent event = new PracticeSessionCompletedEvent(
                UUID.randomUUID(), candidateId, UUID.randomUUID(),
                1, 2, 1, 2, 8, topicJson, subtopicJson
        );

        when(subtopicRepository.findByCandidateIdAndSubtopicName(candidateId, "Moment of Inertia"))
                .thenReturn(Optional.empty());

        LearnerProfile updated = learnerProfileService.updateProfile(event);

        assertThat(updated.getTotalPracticeSessions()).isEqualTo(2);
        assertThat(updated.getTotalQuestionsAttempted()).isEqualTo(8);
        assertThat(updated.getSubtopicAccuracyMap()).contains("Moment of Inertia");

        ArgumentCaptor<LearnerSubtopicPerformance> perfCaptor =
                ArgumentCaptor.forClass(LearnerSubtopicPerformance.class);
        verify(subtopicRepository).save(perfCaptor.capture());

        LearnerSubtopicPerformance savedPerf = perfCaptor.getValue();
        assertThat(savedPerf.getCandidateId()).isEqualTo(candidateId);
        assertThat(savedPerf.getSubtopicName()).isEqualTo("Moment of Inertia");
        assertThat(savedPerf.getTotalQuestions()).isEqualTo(4);
        assertThat(savedPerf.getTotalAttempts()).isEqualTo(3);
        assertThat(savedPerf.getTotalCorrect()).isEqualTo(1);
        assertThat(savedPerf.getTotalSkipped()).isEqualTo(1);
        assertThat(savedPerf.getAvgAccuracyPercent()).isEqualTo(33.33);
        assertThat(savedPerf.getAvgSkipRatePercent()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("getProfile - returns empty default profile when candidate does not exist")
    void testGetProfileEmpty() {
        when(profileRepository.findByCandidateId(candidateId)).thenReturn(Optional.empty());

        LearnerProfileDto dto = learnerProfileService.getProfile(candidateId);

        assertThat(dto.candidateId()).isEqualTo(candidateId);
        assertThat(dto.totalPracticeSessions()).isEqualTo(0);
        assertThat(dto.overallAccuracy()).isEqualTo(0.0);
        assertThat(dto.subtopicAccuracyMap()).isEqualTo("{}");
    }
}
