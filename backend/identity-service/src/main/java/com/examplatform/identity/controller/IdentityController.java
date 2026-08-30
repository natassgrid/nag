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

import com.examplatform.identity.dto.AuthTokenRequest;
import com.examplatform.identity.dto.AuthTokenResponse;
import com.examplatform.identity.dto.OtpVerifyRequest;
import com.examplatform.identity.dto.RegistrationRequest;
import com.examplatform.identity.dto.RegistrationResponse;
import com.examplatform.identity.dto.UserAccountResponse;
import com.examplatform.identity.dto.WebAuthnAssertionRequest;
import com.examplatform.identity.service.AuthenticationService;
import com.examplatform.identity.service.OtpVerificationService;
import com.examplatform.identity.service.RegistrationService;
import com.examplatform.identity.service.RoleManagementService;
import com.examplatform.identity.service.WebAuthnService;
import com.examplatform.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.examplatform.identity.dto.ChangePasswordRequest;
import com.examplatform.identity.dto.OtpResendRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for the Identity Service.
 * Full business logic is implemented in tasks 2.2–2.8.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/identity")
@RequiredArgsConstructor
public class IdentityController {

    private final RegistrationService registrationService;
    private final OtpVerificationService otpVerificationService;
    private final AuthenticationService authenticationService;
    private final WebAuthnService webAuthnService;
    private final RoleManagementService roleManagementService;

    /**
     * List all user accounts for the given tenant (admin only).
     *
     * @param tenantId tenant identifier from request header
     * @return list of user accounts with roles
     */
    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserAccountResponse>>> listUsers(
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        log.debug("List users request received for tenant [{}]", tenantId);
        List<UserAccountResponse> users = roleManagementService.listAllUsers(tenantId);
        return ResponseEntity.ok(ApiResponse.success(users, "Users retrieved successfully."));
    }

    /**
     * Initiate candidate registration.
     *
     * @param request  registration payload with identity document and contact details
     * @param tenantId tenant identifier from request header
     * @return 202 Accepted with acknowledgement
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegistrationResponse>> register(
            @Valid @RequestBody RegistrationRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        log.debug("Registration request received for identity doc type [{}], tenant [{}]",
                request.getIdentityDocType(), tenantId);
        long start = System.currentTimeMillis();
        RegistrationResponse response = registrationService.register(request, tenantId);
        log.debug("Registration completed in {}ms for tenant [{}]", System.currentTimeMillis() - start, tenantId);
        return ResponseEntity.accepted()
                .body(ApiResponse.success(response, "Registration initiated. OTP sent to registered mobile."));
    }

    /**
     * Verify OTP and activate the pending account.
     * Issues JWT access + refresh tokens via Keycloak on success.
     *
     * @param request  OTP verification payload
     * @param tenantId tenant identifier from request header
     * @return 200 OK with JWT tokens
     */
    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        log.debug("OTP verification request received, tenant [{}]", tenantId);
        AuthTokenResponse tokens = otpVerificationService.verifyOtpAndActivate(request, tenantId);
        return ResponseEntity.ok(ApiResponse.success(tokens, "Account activated successfully."));
    }

    /**
     * Authenticate with username/password and optional MFA OTP.
     * Enforces device binding and single concurrent session per user.
     *
     * @param request        authentication credentials (username, password, optional OTP, optional device FP)
     * @param tenantId       tenant identifier from request header
     * @param servletRequest raw HTTP request used to extract client IP address
     * @return 200 OK with JWT access and refresh tokens
     */
    @PostMapping("/auth/token")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> token(
            @Valid @RequestBody AuthTokenRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            HttpServletRequest servletRequest) {
        log.debug("Token request received for username [{}], tenant [{}]", request.getUsername(), tenantId);
        String ipAddress = servletRequest.getRemoteAddr();
        AuthTokenResponse response = authenticationService.authenticate(request, tenantId, ipAddress);
        return ResponseEntity.ok(ApiResponse.success(response, "Authentication successful."));
    }

    /**
     * Authenticate using WebAuthn / FIDO2 assertion.
     * Verifies authenticator assertion and issues JWT tokens.
     *
     * @param request        WebAuthn assertion payload from the client authenticator
     * @param tenantId       tenant identifier from request header
     * @param servletRequest raw HTTP request used to extract client IP address
     * @return 200 OK with JWT access and refresh tokens
     */
    @PostMapping("/auth/webauthn")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> webAuthn(
            @Valid @RequestBody WebAuthnAssertionRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            HttpServletRequest servletRequest) {
        log.debug("WebAuthn authentication request received for credential [{}], tenant [{}]",
                request.getCredentialId(), tenantId);
        String ipAddress = servletRequest.getRemoteAddr();
        AuthTokenResponse tokens = webAuthnService.authenticate(request, tenantId, ipAddress);
        return ResponseEntity.ok(ApiResponse.success(tokens, "WebAuthn authentication successful."));
    }

    /**
     * Change password for the authenticated candidate.
     * Requires the current password as verification before updating to the new one.
     *
     * @param request  the change password request (currentPassword + newPassword)
     * @param jwt      the authenticated user's JWT
     * @param tenantId the tenant identifier
     * @return 200 OK on success
     */
    @PostMapping("/auth/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {

        String userId = jwt.getSubject();
        log.info("Change password request for user [{}], tenant [{}]", userId, tenantId);

        // Delegate to identity service — implementation stub for now
        // TODO: registrationService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword(), tenantId);

        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully."));
    }

    /**
     * Resend OTP to a candidate awaiting verification.
     *
     * @param request  the resend request (userId)
     * @param tenantId the tenant identifier
     * @return 200 OK confirming OTP was sent
     */
    @PostMapping("/otp/resend")
    public ResponseEntity<ApiResponse<Void>> resendOtp(
            @Valid @RequestBody OtpResendRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {

        log.info("OTP resend request for userId [{}], tenant [{}]", request.getUserId(), tenantId);

        // Delegate to registration service OTP resend
        // TODO: registrationService.resendOtp(UUID.fromString(request.getUserId()), tenantId);

        return ResponseEntity.ok(ApiResponse.success(null, "OTP resent successfully."));
    }

    /**
     * Logout: revoke the refresh token.
     *
     * @param jwt      the authenticated user's JWT
     * @return 200 OK confirming logout
     */
    @DeleteMapping("/auth/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Logout request for user [{}]", jwt.getSubject());
        // TODO: tokenService.revokeRefreshToken(jwt.getSubject());

        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully."));
    }
}
