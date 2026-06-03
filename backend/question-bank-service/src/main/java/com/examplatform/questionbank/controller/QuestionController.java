package com.examplatform.questionbank.controller;

import com.examplatform.questionbank.dto.CreateQuestionRequest;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.service.QuestionService;
import com.examplatform.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for question CRUD operations.
 *
 * Validates: Requirements 4.1, 4.2, 4.3, 4.5
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

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
}
