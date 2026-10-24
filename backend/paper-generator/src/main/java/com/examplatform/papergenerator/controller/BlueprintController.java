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

package com.examplatform.papergenerator.controller;

import com.examplatform.papergenerator.dto.BlueprintFeasibilityRequest;
import com.examplatform.papergenerator.dto.BlueprintFeasibilityResponse;
import com.examplatform.papergenerator.dto.BlueprintRequest;
import com.examplatform.papergenerator.dto.BlueprintResponse;
import com.examplatform.papergenerator.service.BlueprintService;
import com.examplatform.papergenerator.service.PaperAssemblyService;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.security.UserContext;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for managing reusable examination blueprints.
 *
 * <pre>
 *   GET    /api/v1/papers/blueprints                         list all visible (platform + tenant)
 *   GET    /api/v1/papers/blueprints/{id}                    get one
 *   POST   /api/v1/papers/blueprints                         create
 *   PUT    /api/v1/papers/blueprints/{id}                    update
 *   DELETE /api/v1/papers/blueprints/{id}                    delete
 *   POST   /api/v1/papers/blueprints/{id}/check-sufficiency  audit sufficiency against question bank
 *   POST   /api/v1/papers/blueprints/check-sufficiency       inline audit sufficiency
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/papers/blueprints")
@RequiredArgsConstructor
public class BlueprintController {

    private final BlueprintService service;
    private final PaperAssemblyService paperAssemblyService;

    // ── List ────────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<List<BlueprintResponse>> list() {
        return ResponseEntity.ok(service.listVisible(tenantId()));
    }

    // ── Get One ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<BlueprintResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id, tenantId()));
    }

    /**
     * Real-time feasibility check: audits a blueprint against the live question bank.
     */
    @PostMapping("/{id}/check-sufficiency")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<BlueprintFeasibilityResponse> checkSufficiency(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID examId,
            @RequestParam(required = false, defaultValue = "1") int shifts,
            @RequestParam(defaultValue = "true") boolean notifyAdmin) {

        String tenantId = tenantId();
        BlueprintFeasibilityResponse response = service.checkSufficiency(id, examId, shifts, tenantId, notifyAdmin);
        return ResponseEntity.ok(response);
    }

    // ── Create ──────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<BlueprintResponse> create(
            @Valid @RequestBody BlueprintRequest request) {

        UUID createdBy = UserContext.getUserId();
        BlueprintResponse created = service.create(request, createdBy, tenantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── Update ──────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<BlueprintResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody BlueprintRequest request) {

        return ResponseEntity.ok(service.update(id, request, tenantId()));
    }

    // ── Delete ──────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id, tenantId());
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String tenantId() {
        String t = TenantContext.getTenantId();
        return (t != null && !t.isBlank()) ? t : SharedConstants.DEFAULT_TENANT;
    }
}
