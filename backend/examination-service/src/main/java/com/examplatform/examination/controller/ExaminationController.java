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

package com.examplatform.examination.controller;

import com.examplatform.examination.dto.CreateExaminationRequest;
import com.examplatform.examination.dto.ExaminationResponse;
import com.examplatform.examination.service.ExaminationService;
import com.examplatform.shared.api.ApiResponse;
import com.examplatform.shared.tenant.TenantContext;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for examination CRUD operations.
 *
 * Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5, 7.6
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/examinations")
@RequiredArgsConstructor
public class ExaminationController {

    private final ExaminationService examinationService;

    /**
     * List all examinations for the current tenant (paginated). Requires EXAM_CONTROLLER role.
     */
    @GetMapping
    @PreAuthorize("hasRole('EXAM_CONTROLLER')")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<ExaminationResponse>>> list(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal Jwt jwt) {
        org.springframework.data.domain.Page<ExaminationResponse> responses =
                examinationService.listByTenantPaged(tenantId, search, page, size);
        return ResponseEntity.ok(ApiResponse.success(responses, "Examinations retrieved successfully"));
    }

    /**
     * Create a new examination. Requires EXAM_CONTROLLER role.
     */
    @PostMapping
    @PreAuthorize("hasRole('EXAM_CONTROLLER')")
    public ResponseEntity<ApiResponse<ExaminationResponse>> create(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody CreateExaminationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ExaminationResponse response = examinationService.create(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Examination created successfully"));
    }

    /**
     * Update an existing examination. Requires EXAM_CONTROLLER role.
     */
    @PutMapping("/{examId}")
    @PreAuthorize("hasRole('EXAM_CONTROLLER')")
    public ResponseEntity<ApiResponse<ExaminationResponse>> update(
            @PathVariable UUID examId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody CreateExaminationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ExaminationResponse response = examinationService.update(examId, request, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Examination updated successfully"));
    }

    /**
     * Publish an examination (transition from DRAFT to PUBLISHED). Requires EXAM_CONTROLLER role.
     */
    @PutMapping("/{examId}/publish")
    @PreAuthorize("hasRole('EXAM_CONTROLLER')")
    public ResponseEntity<ApiResponse<ExaminationResponse>> publish(
            @PathVariable UUID examId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {
        ExaminationResponse response = examinationService.publish(examId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Examination published successfully"));
    }

    /**
     * Retrieve an examination by ID. Requires EXAM_CONTROLLER or SUPER_ADMIN role.
     */
    @GetMapping("/{examId}")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ExaminationResponse>> getById(
            @PathVariable UUID examId,
            @AuthenticationPrincipal Jwt jwt) {
        ExaminationResponse response = examinationService.getById(examId);
        return ResponseEntity.ok(ApiResponse.success(response, "Examination retrieved successfully"));
    }
}
