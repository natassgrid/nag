/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU标识 Affero General Public License as published
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

package com.examplatform.identity.service;

import com.examplatform.identity.config.AppSecurityProperties;
import com.examplatform.identity.domain.ActiveSession;
import com.examplatform.identity.domain.UserAccount;
import com.examplatform.identity.domain.UserRoleAssignment;
import com.examplatform.identity.domain.enums.AccountStatus;
import com.examplatform.identity.domain.enums.UserRole;
import com.examplatform.identity.dto.AuthTokenRequest;
import com.examplatform.identity.dto.AuthTokenResponse;
import com.examplatform.identity.dto.RefreshTokenRequest;
import com.examplatform.identity.exception.AccountNotFoundException;
import com.examplatform.identity.exception.AccountNotVerifiedException;
import com.examplatform.identity.exception.AuthenticationException;
import com.examplatform.identity.exception.MfaRequiredException;
import com.examplatform.identity.repository.ActiveSessionRepository;
import com.examplatform.identity.repository.UserAccountRepository;
import com.examplatform.identity.repository.UserRoleAssignmentRepository;
import com.examplatform.shared.audit.AuditEventType;
import com.examplatform.shared.config.DynamicConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles password + MFA authentication with device binding and
 * single concurrent session enforcement.
 *
 * <p><strong>Validates: Requirements 2.1, 2.2, 2.5, 2.7, Multi-Tenancy Hardening (Issue #133)</strong></p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationService {

    private final UserAccountRepository userAccountRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final ActiveSessionRepository activeSessionRepository;
    private final KeycloakService keycloakService;
    private final HashingService hashingService;
    private final OtpService otpService;
    private final TotpService totpService;
    private final RiskAssessmentService riskAssessmentService;
    private final AccountLockoutService accountLockoutService;
    private final DynamicConfigService dynamicConfigService;
    private final AuditEventPublisher auditEventPublisher;
    private final AppSecurityProperties appSecurityProperties;

    /**
     * Authenticate a user with username/password and optional MFA OTP / TOTP 2FA.
     *
     * @param request   authentication credentials (username, password, optional OTP, optional device FP)
     * @param tenantId  the tenant the request belongs to
     * @param ipAddress the originating client IP address
     * @return JWT access and refresh tokens on success
     * @throws AuthenticationException if any authentication check fails
     * @throws MfaRequiredException    if MFA is required but OTP was not supplied
     */
    public AuthTokenResponse authenticate(AuthTokenRequest request, String tenantId, String ipAddress) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";

        // 1. Find account by email hash, mobile hash, or username strictly within tenant
        String rawInput = request.getUsername().trim();
        String hash = hashingService.sha256(rawInput.toLowerCase());
        UserAccount account = userAccountRepository
                .findByEmailHashAndTenantId(hash, effectiveTenant)
                .or(() -> userAccountRepository.findByMobileHashAndTenantId(hash, effectiveTenant))
                .or(() -> userAccountRepository.findByUsernameIgnoreCaseAndTenantId(rawInput, effectiveTenant))
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        // 2. Check account status
        AccountStatus status = account.getAccountStatus();
        switch (status) {
            case LOCKED ->
                throw new AuthenticationException("Account is locked. Please contact support.");
            case DEACTIVATED ->
                throw new AuthenticationException("Account has been deactivated.");
            case PENDING_VERIFICATION ->
                throw new AccountNotVerifiedException(
                    "Account not yet verified. Please complete verification.",
                    account.getId(),
                    account.getUsername()
                );
            case PENDING_SETUP ->
                throw new AuthenticationException("Account setup is pending. Please use your email invitation link.");
            case ACTIVE -> { /* proceed */ }
            default ->
                throw new AuthenticationException("Invalid credentials");
        }

        // 3. Validate password via Keycloak
        AuthTokenResponse tokens;
        try {
            tokens = keycloakService.getTokens(request.getUsername(), request.getPassword(), account.getId().toString());
        } catch (Exception ex) {
            account.setFailedAttemptCount(account.getFailedAttemptCount() + 1);
            account.setLastFailedAt(LocalDateTime.now());
            userAccountRepository.save(account);
            log.warn("Failed login for user [{}] tenant [{}]: {}", account.getId(), effectiveTenant, ex.getMessage());
            // Check if lockout threshold reached
            accountLockoutService.checkAndLockIfNeeded(account, effectiveTenant);
            throw new AuthenticationException("Invalid credentials");
        }

        // Reset failed attempt counter on successful Keycloak auth
        account.setFailedAttemptCount(0);
        account.setLastFailedAt(null);

        // 3b. Role-Based & Global MFA Enforcement Policy
        List<UserRoleAssignment> roles = userRoleAssignmentRepository.findByUserIdAndTenantId(account.getId(), effectiveTenant);
        boolean isAdminUser = roles != null && roles.stream().anyMatch(r -> r.getRole() != UserRole.CANDIDATE);

        String adminPolicy = dynamicConfigService.getString("auth.mfa.admin.policy", effectiveTenant, "OPTIONAL");
        String candidatePolicy = dynamicConfigService.getString("auth.mfa.candidate.policy", effectiveTenant, "OPTIONAL");
        boolean globalMfaEnforced = dynamicConfigService.getBoolean(
                "auth.mfa.enforced", effectiveTenant, appSecurityProperties != null && appSecurityProperties.isMfaEnabled());

        boolean mfaRequiredForUser;
        if (globalMfaEnforced) {
            mfaRequiredForUser = true;
        } else if (isAdminUser) {
            if ("ENFORCED".equalsIgnoreCase(adminPolicy)) {
                mfaRequiredForUser = true;
            } else if ("DISABLED".equalsIgnoreCase(adminPolicy)) {
                mfaRequiredForUser = false;
            } else { // OPTIONAL
                mfaRequiredForUser = account.isMfaEnabled() || account.getTotpSecret() != null;
            }
        } else { // Candidate
            if ("ENFORCED".equalsIgnoreCase(candidatePolicy)) {
                mfaRequiredForUser = true;
            } else if ("DISABLED".equalsIgnoreCase(candidatePolicy)) {
                mfaRequiredForUser = false;
            } else { // OPTIONAL
                mfaRequiredForUser = account.isMfaEnabled() || account.getTotpSecret() != null;
            }
        }

        if (mfaRequiredForUser) {
            String otpCode = request.getOtpCode();
            if (otpCode == null || otpCode.isBlank()) {
                if (account.getTotpSecret() == null) {
                    String mobileHash = account.getMobileHash() != null ? account.getMobileHash() : hashingService.sha256(request.getUsername().toLowerCase().trim());
                    otpService.sendOtp(account.getId(), mobileHash, null);
                }
                throw new MfaRequiredException("2FA / MFA required. Please provide your authenticator OTP code.");
            }

            boolean mfaValid = false;

            // 1. Check TOTP Secret if configured
            if (account.getTotpSecret() != null && !account.getTotpSecret().isBlank()) {
                mfaValid = totpService.verifyTotpCode(account.getTotpSecret(), otpCode);
                // 2. Check Backup codes
                if (!mfaValid && account.getBackupCodes() != null) {
                    String remainingBackupCodes = totpService.validateAndConsumeBackupCode(otpCode, account.getBackupCodes());
                    if (remainingBackupCodes != null) {
                        mfaValid = true;
                        account.setBackupCodes(remainingBackupCodes);
                        log.info("Backup recovery code used for user [{}]", account.getId());
                    }
                }
            }

            // 3. Fallback to SMS OTP if TOTP was not matched
            if (!mfaValid) {
                String mobileHash = account.getMobileHash() != null ? account.getMobileHash() : hashingService.sha256(request.getUsername().toLowerCase().trim());
                mfaValid = otpService.verifyOtp(mobileHash, otpCode);
            }

            if (!mfaValid) {
                throw new AuthenticationException("Invalid MFA code / 2FA verification failed.");
            }
        } else {
            // Risk-based step-up evaluation only when step-up is explicitly enabled in config
            boolean stepUpEnabled = dynamicConfigService.getBoolean("auth.stepup.enforced", effectiveTenant, false);
            if (stepUpEnabled && riskAssessmentService != null && riskAssessmentService.isStepUpRequired(
                    account, request.getDeviceFingerprint(), ipAddress, LocalDateTime.now())) {
                String otpCode = request.getOtpCode();
                String mobileHash = account.getMobileHash() != null ? account.getMobileHash() : hashingService.sha256(request.getUsername().toLowerCase().trim());
                if (otpCode == null || otpCode.isBlank()) {
                    userAccountRepository.save(account);
                    otpService.sendOtp(account.getId(), mobileHash, null);
                    throw new MfaRequiredException("Step-up authentication required. Please provide OTP code.");
                }
                boolean otpValid = otpService.verifyOtp(mobileHash, otpCode);
                if (!otpValid) {
                    throw new AuthenticationException("Invalid MFA code / 2FA verification failed.");
                }
            }
        }

        // 5. Device binding enforcement
        String requestedFingerprint = request.getDeviceFingerprint();
        String storedFingerprint = account.getDeviceFingerprint();
        if (storedFingerprint != null && !storedFingerprint.isBlank()) {
            if (requestedFingerprint != null && !requestedFingerprint.equals(storedFingerprint)) {
                log.warn("Device fingerprint mismatch for user [{}] tenant [{}] ip [{}]",
                        account.getId(), effectiveTenant, ipAddress);
                publishAuditEventAsync(
                        AuditEventType.DENIED_ACCESS,
                        account.getId().toString(),
                        effectiveTenant,
                        ipAddress,
                        requestedFingerprint
                );
                throw new AuthenticationException("Device not recognised.");
            }
        } else if (requestedFingerprint != null && !requestedFingerprint.isBlank()) {
            // Bind device fingerprint on first login that provides one
            account.setDeviceFingerprint(requestedFingerprint);
            log.info("Device fingerprint bound for user [{}] tenant [{}]", account.getId(), effectiveTenant);
        }

        // Save account updates (failed attempt reset + optional device FP binding)
        userAccountRepository.save(account);

        // 6. Single concurrent session enforcement — new login wins
        if (activeSessionRepository.existsByUserIdAndTenantId(account.getId(), effectiveTenant)) {
            log.info("Invalidating existing session for user [{}] tenant [{}]", account.getId(), effectiveTenant);
            activeSessionRepository.deleteByUserIdAndTenantId(account.getId(), effectiveTenant);
        }

        // 7. Create new session record with dynamic timeout
        int timeoutMinutes = dynamicConfigService.getInt(
                "auth.session.timeout.minutes",
                effectiveTenant,
                appSecurityProperties != null ? Math.max(1, (int) (appSecurityProperties.getSessionIdleTimeoutSeconds() / 60)) : 30
        );
        String sessionToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(timeoutMinutes);

        ActiveSession session = ActiveSession.builder()
                .userId(account.getId())
                .sessionToken(sessionToken)
                .deviceFp(requestedFingerprint)
                .ipAddress(ipAddress)
                .expiresAt(expiresAt)
                .build();
        session.setTenantId(effectiveTenant);
        activeSessionRepository.save(session);

        // 8. Publish LOGIN audit event asynchronously
        publishAuditEventAsync(
                AuditEventType.LOGIN,
                account.getId().toString(),
                effectiveTenant,
                ipAddress,
                requestedFingerprint
        );

        // 9. Return tokens obtained from Keycloak
        return tokens;
    }

    /**
     * Refresh JWT access token using a valid refresh token.
     *
     * @param request   the refresh token payload
     * @param tenantId  the tenant the request belongs to
     * @param ipAddress the originating client IP address
     * @return new JWT tokens on success
     * @throws AuthenticationException if the refresh token is invalid or expired
     */
    public AuthTokenResponse refreshToken(RefreshTokenRequest request, String tenantId, String ipAddress) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        log.debug("Refreshing token for tenant [{}]", effectiveTenant);
        try {
            AuthTokenResponse tokens = keycloakService.refreshToken(request.getRefreshToken());
            if (tokens != null && tokens.getUserId() != null) {
                try {
                    UUID userId = UUID.fromString(tokens.getUserId());
                    UserAccount account = userAccountRepository.findByIdAndTenantId(userId, effectiveTenant)
                            .or(() -> userAccountRepository.findById(userId))
                            .orElseThrow(() -> new AuthenticationException("Account not found in tenant: " + effectiveTenant));
                    if (account.getAccountStatus() != AccountStatus.ACTIVE) {
                        throw new AuthenticationException("Account is not active");
                    }
                    if (account.getTenantId() != null && !account.getTenantId().equals(effectiveTenant)) {
                        throw new AuthenticationException("Invalid tenant");
                    }
                    activeSessionRepository.findByUserIdAndTenantId(userId, effectiveTenant).ifPresent(session -> {
                        session.setExpiresAt(LocalDateTime.now().plusMinutes(
                                dynamicConfigService.getInt("auth.session.timeout.minutes", effectiveTenant, 30)));
                        activeSessionRepository.save(session);
                    });
                } catch (IllegalArgumentException ignored) {
                }
            }
            return tokens;
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Token refresh failed for tenant [{}]: {}", effectiveTenant, ex.getMessage());
            throw new AuthenticationException("Invalid or expired refresh token");
        }
    }

    /**
     * Terminate active session and log out user.
     */
    public void logout(String userIdStr, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        try {
            UUID userId = UUID.fromString(userIdStr);
            activeSessionRepository.deleteByUserIdAndTenantId(userId, effectiveTenant);
            publishAuditEventAsync(
                    AuditEventType.LOGOUT,
                    userIdStr,
                    effectiveTenant,
                    null,
                    null
            );
            log.info("User [{}] logged out successfully from tenant [{}]", userIdStr, effectiveTenant);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid userId format during logout: {}", userIdStr);
        }
    }

    /**
     * Change password for the user.
     */
    public void changePassword(String userIdStr, String currentPassword, String newPassword, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            throw new AccountNotFoundException("Invalid user ID: " + userIdStr);
        }
        UserAccount account = userAccountRepository.findByIdAndTenantId(userId, effectiveTenant)
                .or(() -> userAccountRepository.findById(userId))
                .orElseThrow(() -> new AccountNotFoundException("User not found: " + userId + " in tenant: " + effectiveTenant));

        // Update password in Keycloak
        if (account.getKeycloakUserId() != null) {
            keycloakService.resetPassword(account.getKeycloakUserId(), newPassword);
        }

        publishAuditEventAsync(
                AuditEventType.ROLE_CHANGE,
                account.getId().toString(),
                effectiveTenant,
                null,
                null
        );
        log.info("Password changed successfully for user [{}] in tenant [{}]", userId, effectiveTenant);
    }

    @Async
    protected void publishAuditEventAsync(AuditEventType eventType, String actorId,
                                           String tenantId, String ipAddress, String deviceFingerprint) {
        try {
            auditEventPublisher.publish(
                    eventType,
                    actorId,
                    "identity:auth/token",
                    ipAddress,
                    deviceFingerprint,
                    Map.of("tenantId", tenantId != null ? tenantId : "default")
            );
        } catch (Exception ex) {
            log.warn("Failed to publish audit event {}: {}", eventType, ex.getMessage());
        }
    }
}
