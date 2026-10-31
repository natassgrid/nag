// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.event;

import com.examplatform.recommendation.domain.LearnerProfile;
import com.examplatform.recommendation.dto.CompleteActivityRequest;
import com.examplatform.recommendation.service.LearnerProfileService;
import com.examplatform.recommendation.service.LearningPathGeneratorService;
import com.examplatform.recommendation.service.MilestoneProgressionService;
import com.examplatform.recommendation.service.NextBestActivityEngine;
import com.examplatform.recommendation.service.RecommendationService;
import com.examplatform.shared.dto.SubtopicPerformanceDto;
import com.examplatform.shared.event.PracticeSessionCompletedEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PracticeSessionCompletedConsumer {
    private final LearnerProfileService learnerProfileService;
    private final MilestoneProgressionService milestoneProgressionService;
    private final LearningPathGeneratorService learningPathGeneratorService;
    private final RecommendationService recommendationService;
    private final NextBestActivityEngine nextBestActivityEngine;
    private final ObjectMapper objectMapper;

    public PracticeSessionCompletedConsumer(
            LearnerProfileService learnerProfileService,
            MilestoneProgressionService milestoneProgressionService,
            LearningPathGeneratorService learningPathGeneratorService,
            RecommendationService recommendationService) {
        this(learnerProfileService, milestoneProgressionService, learningPathGeneratorService, recommendationService, null, null);
    }

    @Autowired
    public PracticeSessionCompletedConsumer(
            LearnerProfileService learnerProfileService,
            MilestoneProgressionService milestoneProgressionService,
            LearningPathGeneratorService learningPathGeneratorService,
            RecommendationService recommendationService,
            @Autowired(required = false) NextBestActivityEngine nextBestActivityEngine,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this.learnerProfileService = learnerProfileService;
        this.milestoneProgressionService = milestoneProgressionService;
        this.learningPathGeneratorService = learningPathGeneratorService;
        this.recommendationService = recommendationService;
        this.nextBestActivityEngine = nextBestActivityEngine;
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
    }

    @EventListener
    public void onSessionCompleted(PracticeSessionCompletedEvent event) {
        // 1. Update learner profile & subtopic performance telemetry
        learnerProfileService.updateProfile(event);

        // 2. Evaluate if completed session was an active Phase 2 Quiz or Phase 3 Gatekeeper test
        int totalAttempted = event.correctCount() + event.incorrectCount();
        double overallAccuracy = totalAttempted > 0 ? (event.correctCount() * 100.0) / totalAttempted : 0.0;
        boolean handledByMilestoneEngine = milestoneProgressionService.handleCompletedSessionIfApplicable(
                event.candidateId(), event.sessionId(), overallAccuracy);

        // 3. Deterministically generate / recalculate Dynamic Learning Path if not handled by milestone engine
        if (!handledByMilestoneEngine) {
            learningPathGeneratorService.generateOrRecalculate(event.candidateId(), event.sessionId(), null);
        }

        // 4. If enrollmentId is present, update canonical learner_subtopic_state
        if (event.enrollmentId() != null && nextBestActivityEngine != null
                && event.subtopicWiseBreakdown() != null && !event.subtopicWiseBreakdown().isBlank()) {
            try {
                List<SubtopicPerformanceDto> list = objectMapper.readValue(
                        event.subtopicWiseBreakdown(), new TypeReference<>() {});
                for (SubtopicPerformanceDto dto : list) {
                    if (dto.subtopicId() != null) {
                        nextBestActivityEngine.completeActivity(
                                event.enrollmentId(),
                                event.sessionId(),
                                new CompleteActivityRequest(
                                        "PRACTICE",
                                        dto.subtopicId(),
                                        null,
                                        null,
                                        null,
                                        dto.attemptedCount(),
                                        dto.correctCount(),
                                        dto.totalTimeSpentMs()
                                )
                        );
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to propagate subtopic performance to learner_subtopic_state: {}", e.getMessage());
            }
        }

        // 5. Generate recommendations asynchronously
        recommendationService.generateRecommendationsAsync(event);
    }
}
