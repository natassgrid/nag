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
import com.examplatform.identity.dto.ChangePasswordRequest;
import com.examplatform.identity.dto.EmailVerifyRequest;
import com.examplatform.identity.dto.MobileVerifyRequest;
import com.examplatform.identity.dto.OtpResendRequest;
import com.examplatform.identity.dto.OtpVerifyRequest;
import com.examplatform.identity.dto.RefreshTokenRequest;
import com.examplatform.identity.dto.RegistrationRequest;
import com.examplatform.identity.dto.RegistrationResponse;
import com.examplatform.identity.dto.ReviewerResponse;
import com.examplatform.identity.dto.TotpSetupResponse;
import com.examplatform.identity.dto.TotpVerifySetupRequest;
import com.examplatform.identity.dto.UserAccountResponse;
import com.examplatform.identity.dto.VerificationStatusResponse;
import com.examplatform.identity.dto.WebAuthnAssertionRequest;
import com.examplatform.identity.exception.AccountNotFoundException;
import com.examplatform.identity.service.AdminInvitationService;
import com.examplatform.identity.service.AuthenticationService;
import com.examplatform.identity.service.CandidateVerificationService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the Identity Service.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/identity")
@RequiredArgsConstructor
public class IdentityController {

    private final RegistrationService registrationService;
    private final OtpVerificationService otpVerificationService;
    private final CandidateVerificationService candidateVerificationService;
    private final AdminInvitationService adminInvitationService;
    private final AuthenticationService authenticationService;
    private final WebAuthnService webAuthnService;
    private final RoleManagementService roleManagementService;

    /**
     * List all user accounts for the given tenant (admin only).
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
     * List reviewers and SMEs matching a given subject and tenant.
     */
    @GetMapping("/reviewers")
    public ResponseEntity<ApiResponse<List<ReviewerResponse>>> getReviewers(
            @RequestParam(required = false) String subject,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        log.debug("Get reviewers request received for subject [{}], tenant [{}]", subject, tenantId);
        List<ReviewerResponse> reviewers = roleManagementService.findReviewers(subject, tenantId);
        return ResponseEntity.ok(ApiResponse.success(reviewers, "Reviewers retrieved successfully."));
    }

    /**
     * Initiate candidate registration.
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
                .body(ApiResponse.success(response, "Registration initiated. Verification OTP sent to email and mobile."));
    }

    /**
     * Verify Email OTP for candidate.
     */
    @PostMapping({"/verify/email", "/auth/verify/email"})
    public ResponseEntity<ApiResponse<VerificationStatusResponse>> verifyEmail(
            @Valid @RequestBody EmailVerifyRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        log.debug("Candidate email OTP verification request for userId [{}], tenant [{}]", request.getUserId(), tenantId);
        VerificationStatusResponse response = candidateVerificationService.verifyEmailOtp(request, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Email verified successfully."));
    }

    /**
     * Verify Mobile OTP for candidate.
     */
    @PostMapping({"/verify/mobile", "/auth/verify/mobile"})
    public ResponseEntity<ApiResponse<VerificationStatusResponse>> verifyMobile(
            @Valid @RequestBody MobileVerifyRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        log.debug("Candidate mobile OTP verification request for userId [{}], tenant [{}]", request.getUserId(), tenantId);
        VerificationStatusResponse response = candidateVerificationService.verifyMobileOtp(request, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Mobile number verified successfully."));
    }

    /**
     * Get candidate verification status and remaining SMS count.
     */
    @GetMapping("/verification-status")
    public ResponseEntity<ApiResponse<VerificationStatusResponse>> getVerificationStatus(
            @RequestParam String userId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        UUID uid;
        try {
            uid = UUID.fromString(userId.trim());
        } catch (IllegalArgumentException e) {
            throw new AccountNotFoundException("Invalid user ID format: " + userId);
        }
        VerificationStatusResponse response = candidateVerificationService.getVerificationStatus(uid, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Verification status retrieved successfully."));
    }

    /**
     * Resend candidate Email OTP.
     */
    @PostMapping("/resend/email-otp")
    public ResponseEntity<ApiResponse<Void>> resendEmailOtp(
            @Valid @RequestBody OtpResendRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        UUID userId;
        try {
            userId = UUID.fromString(request.getUserId().trim());
        } catch (IllegalArgumentException e) {
            throw new AccountNotFoundException("Invalid user ID format: " + request.getUserId());
        }
        registrationService.resendEmailOtp(userId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "Email OTP resent successfully."));
    }

    /**
     * Resend candidate SMS OTP.
     */
    @PostMapping("/resend/sms-otp")
    public ResponseEntity<ApiResponse<Void>> resendSmsOtp(
            @Valid @RequestBody OtpResendRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        UUID userId;
        try {
            userId = UUID.fromString(request.getUserId().trim());
        } catch (IllegalArgumentException e) {
            throw new AccountNotFoundException("Invalid user ID format: " + request.getUserId());
        }
        registrationService.resendSmsOtp(userId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "SMS OTP resent successfully."));
    }

    /**
     * Verify OTP and activate the pending account (backward-compatible).
     */
    @PostMapping({"/otp/verify", "/verify-otp"})
    public ResponseEntity<ApiResponse<AuthTokenResponse>> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        log.debug("OTP verification request received, tenant [{}]", tenantId);
        AuthTokenResponse tokens = otpVerificationService.verifyOtpAndActivate(request, tenantId);
        return ResponseEntity.ok(ApiResponse.success(tokens, "Account activated successfully."));
    }

