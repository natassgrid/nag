/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.recommendation.controller;

import com.examplatform.recommendation.dto.ActivityCompletionResponse;
import com.examplatform.recommendation.dto.CompleteActivityRequest;
import com.examplatform.recommendation.dto.LearnerProgressResponse;
import com.examplatform.recommendation.dto.NextActivityResponse;
import com.examplatform.recommendation.service.NextBestActivityEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for the exam-adaptive learning loop under Unified Exam Spine:
 * Next-best-activity engine, pedagogical feedback completion, and syllabus progress tracking.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/learning")
@RequiredArgsConstructor
public class LearnerActivityController {

    private final NextBestActivityEngine nextBestActivityEngine;

    /**
     * GET /api/v1/learning/enrollments/{id}/next-activity
     * Calculates the next-best-activity for the learner's active target examination enrollment.
     */
    @GetMapping("/enrollments/{id}/next-activity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NextActivityResponse> getNextActivity(@PathVariable UUID id) {
        return ResponseEntity.ok(nextBestActivityEngine.determineNextActivity(id));
    }

    /**
     * POST /api/v1/learning/enrollments/{id}/activities/{aid}/complete
     * Submits completion metrics for an assigned activity (lesson/practice/revision/mock)
     * and triggers Bayesian Knowledge Tracing mastery updates.
     */
    @PostMapping("/enrollments/{id}/activities/{aid}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ActivityCompletionResponse> completeActivity(
            @PathVariable UUID id,
            @PathVariable UUID aid,
            @RequestBody CompleteActivityRequest request) {
        return ResponseEntity.ok(nextBestActivityEngine.completeActivity(id, aid, request));
    }

    /**
     * GET /api/v1/learning/enrollments/{id}/progress
     * Retrieves overall examination readiness score, due spaced repetition revisions,
     * and granular subtopic-by-subtopic mastery heatmap data.
     */
    @GetMapping("/enrollments/{id}/progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LearnerProgressResponse> getProgress(@PathVariable UUID id) {
        return ResponseEntity.ok(nextBestActivityEngine.getProgress(id));
    }
}
