// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.event;

import com.examplatform.recommendation.domain.LearnerProfile;
import com.examplatform.recommendation.service.LearnerProfileService;
import com.examplatform.recommendation.service.LearningPathGeneratorService;
import com.examplatform.recommendation.service.MilestoneProgressionService;
import com.examplatform.recommendation.service.RecommendationService;
import com.examplatform.shared.event.PracticeSessionCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PracticeSessionCompletedConsumer {
    private final LearnerProfileService learnerProfileService;
    private final MilestoneProgressionService milestoneProgressionService;
    private final LearningPathGeneratorService learningPathGeneratorService;
    private final RecommendationService recommendationService;

    @EventListener
    public void onSessionCompleted(PracticeSessionCompletedEvent event) {
        // 1. Update learner profile & subtopic performance telemetry
        LearnerProfile profile = learnerProfileService.updateProfile(event);

        // 2. Evaluate if completed session was an active Phase 2 Quiz or Phase 3 Gatekeeper test
        int totalAttempted = event.correctCount() + event.incorrectCount();
        double overallAccuracy = totalAttempted > 0 ? (event.correctCount() * 100.0) / totalAttempted : 0.0;
        boolean handledByMilestoneEngine = milestoneProgressionService.handleCompletedSessionIfApplicable(
                event.candidateId(), event.sessionId(), overallAccuracy);

        // 3. Deterministically generate / recalculate Dynamic Learning Path if not handled by milestone engine
        if (!handledByMilestoneEngine) {
            learningPathGeneratorService.generateOrRecalculate(event.candidateId(), event.sessionId(), null);
        }

        // 4. Generate recommendations asynchronously
        recommendationService.generateRecommendationsAsync(event);
    }
}