    /**
     * Resend OTP (backward-compatible).
     */
    @PostMapping("/otp/resend")
    public ResponseEntity<ApiResponse<Void>> resendOtp(
            @Valid @RequestBody OtpResendRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        log.info("OTP resend request for userId [{}], tenant [{}]", request.getUserId(), tenantId);
        UUID userId;
        try {
            userId = UUID.fromString(request.getUserId().trim());
        } catch (IllegalArgumentException e) {
            throw new AccountNotFoundException("Invalid user ID format: " + request.getUserId());
        }
        registrationService.resendOtp(userId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "OTP resent successfully."));
    }

    /**
     * Authenticate with username/password and optional MFA OTP.
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
     * Refresh JWT access token using a valid refresh token.
     */
    @PostMapping({"/auth/token/refresh", "/auth/refresh"})
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            HttpServletRequest servletRequest) {
        log.debug("Token refresh request received, tenant [{}]", tenantId);
        String ipAddress = servletRequest.getRemoteAddr();
        AuthTokenResponse response = authenticationService.refreshToken(request, tenantId, ipAddress);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully."));
    }

    /**
     * Authenticate using WebAuthn / FIDO2 assertion.
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
     * Initiate TOTP 2FA setup (returns secret, otpauth URI, backup codes).
     */
    @PostMapping("/auth/2fa/setup")
    public ResponseEntity<ApiResponse<TotpSetupResponse>> setup2fa(
            @RequestParam(required = false) String username,
            @AuthenticationPrincipal Jwt jwt) {
        String targetUsername = (jwt != null) ? jwt.getClaimAsString("preferred_username") : username;
        if (targetUsername == null || targetUsername.isBlank()) {
            targetUsername = (jwt != null) ? jwt.getSubject() : "admin-user";
        }
        TotpSetupResponse setup = adminInvitationService.generateTotpSetup(targetUsername);
        return ResponseEntity.ok(ApiResponse.success(setup, "TOTP 2FA setup credentials generated."));
    }

    /**
     * Verify and activate TOTP 2FA setup for user.
     */
    @PostMapping("/auth/2fa/verify-setup")
    public ResponseEntity<ApiResponse<Void>> verify2faSetup(
            @Valid @RequestBody TotpVerifySetupRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        String userIdStr = (jwt != null) ? jwt.getSubject() : request.getUserId();
        if (userIdStr == null || userIdStr.isBlank()) {
            throw new AccountNotFoundException("User ID is required to enable 2FA.");
        }
        UUID userId = UUID.fromString(userIdStr.trim());
        adminInvitationService.verifyAndEnableTotp(userId, request, tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "2FA TOTP configured and activated successfully."));
    }

    /**
     * Change password for the authenticated user.
     */
    @RequestMapping(
            value = "/auth/change-password",
            method = {RequestMethod.POST, RequestMethod.PUT}
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {

        String userId = jwt.getSubject();
        log.info("Change password request for user [{}], tenant [{}]", userId, tenantId);

        authenticationService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword(), tenantId);

        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully."));
    }

    /**
     * Logout: revoke active sessions and refresh tokens.
     */
    @DeleteMapping("/auth/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {

        String userId = jwt.getSubject();
        log.info("Logout request for user [{}], tenant [{}]", userId, tenantId);

        authenticationService.logout(userId, tenantId);

        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully."));
    }
}
