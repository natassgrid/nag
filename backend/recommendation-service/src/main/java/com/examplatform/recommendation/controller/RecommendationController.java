// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.controller;

import com.examplatform.recommendation.domain.DynamicLearningPath;
import com.examplatform.recommendation.dto.*;
import com.examplatform.recommendation.service.LearnerProfileService;
import com.examplatform.recommendation.service.LearningPathGeneratorService;
import com.examplatform.recommendation.service.RecommendationService;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService recommendationService;
    private final LearnerProfileService learnerProfileService;
    private final LearningPathGeneratorService learningPathGeneratorService;

    @GetMapping("/latest")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecommendationDto> getLatest() {
        UUID candidateId = UserContext.getRequiredUserId();
        return recommendationService.getLatest(candidateId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/generate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecommendationDto> generateRecommendations(
            @RequestParam(required = false, defaultValue = SharedConstants.JEE_MAIN) String examTarget) {
        UUID candidateId = UserContext.getRequiredUserId();
        return ResponseEntity.ok(recommendationService.generateOrRefresh(candidateId, examTarget));
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<RecommendationDto>> getHistory(
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size) {
        UUID candidateId = UserContext.getRequiredUserId();
        return ResponseEntity.ok(recommendationService.getHistory(candidateId, PageRequest.of(page, size)));
    }

    @PostMapping("/{id}/dismiss")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> dismiss(@PathVariable UUID id) {
        UUID candidateId = UserContext.getRequiredUserId();
        recommendationService.dismiss(id, candidateId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/study-plan/days/{dayNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecommendationResponse> updateStudyPlanDay(
            @PathVariable int dayNumber,
            @RequestParam(defaultValue = "true") boolean completed) {
        UUID candidateId = UserContext.getRequiredUserId();
        RecommendationResponse response = recommendationService.updateStudyPlanDay(candidateId, dayNumber, completed);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/learner-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LearnerProfileDto> getLearnerProfile() {
        UUID candidateId = UserContext.getRequiredUserId();
        return ResponseEntity.ok(learnerProfileService.getProfile(candidateId));
    }

    // Dynamic Learning Path (DLP) endpoints exposed alongside legacy recommendations
    @GetMapping("/learning-path/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DynamicLearningPathResponse> getCurrentLearningPath() {
        UUID candidateId = UserContext.getRequiredUserId();
        return learningPathGeneratorService.getCurrentActivePath(candidateId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    DynamicLearningPath path = learningPathGeneratorService.generateOrRecalculate(candidateId, null, null);
                    return ResponseEntity.ok(learningPathGeneratorService.toResponse(path));
                });
    }

    @PostMapping("/learning-path/recalculate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DynamicLearningPathResponse> recalculateLearningPath(
            @RequestBody(required = false) RecalculateLearningPathRequest request) {
        UUID candidateId = UserContext.getRequiredUserId();
        UUID sourceSessionId = request != null ? request.sourceSessionId() : null;
        String examTarget = request != null ? request.examTarget() : null;
        DynamicLearningPath path = learningPathGeneratorService.generateOrRecalculate(candidateId, sourceSessionId, examTarget);
        return ResponseEntity.ok(learningPathGeneratorService.toResponse(path));
    }

    @GetMapping("/learning-path/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LearningPathSummaryResponse> getLearningPathSummary() {
        UUID candidateId = UserContext.getRequiredUserId();
        return ResponseEntity.ok(learningPathGeneratorService.getSummary(candidateId));
    }
}
