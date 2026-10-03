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

import com.examplatform.identity.dto.ActiveSessionResponse;
import com.examplatform.identity.dto.AdminActivityLogResponse;
import com.examplatform.identity.dto.AdminPermissionDetailResponse;
import com.examplatform.identity.dto.AdminUpdateUserRequest;
import com.examplatform.identity.dto.ChangePasswordRequest;
import com.examplatform.identity.dto.CreatePersonalAccessTokenRequest;
import com.examplatform.identity.dto.PersonalAccessTokenResponse;
import com.examplatform.identity.dto.TotpSetupResponse;
import com.examplatform.identity.dto.TotpVerifySetupRequest;
import com.examplatform.identity.dto.UserAccountResponse;
import com.examplatform.identity.exception.AccountNotFoundException;
import com.examplatform.identity.service.AdminActivityService;
import com.examplatform.identity.service.AdminInvitationService;
import com.examplatform.identity.service.AuthenticationService;
import com.examplatform.identity.service.PersonalAccessTokenService;
import com.examplatform.identity.service.RoleManagementService;
import com.examplatform.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dedicated REST controller for Administrator Profile Management (Issue #154).
 * Supports profile metadata, localization preferences, credential management,
 * TOTP 2FA, session invalidation, Personal Access Tokens (PATs), and audit history.
 */
@Slf4j
@RestController
@RequestMapping({"/api/v1/admin/me", "/api/v1/identity/admin/me"})
@RequiredArgsConstructor
public class AdminProfileController {

    private final RoleManagementService roleManagementService;
    private final AuthenticationService authenticationService;
    private final AdminInvitationService adminInvitationService;
    private final PersonalAccessTokenService tokenService;
    private final AdminActivityService activityService;

    /**
     * 1. Retrieve Current Admin Profile & Permissions
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserAccountResponse>> getProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        String userIdStr = extractUserId(jwt);
        UserAccountResponse profile = roleManagementService.getUserProfile(userIdStr, tenantId);
        return ResponseEntity.ok(ApiResponse.success(profile, "Admin profile retrieved successfully."));
    }

    /**
     * 2. Update Personal Profile & Localization Preferences
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserAccountResponse>> updateProfile(
            @Valid @RequestBody AdminUpdateUserRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        String userIdStr = extractUserId(jwt);
        UserAccountResponse updated = roleManagementService.updateUserProfile(userIdStr, request, tenantId);
        return ResponseEntity.ok(ApiResponse.success(updated, "Admin profile updated successfully."));
    }

    /**
     * 3. Change Account Password
     */
    @RequestMapping(
            value = "/security/password",
            method = {RequestMethod.POST, RequestMethod.PUT}
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        String userIdStr = extractUserId(jwt);
        authenticationService.changePassword(userIdStr, request.getCurrentPassword(), request.getNewPassword(), tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully."));
    }

    /**
     * 4. Multi-Factor Authentication: Setup TOTP
     */
    @PostMapping("/security/mfa/setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TotpSetupResponse>> setupMfa(
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "admin";
        if (username == null || username.isBlank()) {
            username = extractUserId(jwt);
        }
        TotpSetupResponse setup = adminInvitationService.generateTotpSetup(username);
        return ResponseEntity.ok(ApiResponse.success(setup, "MFA TOTP credentials generated."));
    }

    /**
     * 5. Multi-Factor Authentication: Verify and Enable TOTP
     */
    @PostMapping({"/security/mfa/verify", "/security/mfa/verify-setup"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> verifyMfa(
            @Valid @RequestBody TotpVerifySetupRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        String userIdStr = extractUserId(jwt);
        UUID userId = resolveUuid(userIdStr, tenantId);
        adminInvitationService.verifyAndEnableTotp(userId, request, tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "2FA TOTP activated successfully."));
    }

    /**
     * 6. Multi-Factor Authentication: Check Status
     */
    @GetMapping("/security/mfa/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMfaStatus(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        String userIdStr = extractUserId(jwt);
        UUID userId = resolveUuid(userIdStr, tenantId);
        boolean mfaActive = adminInvitationService.isTotpEnabled(userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("mfaEnabled", mfaActive, "userId", userId.toString())));
    }

    /**
     * 7. Multi-Factor Authentication: Disable MFA
     */
    @PostMapping("/security/mfa/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> disableMfa(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        String userIdStr = extractUserId(jwt);
        UUID userId = resolveUuid(userIdStr, tenantId);
        adminInvitationService.disableTotp(userId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "2FA TOTP has been disabled."));
    }

    /**
     * 8. Active Sessions: List
     */
    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ActiveSessionResponse>>> getSessions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        String userIdStr = extractUserId(jwt);
        List<ActiveSessionResponse> sessions = roleManagementService.getActiveSessions(userIdStr, tenantId);
        return ResponseEntity.ok(ApiResponse.success(sessions, "Active sessions retrieved successfully."));
    }

    /**
     * 9. Active Sessions: Terminate Single Session
     */
    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        String userIdStr = extractUserId(jwt);
        roleManagementService.revokeSession(userIdStr, sessionId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "Session terminated successfully."));
    }

    /**
     * 10. Active Sessions: Revoke All Other Sessions
     */
    @DeleteMapping("/sessions/other")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> revokeOtherSessions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        String userIdStr = extractUserId(jwt);
        roleManagementService.revokeOtherSessions(userIdStr, tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "Other active sessions terminated successfully."));
    }

    /**
     * 11. Personal Access Tokens: List
     */
    @GetMapping("/tokens")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PersonalAccessTokenResponse>>> listTokens(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        String userIdStr = extractUserId(jwt);
        UUID userId = resolveUuid(userIdStr, tenantId);
        List<PersonalAccessTokenResponse> tokens = tokenService.listTokens(userId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(tokens, "Personal access tokens retrieved successfully."));
    }

    /**
     * 12. Personal Access Tokens: Create
     */
    @PostMapping("/tokens")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PersonalAccessTokenResponse>> createToken(
            @Valid @RequestBody CreatePersonalAccessTokenRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        String userIdStr = extractUserId(jwt);
        UUID userId = resolveUuid(userIdStr, tenantId);
        PersonalAccessTokenResponse token = tokenService.createToken(userId, request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(token, "Personal access token created successfully. Store secret securely."));
    }

    /**
     * 13. Personal Access Tokens: Revoke
     */
    @DeleteMapping("/tokens/{tokenId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> revokeToken(
            @PathVariable UUID tokenId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        String userIdStr = extractUserId(jwt);
        UUID userId = resolveUuid(userIdStr, tenantId);
        tokenService.revokeToken(tokenId, userId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "Personal access token revoked successfully."));
    }

    /**
     * 14. Activity History & Audit Trail: List
     */
    @GetMapping("/activity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AdminActivityLogResponse>>> getActivity(
            @RequestParam(required = false, defaultValue = "ALL") String category,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        String userIdStr = extractUserId(jwt);
        UUID userId = resolveUuid(userIdStr, tenantId);
        List<AdminActivityLogResponse> activity = activityService.getActivityLogs(userId, category, page, size, tenantId);
        return ResponseEntity.ok(ApiResponse.success(activity, "Admin activity logs retrieved successfully."));
    }

    /**
     * 15. Activity History & Audit Trail: Export (CSV or JSON)
     */
    @GetMapping("/activity/export")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> exportActivity(
            @RequestParam(required = false, defaultValue = "csv") String format,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        String userIdStr = extractUserId(jwt);
        UUID userId = resolveUuid(userIdStr, tenantId);
        String exportedContent = activityService.exportActivityLogs(userId, format, tenantId);

        String contentType = "csv".equalsIgnoreCase(format) ? "text/csv" : "application/json";
        String filename = "admin-activity-" + userId + "." + ("csv".equalsIgnoreCase(format) ? "csv" : "json");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(exportedContent);
    }

    /**
     * 16. Granular RBAC & Effective Permissions Inspector
     */
    @GetMapping("/permissions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AdminPermissionDetailResponse>>> getPermissions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        String userIdStr = extractUserId(jwt);
        UUID userId = resolveUuid(userIdStr, tenantId);
        List<AdminPermissionDetailResponse> perms = activityService.getEffectivePermissions(userId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(perms, "Effective permissions retrieved successfully."));
    }

    private String extractUserId(Jwt jwt) {
        return jwt != null ? jwt.getSubject() : "user-unknown";
    }

    private UUID resolveUuid(String userIdStr, String tenantId) {
        try {
            return UUID.fromString(userIdStr.trim());
        } catch (Exception e) {
            UserAccountResponse profile = roleManagementService.getUserProfile(userIdStr, tenantId);
            if (profile != null && profile.getId() != null) {
                return profile.getId();
            }
            return UUID.randomUUID();
        }
    }
}
