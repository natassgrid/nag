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

package com.examplatform.candidate.controller;

import com.examplatform.candidate.dto.CandidateProfileResponse;
import com.examplatform.candidate.dto.ConsentRequest;
import com.examplatform.candidate.dto.CreateCandidateProfileRequest;
import com.examplatform.candidate.dto.FaceVerificationRequest;
import com.examplatform.candidate.dto.UpdateCandidateProfileRequest;
import com.examplatform.candidate.service.CandidateProfileService;
import com.examplatform.candidate.service.DigiLockerService;
import com.examplatform.candidate.service.FaceVerificationService;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for candidate profile CRUD and DPDP erasure operations.
 *
 * Validates: Requirements 1.6, 25.2
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/candidates")
@RequiredArgsConstructor
public class CandidateProfileController {

    private final CandidateProfileService candidateProfileService;
    private final DigiLockerService digiLockerService;
    private final FaceVerificationService faceVerificationService;

    /**
     * Create a new candidate profile. Requires CANDIDATE role.
     */
    @PostMapping
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<CandidateProfileResponse> create(
            @Valid @RequestBody CreateCandidateProfileRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantContext.get();
        CandidateProfileResponse response = candidateProfileService.create(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get candidate profile. CANDIDATE can get own profile, SUPER_ADMIN can get any.
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'SUPER_ADMIN')")
    public ResponseEntity<CandidateProfileResponse> getByUserId(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        enforceOwnershipOrAdmin(userId, jwt);
        String tenantId = TenantContext.get();
        CandidateProfileResponse response = candidateProfileService.getByUserId(userId, tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update candidate profile. CANDIDATE can update own profile only.
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<CandidateProfileResponse> update(
            @PathVariable UUID userId,
            @RequestBody UpdateCandidateProfileRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        enforceOwnership(userId, jwt);
        String tenantId = TenantContext.get();
        CandidateProfileResponse response = candidateProfileService.update(userId, request, tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * DPDP erasure endpoint. CANDIDATE can erase own data, SUPER_ADMIN can erase any.
     */
    @DeleteMapping("/{userId}/pii")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'SUPER_ADMIN')")
    public ResponseEntity<Void> erasePii(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        enforceOwnershipOrAdmin(userId, jwt);
        String tenantId = TenantContext.get();
        candidateProfileService.erasePii(userId, tenantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Record explicit consent before biometric data collection. Requires CANDIDATE role.
     *
     * Validates: Requirements 25.3
     */
    @PostMapping("/{userId}/consent")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Void> recordConsent(
            @PathVariable UUID userId,
            @Valid @RequestBody ConsentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        if (!request.isConsentGiven()) {
            return ResponseEntity.badRequest().build();
        }
        enforceOwnership(userId, jwt);
        String tenantId = TenantContext.get();
        candidateProfileService.recordConsent(userId, tenantId);
        return ResponseEntity.ok().build();
    }

    /**
     * Verify candidate identity document via DigiLocker. Requires CANDIDATE role.
     *
     * Validates: Requirements 1.3
     */
    @PostMapping("/{userId}/digilocker/verify")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Map<String, String>> verifyDigiLocker(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        enforceOwnership(userId, jwt);
        String tenantId = TenantContext.get();
        String status = digiLockerService.verifyDocument(userId, tenantId);
        return ResponseEntity.ok(Map.of("status", status));
    }

    /**
     * Verify candidate face against identity document photograph. Requires CANDIDATE role.
     *
     * Validates: Requirements 1.4
     */
    @PostMapping("/{userId}/face/verify")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Map<String, String>> verifyFace(
            @PathVariable UUID userId,
            @Valid @RequestBody FaceVerificationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        enforceOwnership(userId, jwt);
        String tenantId = TenantContext.get();
        String status = faceVerificationService.verifyFace(userId, request, tenantId);
        return ResponseEntity.ok(Map.of("status", status));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void enforceOwnership(UUID userId, Jwt jwt) {
        String sub = jwt.getSubject();
        if (!userId.toString().equals(sub)) {
            throw new AccessDeniedException("You can only access your own profile");
        }
    }

    private void enforceOwnershipOrAdmin(UUID userId, Jwt jwt) {
        String sub = jwt.getSubject();
        if (userId.toString().equals(sub)) {
            return; // Owner access
        }
        // Check if user has SUPER_ADMIN role
        var authorities = jwt.getClaimAsStringList("realm_access.roles");
        if (authorities != null && authorities.contains("SUPER_ADMIN")) {
            return;
        }
        throw new AccessDeniedException("You can only access your own profile");
    }
}
