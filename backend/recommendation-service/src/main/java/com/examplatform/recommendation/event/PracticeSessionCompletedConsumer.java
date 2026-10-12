// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.event;

import com.examplatform.recommendation.domain.LearnerProfile;
import com.examplatform.recommendation.service.LearnerProfileService;
import com.examplatform.recommendation.service.LearningPathGeneratorService;
import com.examplatform.recommendation.service.RecommendationAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PracticeSessionCompletedConsumer {
    private final LearnerProfileService learnerProfileService;
    private final RecommendationAiService recommendationAiService;
    private final LearningPathGeneratorService learningPathGeneratorService;

    @EventListener
    public void onSessionCompleted(PracticeSessionCompletedEvent event) {
        // 1. Update learner profile & subtopic performance telemetry
        LearnerProfile profile = learnerProfileService.updateProfile(event);

        // 2. Deterministically generate / update Dynamic Learning Path (fast, rule-based)
        learningPathGeneratorService.generateOrRecalculate(event.candidateId(), event.sessionId(), null);

        // 3. Generate legacy recommendation asynchronously (advisory/chat background task)
        recommendationAiService.generateAndSave(profile, event.sessionId());
    }
}
