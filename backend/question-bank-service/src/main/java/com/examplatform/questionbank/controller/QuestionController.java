/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.questionbank.controller;

import com.examplatform.questionbank.domain.QuestionVersion;
import com.examplatform.questionbank.dto.CreateQuestionRequest;
import com.examplatform.questionbank.dto.QuestionAnalytics;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.dto.TransitionRequest;
import com.examplatform.questionbank.service.QuestionLifecycleService;
import com.examplatform.questionbank.service.QuestionSearchService;
import com.examplatform.questionbank.service.QuestionService;
import com.examplatform.questionbank.service.QuestionUpdateService;
import com.examplatform.questionbank.service.QuestionVersioningService;
import com.examplatform.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for question CRUD operations.
 *
 * Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 5.5
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final QuestionUpdateService questionUpdateService;
    private final QuestionVersioningService questionVersioningService;
    private final QuestionLifecycleService questionLifecycleService;
    private final QuestionSearchService questionSearchService;

    /**
     * Create a new question in DRAFT state.
     * Requires QUESTION_AUTHOR role.
     *
     * @param request   the question creation payload (validated)
     * @param jwt       the authenticated JWT principal
     * @param tenantId  tenant identifier from the X-Tenant-Id header
     * @return 201 Created with the question response
     */
    @PostMapping
    @PreAuthorize("hasRole('QUESTION_AUTHOR')")
    public ResponseEntity<ApiResponse<QuestionResponse>> createQuestion(
            @Valid @RequestBody CreateQuestionRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID authorId = UUID.fromString(jwt.getSubject());

        log.info("Creating question: type={}, subject={}, author={}, tenant={}",
                request.getQuestionType(), request.getSubject(), authorId, tenantId);

        QuestionResponse response = questionService.createQuestion(request, authorId, tenantId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Question created successfully"));
    }

    /**
     * List questions with optional filters and pagination.
     * Requires QUESTION_AUTHOR, REVIEWER, or APPROVER role.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<Page<QuestionResponse>>> listQuestions(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        log.info("Listing questions: subject={}, topic={}, difficulty={}, state={}, page={}, size={}, tenant={}",
                subject, topic, difficulty, state, page, size, tenantId);

        Page<QuestionResponse> responses = questionService.listQuestions(
                subject, topic, difficulty, state, page, size, tenantId);
        return ResponseEntity.ok(ApiResponse.success(responses, "Questions retrieved successfully"));
    }

    /**
     * Retrieve a question by ID.
     * Requires QUESTION_AUTHOR, REVIEWER, or APPROVER role.
     *
     * @param id the question UUID
     * @return 200 OK with the question response
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<QuestionResponse>> getQuestion(@PathVariable UUID id) {
        QuestionResponse response = questionService.getQuestion(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Question retrieved successfully"));
    }

    /**
     * Submit a DRAFT question for review — transitions to REVIEW state.
     * Requires QUESTION_AUTHOR role.
     *
     * @param id       the question UUID
     * @param jwt      the authenticated JWT principal
     * @param tenantId tenant identifier from the X-Tenant-Id header
     * @return 200 OK with the updated question response
     */
    @PutMapping("/{id}/submit")
    @PreAuthorize("hasRole('QUESTION_AUTHOR')")
    public ResponseEntity<ApiResponse<QuestionResponse>> submitForReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID authorId = UUID.fromString(jwt.getSubject());

        log.info("Submitting question for review: id={}, author={}, tenant={}", id, authorId, tenantId);

        QuestionResponse response = questionService.submitForReview(id, authorId, tenantId);

        return ResponseEntity.ok(ApiResponse.success(response, "Question submitted for review"));
    }

    /**
     * Update an existing question.
     * Requires QUESTION_AUTHOR role. Creates a version record tracking changes.
     *
     * @param id        the question UUID
     * @param request   the update payload (validated)
     * @param jwt       the authenticated JWT principal
     * @param tenantId  tenant identifier from the X-Tenant-Id header
     * @return 200 OK with the updated question response
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('QUESTION_AUTHOR')")
    public ResponseEntity<ApiResponse<QuestionResponse>> updateQuestion(
            @PathVariable UUID id,
            @Valid @RequestBody CreateQuestionRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID authorId = UUID.fromString(jwt.getSubject());

        log.info("Updating question: id={}, author={}, tenant={}", id, authorId, tenantId);

        QuestionResponse response = questionUpdateService.updateQuestion(id, request, authorId, tenantId);

        return ResponseEntity.ok(ApiResponse.success(response, "Question updated successfully"));
    }

    /**
     * Retrieve version history for a question.
     * Requires QUESTION_AUTHOR, REVIEWER, or APPROVER role.
     *
     * @param id the question UUID
     * @return 200 OK with the list of versions (newest first)
     */
    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<List<QuestionVersion>>> getQuestionVersions(@PathVariable UUID id) {
        List<QuestionVersion> versions = questionVersioningService.getVersions(id);
        return ResponseEntity.ok(ApiResponse.success(versions, "Question versions retrieved successfully"));
    }

    /**
     * Transition a question through the lifecycle FSM.
     * Requires REVIEWER or APPROVER role.
     * Rejects invalid transitions with HTTP 422.
     * Enforces four-eyes principle (reviewer ≠ approver) with HTTP 403.
     *
     * Validates: Requirements 4.6, 5.5
     *
     * @param id       the question UUID
     * @param request  the transition request containing target state
     * @param jwt      the authenticated JWT principal
     * @param tenantId tenant identifier from the X-Tenant-Id header
     * @return 200 OK with the updated question response
     */
    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<QuestionResponse>> transitionQuestion(
            @PathVariable UUID id,
            @Valid @RequestBody TransitionRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID actorId = UUID.fromString(jwt.getSubject());

        log.info("Transitioning question: id={}, targetState={}, actor={}, tenant={}",
                id, request.getTargetState(), actorId, tenantId);

        QuestionResponse response = questionLifecycleService.transition(id, request, actorId, tenantId);

        return ResponseEntity.ok(ApiResponse.success(response, "Question transitioned successfully"));
    }

    /**
     * Approve a question currently in REVIEW state.
     * Transitions the question to APPROVED state and records the reviewer.
     * Requires REVIEWER or APPROVER role.
     *
     * Validates: Requirements 5.1, 5.2
     *
     * @param id       the question UUID
     * @param jwt      the authenticated JWT principal
     * @param tenantId tenant identifier from the X-Tenant-Id header
     * @return 200 OK with the updated question response
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<QuestionResponse>> approveQuestion(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID reviewerId = UUID.fromString(jwt.getSubject());

        log.info("Approving question: id={}, reviewer={}, tenant={}", id, reviewerId, tenantId);

        QuestionResponse response = questionLifecycleService.approve(id, reviewerId, tenantId);

        return ResponseEntity.ok(ApiResponse.success(response, "Question approved"));
    }

    /**
     * Reject a question currently in REVIEW state.
     * Transitions the question back to DRAFT state for revision.
     * Requires REVIEWER or APPROVER role.
     *
     * Validates: Requirements 5.1, 5.3
     *
     * @param id       the question UUID
     * @param body     request body containing optional "comments" field
     * @param jwt      the authenticated JWT principal
     * @param tenantId tenant identifier from the X-Tenant-Id header
     * @return 200 OK with the updated question response
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<QuestionResponse>> rejectQuestion(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID reviewerId = UUID.fromString(jwt.getSubject());
        String comments = body.getOrDefault("comments", "");

        log.info("Rejecting question: id={}, reviewer={}, tenant={}", id, reviewerId, tenantId);

        QuestionResponse response = questionLifecycleService.reject(id, reviewerId, comments, tenantId);

        return ResponseEntity.ok(ApiResponse.success(response, "Question rejected"));
    }

    /**
     * Full-text search for questions with optional subject and difficulty filters.
     * Falls back to JPA LIKE queries; production uses OpenSearch for sub-2-second p95.
     *
     * Validates: Requirements 19.3, 26.5
     *
     * @param query      free-text search query
     * @param subject    optional subject filter
     * @param difficulty optional difficulty filter
     * @param page       page number (0-based, default 0)
     * @param size       page size (default 20)
     * @param tenantId   tenant identifier from the X-Tenant-Id header
     * @return 200 OK with paginated question results
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<Page<QuestionResponse>>> searchQuestions(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        Page<QuestionResponse> results = questionSearchService.search(query, subject, difficulty, page, size, tenantId);
        return ResponseEntity.ok(ApiResponse.success(results, "Search completed successfully"));
    }

    /**
     * Retrieves analytics data for a specific question.
     *
     * Validates: Requirements 26.5
     *
     * @param id the question UUID
     * @return 200 OK with analytics data
     */
    @GetMapping("/{id}/analytics")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<QuestionAnalytics>> getQuestionAnalytics(@PathVariable UUID id) {
        QuestionResponse question = questionService.getQuestion(id);

        // Stub analytics — production would compute from evaluation data
        QuestionAnalytics analytics = QuestionAnalytics.builder()
                .difficultyIndex(0.0)
                .discriminationIndex(0.0)
                .usageCount(0)
                .build();

        return ResponseEntity.ok(ApiResponse.success(analytics, "Analytics retrieved successfully"));
    }
}
