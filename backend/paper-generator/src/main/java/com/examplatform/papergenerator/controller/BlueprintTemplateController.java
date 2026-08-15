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

package com.examplatform.papergenerator.controller;

import com.examplatform.papergenerator.dto.BlueprintTemplateRequest;
import com.examplatform.papergenerator.dto.BlueprintTemplateResponse;
import com.examplatform.papergenerator.service.BlueprintTemplateService;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST API for managing reusable blueprint templates.
 *
 * <pre>
 *   GET    /api/v1/papers/blueprint-templates            list all (tenant-scoped)
 *   GET    /api/v1/papers/blueprint-templates?examId=... list pinned to an exam
 *   GET    /api/v1/papers/blueprint-templates/{id}       get one
 *   POST   /api/v1/papers/blueprint-templates            create
 *   PUT    /api/v1/papers/blueprint-templates/{id}       update
 *   DELETE /api/v1/papers/blueprint-templates/{id}       delete
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/papers/blueprint-templates")
@RequiredArgsConstructor
public class BlueprintTemplateController {

    private final BlueprintTemplateService service;

    // ── List ──────────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<List<BlueprintTemplateResponse>> list(
            @RequestParam(required = false) UUID examId) {

        String tenantId = tenantId();
        List<BlueprintTemplateResponse> templates = examId != null
                ? service.listByExam(examId, tenantId)
                : service.listAll(tenantId);

        return ResponseEntity.ok(templates);
    }

    // ── Get one ───────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<BlueprintTemplateResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id, tenantId()));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<BlueprintTemplateResponse> create(
            @Valid @RequestBody BlueprintTemplateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID createdBy = UUID.fromString(jwt.getSubject());
        BlueprintTemplateResponse response = service.create(request, createdBy, tenantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<BlueprintTemplateResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody BlueprintTemplateRequest request) {

        return ResponseEntity.ok(service.update(id, request, tenantId()));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id, tenantId());
        return ResponseEntity.noContent().build();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String tenantId() {
        String t = TenantContext.get();
        return t != null ? t : "default";
    }
}
