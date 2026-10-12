// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.controller;

import com.examplatform.recommendation.domain.DynamicLearningPath;
import com.examplatform.recommendation.dto.DynamicLearningPathResponse;
import com.examplatform.recommendation.dto.LearnerProfileDto;
import com.examplatform.recommendation.dto.LearningPathSummaryResponse;
import com.examplatform.recommendation.dto.RecalculateLearningPathRequest;
import com.examplatform.recommendation.dto.RecommendationDto;
import com.examplatform.recommendation.service.LearnerProfileService;
import com.examplatform.recommendation.service.LearningPathGeneratorService;
import com.examplatform.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<RecommendationDto> getLatest(@AuthenticationPrincipal Jwt jwt) {
        UUID candidateId = UUID.fromString(jwt.getSubject());
        return recommendationService.getLatest(candidateId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<RecommendationDto>> getHistory(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size) {
        UUID candidateId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(recommendationService.getHistory(candidateId, PageRequest.of(page, size)));
    }

    @PostMapping("/{id}/dismiss")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> dismiss(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        UUID candidateId = UUID.fromString(jwt.getSubject());
        recommendationService.dismiss(id, candidateId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/learner-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LearnerProfileDto> getLearnerProfile(@AuthenticationPrincipal Jwt jwt) {
        UUID candidateId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(learnerProfileService.getProfile(candidateId));
    }

    // Dynamic Learning Path (DLP) endpoints exposed alongside legacy recommendations
    @GetMapping("/learning-path/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DynamicLearningPathResponse> getCurrentLearningPath(@AuthenticationPrincipal Jwt jwt) {
        UUID candidateId = UUID.fromString(jwt.getSubject());
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
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) RecalculateLearningPathRequest request) {
        UUID candidateId = UUID.fromString(jwt.getSubject());
        UUID sourceSessionId = request != null ? request.sourceSessionId() : null;
        String examTarget = request != null ? request.examTarget() : null;
        DynamicLearningPath path = learningPathGeneratorService.generateOrRecalculate(candidateId, sourceSessionId, examTarget);
        return ResponseEntity.ok(learningPathGeneratorService.toResponse(path));
    }

    @GetMapping("/learning-path/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LearningPathSummaryResponse> getLearningPathSummary(@AuthenticationPrincipal Jwt jwt) {
        UUID candidateId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(learningPathGeneratorService.getSummary(candidateId));
    }
}
