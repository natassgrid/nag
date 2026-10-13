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

package com.examplatform.questionbank.controller;

import com.examplatform.questionbank.dto.PassageRequest;
import com.examplatform.questionbank.dto.PassageResponse;
import com.examplatform.questionbank.dto.TransitionRequest;
import com.examplatform.questionbank.service.PassageLifecycleService;
import com.examplatform.questionbank.service.PassageService;
import com.examplatform.shared.api.ApiResponse;
import com.examplatform.shared.constants.SharedConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * REST controller for comprehension passage and case study lifecycle management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/passages")
@RequiredArgsConstructor
public class PassageController {

    private final PassageService passageService;
    private final PassageLifecycleService passageLifecycleService;

    @PostMapping
    @PreAuthorize("hasRole('QUESTION_AUTHOR')")
    public ResponseEntity<ApiResponse<PassageResponse>> createPassage(
            @Valid @RequestBody PassageRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(SharedConstants.HEADER_TENANT_ID) String tenantId) {

        UUID authorId = getPrincipalId(jwt);
        log.info("Creating passage: author={}, tenant={}, subjectId={}", authorId, tenantId, request.getSubjectId());

        PassageResponse response = passageService.createPassage(request, authorId, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Passage created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<Page<PassageResponse>>> listPassages(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(SharedConstants.HEADER_TENANT_ID) String tenantId) {

        Page<PassageResponse> response = passageService.listPassages(subjectId, state, search, page, size, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Passages retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<PassageResponse>> getPassage(
            @PathVariable UUID id,
            @RequestHeader(SharedConstants.HEADER_TENANT_ID) String tenantId) {

        PassageResponse response = passageService.getPassage(id, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Passage retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('QUESTION_AUTHOR')")
    public ResponseEntity<ApiResponse<PassageResponse>> updatePassage(
            @PathVariable UUID id,
            @Valid @RequestBody PassageRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(SharedConstants.HEADER_TENANT_ID) String tenantId) {

        UUID authorId = getPrincipalId(jwt);
        log.info("Updating passage id={}, author={}, tenant={}", id, authorId, tenantId);

        PassageResponse response = passageService.updatePassage(id, request, authorId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Passage updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('QUESTION_AUTHOR')")
    public ResponseEntity<ApiResponse<Void>> deletePassage(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(SharedConstants.HEADER_TENANT_ID) String tenantId) {

        passageService.deletePassage(id, getPrincipalId(jwt), tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "Passage deleted successfully"));
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasRole('QUESTION_AUTHOR')")
    public ResponseEntity<ApiResponse<PassageResponse>> submitForReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(SharedConstants.HEADER_TENANT_ID) String tenantId) {

        return ResponseEntity.ok(ApiResponse.success(
                passageLifecycleService.submitForReview(id, getPrincipalId(jwt), tenantId),
                "Passage submitted for review"));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<PassageResponse>> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(SharedConstants.HEADER_TENANT_ID) String tenantId) {

        return ResponseEntity.ok(ApiResponse.success(
                passageLifecycleService.approve(id, getPrincipalId(jwt), tenantId),
                "Passage approved successfully"));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<PassageResponse>> reject(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(SharedConstants.HEADER_TENANT_ID) String tenantId) {

        String comments = (body != null) ? body.get("comments") : null;
        PassageResponse response = passageLifecycleService.reject(id, getPrincipalId(jwt), comments, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Passage rejected"));
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<PassageResponse>> transition(
            @PathVariable UUID id,
            @Valid @RequestBody TransitionRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(SharedConstants.HEADER_TENANT_ID) String tenantId) {

        PassageResponse response = passageLifecycleService.transition(id, request, getPrincipalId(jwt), tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Passage state transitioned"));
    }

    private UUID getPrincipalId(Jwt jwt) {
        return UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
    }
}
