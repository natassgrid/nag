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
import com.examplatform.questionbank.dto.BlueprintMatchRequest;
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

import java.util.UUID;

/**
 * REST controller for question lifecycle management.
 * Provides endpoints for creating, retrieving, updating, approving, rejecting,
 * searching, and viewing version history of questions.
 *
 * Validates: Requirements 4.1, 4.4, 4.6, 5.1, 5.2, 5.3, 5.5, 19.3, 26.5
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final QuestionUpdateService questionUpdateService;
    private final QuestionLifecycleService questionLifecycleService;
    private final QuestionVersioningService questionVersioningService;
    private final QuestionSearchService questionSearchService;

    /**
     * Create a new question in DRAFT state.
     * Requires QUESTION_AUTHOR role.
     * Content and answerKey are encrypted using envelope encryption.
     *
     * Validates: Requirements 4.1, 4.2, 4.3, 4.5
     *
     * @param request  the validated creation payload
     * @param jwt      the authenticated JWT principal
     * @param tenantId tenant identifier from the X-Tenant-Id header
     * @return 201 Created with the created question response
     */
    @PostMapping
    @PreAuthorize("hasRole('QUESTION_AUTHOR')")
    public ResponseEntity<ApiResponse<QuestionResponse>> createQuestion(
            @Valid @RequestBody CreateQuestionRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID authorId = UUID.fromString(jwt.getSubject());

        log.info("Creating question: author={}, tenant={}, subjectId={}, topicId={}",
                authorId, tenantId, request.getSubjectId(), request.getTopicId());

        QuestionResponse response = questionService.createQuestion(request, authorId, tenantId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Question created successfully"));
    }

    /**
     * List questions for a tenant with optional filtering and pagination.
     * Requires QUESTION_AUTHOR, REVIEWER, APPROVER, TRANSLATOR, EXAM_CONTROLLER, or ADMIN role.
     *
     * @param subject           optional subject name filter
     * @param subjectId         optional subject numeric ID filter (enables partition pruning)
     * @param topic             optional topic name filter
     * @param topicId           optional topic numeric ID filter
     * @param difficulty        optional difficulty filter
     * @param state             optional state filter (DRAFT, REVIEW, APPROVED, etc.)
     * @param search            optional text search filter
     * @param targetLang        optional target language code filter (e.g. hi, ta, te)
     * @param translationStatus optional translation status filter (MISSING, DRAFT, IN_REVIEW, APPROVED, PUBLISHED, REJECTED, EXISTS)
     * @param page              page number (0-based, default 0)
     * @param size              page size (default 20)
     * @param tenantId          tenant identifier from the X-Tenant-Id header
     * @return 200 OK with paginated question responses
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'TRANSLATOR', 'EXAM_CONTROLLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<QuestionResponse>>> listQuestions(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String targetLang,
            @RequestParam(required = false) String translationStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        log.info("Listing questions: tenant={}, subject={}, subjectId={}, topic={}, topicId={}, difficulty={}, state={}, targetLang={}, translationStatus={}, page={}, size={}",
                tenantId, subject, subjectId, topic, topicId, difficulty, state, targetLang, translationStatus, page, size);

        Page<QuestionResponse> responses = questionService.listQuestions(
                subject, subjectId, topic, topicId, difficulty, state, search, targetLang, translationStatus, page, size, tenantId);
        return ResponseEntity.ok(ApiResponse.success(responses, "Questions retrieved successfully"));
    }

    /**
     * Retrieve a question by ID.
     * Requires QUESTION_AUTHOR, REVIEWER, APPROVER, TRANSLATOR, EXAM_CONTROLLER, or ADMIN role.
     *
     * @param id the question UUID
     * @return 200 OK with the question response
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'TRANSLATOR', 'EXAM_CONTROLLER', 'ADMIN')")
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
        return ResponseEntity.ok(ApiResponse.success(response, "Question submitted for review successfully"));
    }

    /**
     * Generic transition endpoint driven by the transition request payload.
     * Requires REVIEWER or APPROVER role.
     *
     * @param id       the question UUID
     * @param request  transition request containing targetState and optional comments
     * @param jwt      the authenticated JWT principal
     * @param tenantId tenant identifier from the X-Tenant-Id header
     * @return 200 OK with the updated question response
     */
    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<QuestionResponse>> transition(
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
     * Approve a question — transitions to APPROVED state.
     * Enforces the Four-Eyes Principle: approver cannot be the author.
     * Requires REVIEWER or APPROVER role.
     *
     * Validates: Requirements 5.2, 5.5
     *
     * @param id       the question UUID
     * @param jwt      the authenticated JWT principal
     * @param tenantId tenant identifier from the X-Tenant-Id header
     * @return 200 OK with the approved question response
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<QuestionResponse>> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID reviewerId = UUID.fromString(jwt.getSubject());
        log.info("Approving question: id={}, reviewer={}, tenant={}", id, reviewerId, tenantId);

        QuestionResponse response = questionLifecycleService.approve(id, reviewerId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Question approved successfully"));
    }

    /**
     * Reject a question with required comments — transitions back to DRAFT state.
     * Requires REVIEWER or APPROVER role.
     *
     * Validates: Requirement 5.3
     *
     * @param id       the question UUID
     * @param payload  map containing mandatory "comments"
     * @param jwt      the authenticated JWT principal
     * @param tenantId tenant identifier from the X-Tenant-Id header
     * @return 200 OK with the rejected question response (in DRAFT state)
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<QuestionResponse>> reject(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, String> payload,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID reviewerId = UUID.fromString(jwt.getSubject());
        String comments = payload.get("comments");
        if (comments == null || comments.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Comments are required when rejecting a question"));
        }

        log.info("Rejecting question: id={}, reviewer={}, tenant={}", id, reviewerId, tenantId);

        QuestionResponse response = questionLifecycleService.reject(id, reviewerId, comments, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Question rejected successfully"));
    }

    /**
     * Update an existing question.
     * If modified, creates a new QuestionVersion record and clears embedding.
     * Requires QUESTION_AUTHOR role.
     *
     * Validates: Requirements 4.4, 4.6
     *
     * @param id       the question UUID
     * @param request  the updated question payload
     * @param jwt      the authenticated JWT principal
     * @param tenantId tenant identifier from the X-Tenant-Id header
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
     * Retrieve the version history of a question.
     * Requires QUESTION_AUTHOR, REVIEWER, or APPROVER role.
     *
     * Validates: Requirement 4.6
     *
     * @param id the question UUID
     * @return 200 OK with the list of question versions
     */
    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<List<QuestionVersion>>> getVersions(@PathVariable UUID id) {
        List<QuestionVersion> versions = questionVersioningService.getVersions(id);
        return ResponseEntity.ok(ApiResponse.success(versions, "Version history retrieved successfully"));
    }

    /**
     * Search questions using full-text search with optional filters.
     * Requires QUESTION_AUTHOR, REVIEWER, or APPROVER role.
     *
     * Validates: Requirement 19.3
     *
     * @param query      search text query
     * @param difficulty optional difficulty filter
     * @param state      optional state filter
     * @param page       page number (0-based, default 0)
     * @param size       page size (default 20)
     * @param tenantId   tenant identifier from the X-Tenant-Id header
     * @return 200 OK with paginated matching question responses
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<Page<QuestionResponse>>> search(
            @RequestParam String query,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        log.info("Searching questions: query='{}', tenant={}, difficulty={}, state={}, page={}, size={}",
                query, tenantId, difficulty, state, page, size);

        Page<QuestionResponse> results = questionSearchService.search(
                query, difficulty, state, page, size, tenantId);
        return ResponseEntity.ok(ApiResponse.success(results, "Search completed successfully"));
    }

    /**
     * Retrieve analytics and exposure metrics for a question.
     * Requires EXAM_CONTROLLER or ADMIN role.
     *
     * Validates: Requirement 26.5
     *
     * @param id the question UUID
     * @return 200 OK with question analytics data
     */
    @GetMapping("/{id}/analytics")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<QuestionAnalytics>> getAnalytics(@PathVariable UUID id) {
        QuestionAnalytics analytics = questionLifecycleService.getAnalytics(id);
        return ResponseEntity.ok(ApiResponse.success(analytics, "Analytics retrieved successfully"));
    }

    /**
     * Find approved questions matching blueprint criteria for Paper Generator.
     * Requires EXAM_CONTROLLER or ADMIN role.
     *
     * @param request  blueprint matching criteria
     * @param tenantId tenant identifier from the X-Tenant-Id header
     * @return 200 OK with matching approved questions
     */
    @PostMapping("/match-blueprint")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> matchBlueprint(
            @Valid @RequestBody BlueprintMatchRequest request,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {

        List<QuestionResponse> questions = questionService.findBlueprintQuestions(
                request.getSubject(),
                request.getTopic(),
                request.getDifficulty(),
                request.getCognitiveLevel(),
                tenantId
        );
        return ResponseEntity.ok(ApiResponse.success(questions, "Blueprint questions retrieved successfully"));
    }

    /**
     * Find questions by a list of UUIDs for Paper Generator review.
     * Requires EXAM_CONTROLLER or ADMIN role.
     *
     * @param ids      list of question UUIDs
     * @param tenantId tenant identifier from the X-Tenant-Id header
     * @return 200 OK with questions
     */
    @PostMapping("/by-ids")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> getQuestionsByIds(
            @RequestBody List<UUID> ids,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {

        List<QuestionResponse> questions = questionService.findQuestionsByIds(ids, tenantId);
        return ResponseEntity.ok(ApiResponse.success(questions, "Questions retrieved successfully"));
    }
}
