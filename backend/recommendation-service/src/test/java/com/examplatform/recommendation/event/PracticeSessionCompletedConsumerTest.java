// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.event;

import com.examplatform.recommendation.domain.LearnerProfile;
import com.examplatform.recommendation.service.LearnerProfileService;
import com.examplatform.recommendation.service.LearningPathGeneratorService;
import com.examplatform.recommendation.service.MilestoneProgressionService;
import com.examplatform.recommendation.service.RecommendationService;
import com.examplatform.shared.event.PracticeSessionCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PracticeSessionCompletedConsumer Event Routing Tests")
class PracticeSessionCompletedConsumerTest {

    @Mock
    private LearnerProfileService learnerProfileService;

    @Mock
    private MilestoneProgressionService milestoneProgressionService;

    @Mock
    private LearningPathGeneratorService learningPathGeneratorService;

    @Mock
    private RecommendationService recommendationService;

    private PracticeSessionCompletedConsumer consumer;

    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        consumer = new PracticeSessionCompletedConsumer(
                learnerProfileService,
                milestoneProgressionService,
                learningPathGeneratorService,
                recommendationService
        );
    }

    @Test
    @DisplayName("When session is handled by milestone engine, generatorService is NOT invoked")
    void testSessionHandledByMilestoneEngine() {
        PracticeSessionCompletedEvent event = new PracticeSessionCompletedEvent(
                SESSION_ID, CANDIDATE_ID, UUID.randomUUID(), 8, 2, 0, 8, 10, "{}", "[]"
        );
        LearnerProfile profile = LearnerProfile.builder().candidateId(CANDIDATE_ID).build();
        when(learnerProfileService.updateProfile(event)).thenReturn(profile);
        when(milestoneProgressionService.handleCompletedSessionIfApplicable(eq(CANDIDATE_ID), eq(SESSION_ID), eq(80.0)))
                .thenReturn(true);

        consumer.onSessionCompleted(event);

        verify(learnerProfileService).updateProfile(event);
        verify(milestoneProgressionService).handleCompletedSessionIfApplicable(CANDIDATE_ID, SESSION_ID, 80.0);
        verify(learningPathGeneratorService, never()).generateOrRecalculate(any(), any(), any());
        verify(recommendationService).generateRecommendationsAsync(event);
    }

    @Test
    @DisplayName("When session is not handled by milestone engine, generatorService.generateOrRecalculate IS invoked")
    void testSessionNotHandledByMilestoneEngine() {
        PracticeSessionCompletedEvent event = new PracticeSessionCompletedEvent(
                SESSION_ID, CANDIDATE_ID, UUID.randomUUID(), 6, 4, 0, 6, 10, "{}", "[]"
        );
        LearnerProfile profile = LearnerProfile.builder().candidateId(CANDIDATE_ID).build();
        when(learnerProfileService.updateProfile(event)).thenReturn(profile);
        when(milestoneProgressionService.handleCompletedSessionIfApplicable(eq(CANDIDATE_ID), eq(SESSION_ID), eq(60.0)))
                .thenReturn(false);

        consumer.onSessionCompleted(event);

        verify(learnerProfileService).updateProfile(event);
        verify(milestoneProgressionService).handleCompletedSessionIfApplicable(CANDIDATE_ID, SESSION_ID, 60.0);
        verify(learningPathGeneratorService).generateOrRecalculate(CANDIDATE_ID, SESSION_ID, null);
        verify(recommendationService).generateRecommendationsAsync(event);
    }
}
