package com.examplatform.examination.controller;

import com.examplatform.examination.dto.schedule.CentreResponse;
import com.examplatform.examination.dto.schedule.CreateCentreRequest;
import com.examplatform.examination.dto.schedule.SeatAllocationRequest;
import com.examplatform.examination.dto.schedule.SeatAllocationResponse;
import com.examplatform.examination.service.ExaminationCentreService;
import com.examplatform.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * REST controller for examination centre management and seat allocation.
 *
 * Centres: /api/v1/examinations/centres
 * Allocations: /api/v1/examinations/{examId}/schedules/{scheduleId}/shifts/{shiftId}/allocations
 *
 * Validates: Requirements 7b.5, 7b.6, 7b.11
 */
@RestController
@RequiredArgsConstructor
public class ExaminationCentreController {

    private final ExaminationCentreService centreService;

    // ── Centres ───────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/examinations/centres")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CentreResponse>> createCentre(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody CreateCentreRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        CentreResponse response = centreService.createCentre(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Centre created successfully"));
    }

    @GetMapping("/api/v1/examinations/centres")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN','SECURITY_ADMIN')")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<CentreResponse>>> listCentres(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String city,
            @AuthenticationPrincipal Jwt jwt) {

        org.springframework.data.domain.Page<CentreResponse> centres =
                centreService.listCentresPaged(tenantId, search, state, city, page, size);
        return ResponseEntity.ok(ApiResponse.success(centres, "Centres retrieved successfully"));
    }

    @GetMapping("/api/v1/examinations/centres/{centreId}")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN','SECURITY_ADMIN')")
    public ResponseEntity<ApiResponse<CentreResponse>> getCentre(
            @PathVariable UUID centreId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(
                ApiResponse.success(centreService.getCentre(centreId, tenantId), "Centre retrieved"));
    }

    @PutMapping("/api/v1/examinations/centres/{centreId}/deactivate")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CentreResponse>> deactivateCentre(
            @PathVariable UUID centreId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(
                ApiResponse.success(centreService.deactivateCentre(centreId, tenantId),
                        "Centre deactivated"));
    }

    // ── Seat Allocation ───────────────────────────────────────────────────────

    @PostMapping("/api/v1/examinations/{examId}/schedules/{scheduleId}/shifts/{shiftId}/allocations")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SeatAllocationResponse>> upsertAllocation(
            @PathVariable UUID examId,
            @PathVariable UUID scheduleId,
            @PathVariable UUID shiftId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody SeatAllocationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID actorId = UUID.fromString(jwt.getSubject());
        SeatAllocationResponse response = centreService.upsertAllocation(shiftId, request, actorId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Seat allocation updated successfully"));
    }

    @GetMapping("/api/v1/examinations/{examId}/schedules/{scheduleId}/shifts/{shiftId}/allocations")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN','SECURITY_ADMIN')")
    public ResponseEntity<ApiResponse<List<SeatAllocationResponse>>> listAllocations(
            @PathVariable UUID examId,
            @PathVariable UUID scheduleId,
            @PathVariable UUID shiftId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        List<SeatAllocationResponse> allocations = centreService.listAllocations(shiftId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(allocations, "Allocations retrieved successfully"));
    }
}
