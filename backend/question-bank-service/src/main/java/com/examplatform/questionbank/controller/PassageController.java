/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU标志 Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.questionbank.controller;

import com.examplatform.questionbank.dto.PassageRequest;
import com.examplatform.questionbank.dto.PassageResponse;
import com.examplatform.questionbank.dto.TransitionRequest;
import com.examplatform.questionbank.service.PassageLifecycleService;
import com.examplatform.questionbank.service.PassageService;
import com.examplatform.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
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
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID authorId = UUID.fromString(jwt.getSubject());
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
            @RequestHeader("X-Tenant-Id") String tenantId) {

        Page<PassageResponse> response = passageService.listPassages(subjectId, state, search, page, size, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Passages retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<PassageResponse>> getPassage(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        PassageResponse response = passageService.getPassage(id, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Passage retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('QUESTION_AUTHOR')")
    public ResponseEntity<ApiResponse<PassageResponse>> updatePassage(
            @PathVariable UUID id,
            @Valid @RequestBody PassageRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID authorId = UUID.fromString(jwt.getSubject());
        log.info("Updating passage id={}, author={}, tenant={}", id, authorId, tenantId);

        PassageResponse response = passageService.updatePassage(id, request, authorId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Passage updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('QUESTION_AUTHOR')")
    public ResponseEntity<ApiResponse<Void>> deletePassage(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID authorId = UUID.fromString(jwt.getSubject());
        passageService.deletePassage(id, authorId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "Passage deleted successfully"));
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasRole('QUESTION_AUTHOR')")
    public ResponseEntity<ApiResponse<PassageResponse>> submitForReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID authorId = UUID.fromString(jwt.getSubject());
        PassageResponse response = passageLifecycleService.submitForReview(id, authorId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Passage submitted for review"));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<PassageResponse>> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID reviewerId = UUID.fromString(jwt.getSubject());
        PassageResponse response = passageLifecycleService.approve(id, reviewerId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Passage approved successfully"));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<PassageResponse>> reject(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID reviewerId = UUID.fromString(jwt.getSubject());
        String comments = (body != null) ? body.get("comments") : null;
        PassageResponse response = passageLifecycleService.reject(id, reviewerId, comments, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Passage rejected"));
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('REVIEWER', 'APPROVER')")
    public ResponseEntity<ApiResponse<PassageResponse>> transition(
            @PathVariable UUID id,
            @Valid @RequestBody TransitionRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID actorId = UUID.fromString(jwt.getSubject());
        PassageResponse response = passageLifecycleService.transition(id, request, actorId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Passage state transitioned"));
    }
}
