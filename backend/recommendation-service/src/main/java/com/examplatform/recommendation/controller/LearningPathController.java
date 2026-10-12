// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.controller;

import com.examplatform.recommendation.domain.DynamicLearningPath;
import com.examplatform.recommendation.dto.DynamicLearningPathResponse;
import com.examplatform.recommendation.dto.LearningPathSummaryResponse;
import com.examplatform.recommendation.dto.RecalculateLearningPathRequest;
import com.examplatform.recommendation.service.LearningPathGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/learning-path")
@RequiredArgsConstructor
public class LearningPathController {

    private final LearningPathGeneratorService learningPathGeneratorService;

    /**
     * Retrieves the candidate's currently active Dynamic Learning Path (DLP).
     */
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DynamicLearningPathResponse> getCurrentPath(@AuthenticationPrincipal Jwt jwt) {
        UUID candidateId = UUID.fromString(jwt.getSubject());
        return learningPathGeneratorService.getCurrentActivePath(candidateId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    // Auto-generate initial path on the fly if candidate has performance data
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
        UUID candidateId = UUID.fromString(jwt.getSubject());
        UUID sourceSessionId = request != null ? request.sourceSessionId() : null;
        String examTarget = request != null ? request.examTarget() : null;

        DynamicLearningPath path = learningPathGeneratorService.generateOrRecalculate(candidateId, sourceSessionId, examTarget);
        return ResponseEntity.ok(learningPathGeneratorService.toResponse(path));
    }

    /**
     * Overview of mastery distribution across subjects and syllabus hierarchy.
     */
    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LearningPathSummaryResponse> getSummary(@AuthenticationPrincipal Jwt jwt) {
        UUID candidateId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(learningPathGeneratorService.getSummary(candidateId));
    }
}
