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

package com.examplatform.questionbank.controller;

import com.examplatform.questionbank.dto.MergeTaxonomyRequest;
import com.examplatform.questionbank.dto.ReorderTaxonomyRequest;
import com.examplatform.questionbank.dto.TaxonomyOverrideRequest;
import com.examplatform.questionbank.service.TaxonomyService;
import com.examplatform.shared.api.ApiResponse;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.taxonomy.TaxonomySubjectNode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for unified taxonomy operations: effective tree resolution,
 * tenant overrides, node merging, and reordering.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/taxonomy")
@RequiredArgsConstructor
@Tag(name = "Taxonomy", description = "Endpoints for platform taxonomy and tenant overrides")
public class TaxonomyController {

    private final TaxonomyService taxonomyService;

    /**
     * Retrieves the effective taxonomy tree for the caller's tenant, optionally scoped to an exam syllabus.
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN') or isAuthenticated()")
    public ResponseEntity<ApiResponse<List<TaxonomySubjectNode>>> getEffectiveTree(
            @RequestParam(required = false) UUID examId,
            @RequestHeader(value = SharedConstants.HEADER_TENANT_ID, required = false) String tenantId) {

        log.debug("GET /api/v1/taxonomy/tree: tenantId={}, examId={}", tenantId, examId);
        List<TaxonomySubjectNode> tree = taxonomyService.getEffectiveTree(tenantId, examId);
        return ResponseEntity.ok(ApiResponse.success(tree, "Effective taxonomy tree retrieved successfully"));
    }

    /**
     * Applies tenant-level overrides (hide, rename, reorder) on platform taxonomy nodes.
     */
    @PutMapping("/overrides")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> applyOverrides(
            @Valid @RequestBody List<TaxonomyOverrideRequest> requests,
            @RequestHeader(SharedConstants.HEADER_TENANT_ID) String tenantId) {

        log.info("PUT /api/v1/taxonomy/overrides: tenant={}, count={}", tenantId, requests.size());
        taxonomyService.applyOverrides(requests, tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "Taxonomy overrides applied successfully"));
    }

    /**
     * Merges two taxonomy nodes, updating referencing questions, lessons, and child nodes.
     */
    @PostMapping("/merge")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> mergeNodes(
            @Valid @RequestBody MergeTaxonomyRequest request,
            @RequestHeader(value = SharedConstants.HEADER_TENANT_ID, required = false) String tenantId) {

        log.info("POST /api/v1/taxonomy/merge: nodeType={}, source={}, target={}, tenant={}",
                request.getNodeType(), request.getSourceNodeId(), request.getTargetNodeId(), tenantId);
        taxonomyService.mergeNodes(request, tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "Taxonomy nodes merged successfully"));
    }

    /**
     * Reorders taxonomy nodes within their parent scope.
     */
    @PutMapping("/reorder")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reorderNodes(
            @Valid @RequestBody ReorderTaxonomyRequest request,
            @RequestHeader(value = SharedConstants.HEADER_TENANT_ID, required = false) String tenantId) {

        log.info("PUT /api/v1/taxonomy/reorder: nodeType={}, count={}, tenant={}",
                request.getNodeType(), request.getItems().size(), tenantId);
        taxonomyService.reorderNodes(request, tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "Taxonomy nodes reordered successfully"));
    }
}
