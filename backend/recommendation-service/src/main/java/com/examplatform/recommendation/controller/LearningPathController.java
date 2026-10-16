// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.controller;

import com.examplatform.recommendation.domain.DynamicLearningPath;
import com.examplatform.recommendation.dto.*;
import com.examplatform.recommendation.service.LearningPathGeneratorService;
import com.examplatform.recommendation.service.MilestoneProgressionService;
import com.examplatform.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/learning-path")
@RequiredArgsConstructor
public class LearningPathController {

    private final LearningPathGeneratorService learningPathGeneratorService;
    private final MilestoneProgressionService milestoneProgressionService;
    private final RecommendationService recommendationService;

    /**
     * Retrieves the candidate's currently active Dynamic Learning Path (DLP).
     */
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DynamicLearningPathResponse> getCurrentPath(@AuthenticationPrincipal Jwt jwt) {
        UUID candidateId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        return learningPathGeneratorService.getCurrentActivePath(candidateId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    DynamicLearningPath path = learningPathGeneratorService.generateOrRecalculate(candidateId, null, null);
                    return ResponseEntity.ok(learningPathGeneratorService.toResponse(path));
                });
    }

    /**
     * Manually triggers DLP recalculation after diagnostic mock or practice session.
     */
    @PostMapping("/recalculate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DynamicLearningPathResponse> recalculate(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) RecalculateLearningPathRequest request) {
        UUID candidateId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        UUID sourceSessionId = request != null ? request.sourceSessionId() : null;
        String examTarget = request != null ? request.examTarget() : null;

        DynamicLearningPath path = learningPathGeneratorService.generateOrRecalculate(candidateId, sourceSessionId, examTarget);
        if (recommendationService != null) {
            try {
                recommendationService.generateOrRefresh(candidateId, examTarget);
            } catch (Exception e) {
                log.warn("Failed to generate/refresh recommendations during DLP recalculation for candidate {}: {}", candidateId, e.getMessage());
            }
        }
        return ResponseEntity.ok(learningPathGeneratorService.toResponse(path));
    }

    /**
     * Overview of mastery distribution across subjects and syllabus hierarchy.
     */
    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LearningPathSummaryResponse> getSummary(@AuthenticationPrincipal Jwt jwt) {
        UUID candidateId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        return ResponseEntity.ok(learningPathGeneratorService.getSummary(candidateId));
    }

    /**
     * Marks a Phase 1 remedial micro-lesson milestone as completed.
     */
    @PostMapping("/milestones/{milestoneId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DynamicLearningPathResponse> completeMilestone(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String milestoneId) {
        UUID candidateId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        DynamicLearningPathResponse response = milestoneProgressionService.completeMilestone(candidateId, milestoneId);
        return ResponseEntity.ok(response);
    }

    /**
     * Spawns Phase 2 adaptive mini-quiz once Phase 1 remediation is complete.
     */
    @PostMapping("/adaptive-quiz/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StartAdaptiveQuizResponse> startAdaptiveQuiz(@AuthenticationPrincipal Jwt jwt) {
        UUID candidateId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        StartAdaptiveQuizResponse response = milestoneProgressionService.startAdaptiveQuiz(candidateId);
        return ResponseEntity.ok(response);
    }

    /**
     * Evaluates Phase 2 adaptive quiz score (promotes to Phase 3 if score >= 70%).
     */
    @PostMapping("/adaptive-quiz/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DynamicLearningPathResponse> submitAdaptiveQuiz(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody SubmitQuizScoreRequest request) {
        UUID candidateId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        DynamicLearningPathResponse response = milestoneProgressionService.submitAdaptiveQuiz(candidateId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Spawns Phase 3 Gatekeeper test once Phase 2 is complete.
     */
    @PostMapping("/gatekeeper/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StartGatekeeperResponse> startGatekeeperTest(@AuthenticationPrincipal Jwt jwt) {
        UUID candidateId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        StartGatekeeperResponse response = milestoneProgressionService.startGatekeeperTest(candidateId);
        return ResponseEntity.ok(response);
    }

    /**
     * Evaluates Phase 3 Gatekeeper test score (unlocks full mock exam access if score >= 75%).
     */
    @PostMapping("/gatekeeper/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DynamicLearningPathResponse> submitGatekeeperTest(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody SubmitGatekeeperScoreRequest request) {
        UUID candidateId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        DynamicLearningPathResponse response = milestoneProgressionService.submitGatekeeperTest(candidateId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Checks if candidate is cleared for full mock exams or blocked by a pending Gatekeeper test.
     */
    @GetMapping("/gatekeeper/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GatekeeperStatusResponse> getGatekeeperStatus(@AuthenticationPrincipal Jwt jwt) {
        UUID candidateId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        GatekeeperStatusResponse response = milestoneProgressionService.getGatekeeperStatus(candidateId);
        return ResponseEntity.ok(response);
    }

    /**
     * Internal/admin lookup of candidate Gatekeeper clearance status.
     */
    @GetMapping("/gatekeeper/status/{candidateId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GatekeeperStatusResponse> getGatekeeperStatusByCandidateId(
            @PathVariable UUID candidateId) {
        GatekeeperStatusResponse response = milestoneProgressionService.getGatekeeperStatus(candidateId);
        return ResponseEntity.ok(response);
    }
}
