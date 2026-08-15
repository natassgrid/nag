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

import com.examplatform.examination.dto.schedule.AmendScheduleRequest;
import com.examplatform.examination.dto.schedule.CreateScheduleRequest;
import com.examplatform.examination.dto.schedule.CreateShiftRequest;
import com.examplatform.examination.dto.schedule.ScheduleResponse;
import com.examplatform.examination.dto.schedule.ScheduleTransitionRequest;
import com.examplatform.examination.dto.schedule.ShiftResponse;
import com.examplatform.examination.service.ExaminationScheduleService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for examination schedule and shift management.
 *
 * Base path: /api/v1/examinations/{examId}/schedules
 * Validates: Requirements 7b.1–7b.4, 7b.7–7b.10, 7b.13
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/examinations/{examId}/schedules")
@RequiredArgsConstructor
public class ExaminationScheduleController {

    private final ExaminationScheduleService scheduleService;

    // ── Schedules ─────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ScheduleResponse>> create(
            @PathVariable UUID examId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody CreateScheduleRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID actorId = UUID.fromString(jwt.getSubject());
        ScheduleResponse response = scheduleService.createSchedule(examId, request, actorId, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Schedule created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN','SECURITY_ADMIN')")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<ScheduleResponse>>> list(
            @PathVariable UUID examId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {

        org.springframework.data.domain.Page<ScheduleResponse> responses =
                scheduleService.listSchedulesPaged(examId, tenantId, page, size);
        return ResponseEntity.ok(ApiResponse.success(responses, "Schedules retrieved successfully"));
    }

    @GetMapping("/{scheduleId}")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN','SECURITY_ADMIN')")
    public ResponseEntity<ApiResponse<ScheduleResponse>> getById(
            @PathVariable UUID examId,
            @PathVariable UUID scheduleId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        ScheduleResponse response = scheduleService.getSchedule(scheduleId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Schedule retrieved successfully"));
    }

    /**
     * Transition through the approval workflow (Req 7b.7).
     * Actor roles are extracted from the JWT realm_access.roles claim.
     */
    @PutMapping("/{scheduleId}/transition")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN','SECURITY_ADMIN')")
    public ResponseEntity<ApiResponse<ScheduleResponse>> transition(
            @PathVariable UUID examId,
            @PathVariable UUID scheduleId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody ScheduleTransitionRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID actorId = UUID.fromString(jwt.getSubject());
        Set<String> actorRoles = extractRoles(jwt);
        ScheduleResponse response = scheduleService.transitionSchedule(
                scheduleId, request, actorId, actorRoles, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Schedule transitioned to " + request.getTargetStatus()));
    }

    /**
     * Amend a published schedule — creates a new version row (Req 7b.8).
     */
    @PutMapping("/{scheduleId}/amend")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ScheduleResponse>> amend(
            @PathVariable UUID examId,
            @PathVariable UUID scheduleId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody AmendScheduleRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID actorId = UUID.fromString(jwt.getSubject());
        ScheduleResponse response = scheduleService.amendSchedule(scheduleId, request, actorId, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Schedule amendment created as version " + response.getScheduleVersion()));
    }

    // ── Shifts ────────────────────────────────────────────────────────────────

    @PostMapping("/{scheduleId}/shifts")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ShiftResponse>> addShift(
            @PathVariable UUID examId,
            @PathVariable UUID scheduleId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody CreateShiftRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID actorId = UUID.fromString(jwt.getSubject());
        ShiftResponse response = scheduleService.addShift(scheduleId, request, actorId, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Shift created successfully"));
    }

    @PutMapping("/{scheduleId}/shifts/{shiftId}")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ShiftResponse>> updateShift(
            @PathVariable UUID examId,
            @PathVariable UUID scheduleId,
            @PathVariable UUID shiftId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody CreateShiftRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID actorId = UUID.fromString(jwt.getSubject());
        ShiftResponse response = scheduleService.updateShift(scheduleId, shiftId, request, actorId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Shift updated successfully"));
    }

    @GetMapping("/{scheduleId}/shifts")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN','SECURITY_ADMIN')")
    public ResponseEntity<ApiResponse<List<ShiftResponse>>> listShifts(
            @PathVariable UUID examId,
            @PathVariable UUID scheduleId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        List<ShiftResponse> shifts = scheduleService.listShifts(scheduleId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(shifts, "Shifts retrieved successfully"));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Set<String> extractRoles(Jwt jwt) {
        try {
            java.util.Map<String, Object> realmAccess =
                    (java.util.Map<String, Object>) jwt.getClaim("realm_access");
            if (realmAccess == null) return Set.of();
            java.util.Collection<String> roles =
                    (java.util.Collection<String>) realmAccess.get("roles");
            return roles == null ? Set.of() : Set.copyOf(roles);
        } catch (Exception e) {
            return Set.of();
        }
    }
}
