/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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
import com.examplatform.shared.constants.SharedConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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
     * List all examinations for the current tenant (paginated). Requires appropriate staff/admin roles.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN', 'QUESTION_AUTHOR', 'REVIEWER')")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<ExaminationResponse>>> list(
            @RequestHeader(SharedConstants.HEADER_TENANT_ID) String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String order,
            @AuthenticationPrincipal Jwt jwt) {
        org.springframework.data.domain.Page<ExaminationResponse> responses =
                examinationService.listByTenantPaged(tenantId, search, sort, order, page, size);
        return ResponseEntity.ok(ApiResponse.success(responses, "Examinations retrieved successfully"));
    }

    /**
     * Create a new examination. Requires EXAM_CONTROLLER role.
     */
    @PostMapping
    @PreAuthorize("hasRole('EXAM_CONTROLLER')")
    public ResponseEntity<ApiResponse<ExaminationResponse>> create(
            @RequestHeader(SharedConstants.HEADER_TENANT_ID) String tenantId,
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
            @RequestHeader(SharedConstants.HEADER_TENANT_ID) String tenantId,
            @Valid @RequestBody CreateExaminationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ExaminationResponse response = examinationService.update(examId, request, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Examination updated successfully"));
    }

    /**
     * Publish an examination (transition from DRAFT to PUBLISHED). Requires EXAM_CONTROLLER role.
     * Supports both PUT and POST for client flexibility.
     */
    @RequestMapping(value = "/{examId}/publish", method = {RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasRole('EXAM_CONTROLLER')")
    public ResponseEntity<ApiResponse<ExaminationResponse>> publish(
            @PathVariable UUID examId,
            @RequestHeader(SharedConstants.HEADER_TENANT_ID) String tenantId,
            @AuthenticationPrincipal Jwt jwt) {
        ExaminationResponse response = examinationService.publish(examId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Examination published successfully"));
    }

    /**
     * Retrieve an examination by ID. Requires staff or admin role.
     */
    @GetMapping("/{examId}")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN', 'QUESTION_AUTHOR', 'REVIEWER')")
    public ResponseEntity<ApiResponse<ExaminationResponse>> getById(
            @PathVariable UUID examId,
            @AuthenticationPrincipal Jwt jwt) {
        ExaminationResponse response = examinationService.getById(examId);
        return ResponseEntity.ok(ApiResponse.success(response, "Examination retrieved successfully"));
    }

    /**
     * Retrieves the declared syllabus nodes and scoped hierarchy tree for an examination.
     */
    @GetMapping("/{examId}/syllabus")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN', 'QUESTION_AUTHOR', 'REVIEWER', 'CANDIDATE')")
    public ResponseEntity<ApiResponse<com.examplatform.examination.dto.ExamSyllabusResponse>> getSyllabus(
            @PathVariable UUID examId,
            @RequestHeader(value = SharedConstants.HEADER_TENANT_ID, required = false) String tenantId,
            @AuthenticationPrincipal Jwt jwt) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : SharedConstants.DEFAULT_TENANT;
        com.examplatform.examination.dto.ExamSyllabusResponse response =
                examinationService.getSyllabus(examId, effectiveTenant);
        return ResponseEntity.ok(ApiResponse.success(response, "Examination syllabus retrieved successfully"));
    }

    /**
     * Replaces the syllabus nodes of an examination. Only permitted in DRAFT status.
     */
    @PutMapping("/{examId}/syllabus")
    @PreAuthorize("hasRole('EXAM_CONTROLLER')")
    public ResponseEntity<ApiResponse<com.examplatform.examination.dto.ExamSyllabusResponse>> updateSyllabus(
            @PathVariable UUID examId,
            @RequestHeader(SharedConstants.HEADER_TENANT_ID) String tenantId,
            @Valid @RequestBody com.examplatform.examination.dto.ExamSyllabusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        com.examplatform.examination.dto.ExamSyllabusResponse response =
                examinationService.updateSyllabus(examId, request.getNodes(), tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Examination syllabus updated successfully"));
    }

    /**
     * Public / Learner catalogue endpoint for discovering published examinations available for target enrollment.
     */
    @GetMapping("/catalogue")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<com.examplatform.examination.dto.ExamCatalogueItemResponse>>> listCatalogue(
            @RequestHeader(value = SharedConstants.HEADER_TENANT_ID, required = false) String tenantId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : SharedConstants.DEFAULT_TENANT;
        org.springframework.data.domain.Page<com.examplatform.examination.dto.ExamCatalogueItemResponse> catalogue =
                examinationService.listCatalogue(effectiveTenant, search, category, org.springframework.data.domain.PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(catalogue, "Published examination catalogue retrieved successfully"));
    }
}
