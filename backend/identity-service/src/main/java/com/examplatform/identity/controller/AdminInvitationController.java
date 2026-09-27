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

package com.examplatform.identity.controller;

import com.examplatform.identity.dto.AcceptInviteRequest;
import com.examplatform.identity.dto.AdminInviteRequest;
import com.examplatform.identity.dto.AdminInviteResponse;
import com.examplatform.identity.dto.AuthTokenResponse;
import com.examplatform.identity.dto.ValidateInviteResponse;
import com.examplatform.identity.service.AdminInvitationService;
import com.examplatform.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/identity/admin/invite")
@RequiredArgsConstructor
public class AdminInvitationController {

    private final AdminInvitationService adminInvitationService;

    /**
     * Send email invitation to a new admin / staff user.
     * Restricted to SUPER_ADMIN, SECURITY_ADMIN, or TENANT_ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<AdminInviteResponse>> inviteAdmin(
            @Valid @RequestBody AdminInviteRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            Authentication authentication) {

        UUID actorId = null;
        try {
            actorId = UUID.fromString(authentication.getName());
        } catch (Exception ignored) {}

        log.info("Admin [{}] issuing invitation to [{}] with roles [{}] in tenant [{}]",
                authentication.getName(), request.getEmail(), request.getRoles(), tenantId);

        AdminInviteResponse response = adminInvitationService.inviteAdmin(request, actorId, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Invitation sent successfully."));
    }

    /**
     * Validate an invitation token before displaying onboarding 2FA setup form.
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<ValidateInviteResponse>> validateInvite(
            @RequestParam String token) {
        log.debug("Validating admin invitation token");
        ValidateInviteResponse response = adminInvitationService.validateInvitationToken(token);
        return ResponseEntity.ok(ApiResponse.success(response, "Invitation token is valid."));
    }

    /**
     * Accept invitation: set password, configure 2FA TOTP, activate account.
     */
    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> acceptInvite(
            @Valid @RequestBody AcceptInviteRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        log.info("Accepting admin invitation");
        AuthTokenResponse tokens = adminInvitationService.acceptInvitation(request, tenantId);
        return ResponseEntity.ok(ApiResponse.success(tokens, "Account activated successfully with 2FA enabled."));
    }
}
