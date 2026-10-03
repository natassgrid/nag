// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.event;

import com.examplatform.recommendation.domain.LearnerProfile;
import com.examplatform.recommendation.service.LearnerProfileService;
import com.examplatform.recommendation.service.RecommendationAiService;
import com.examplatform.recommendation.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PracticeSessionCompletedConsumer {
    private final LearnerProfileService learnerProfileService;
    private final RecommendationAiService recommendationAiService;
    private final RecommendationRepository recommendationRepository;
    
    @EventListener
    public void onSessionCompleted(PracticeSessionCompletedEvent event) {
        // 1. Update learner profile
        LearnerProfile profile = learnerProfileService.updateProfile(event);
        // 2. Generate recommendation asynchronously (don't block the event thread)
        recommendationAiService.generateAndSave(profile, event.sessionId());
    }
}
