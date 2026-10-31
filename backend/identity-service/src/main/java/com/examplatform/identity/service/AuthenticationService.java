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
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.crypto.HashingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
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

    private static final String DEFAULT_MFA_POLICY = "OPTIONAL";

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
        return authenticate(request, tenantId, ipAddress, LocalDateTime.now(ZoneId.systemDefault()));
    }

    /**
     * Authenticate a user with username/password and optional MFA OTP / TOTP 2FA at a specific login timestamp.
     *
     * @param request   authentication credentials (username, password, optional OTP, optional device FP)
     * @param tenantId  the tenant the request belongs to
     * @param ipAddress the originating client IP address
     * @param loginTime the timestamp of the login attempt (used for time-of-day risk assessment)
     * @return JWT access and refresh tokens on success
     * @throws AuthenticationException if any authentication check fails
     * @throws MfaRequiredException    if MFA is required but OTP was not supplied
     */
    public AuthTokenResponse authenticate(AuthTokenRequest request, String tenantId, String ipAddress, LocalDateTime loginTime) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : SharedConstants.DEFAULT_TENANT;

        // 1. Find account by email hash, mobile hash, or username strictly within tenant
        UserAccount account = findAccount(request.getUsername().trim(), effectiveTenant);

        // 2. Check account status
        validateAccountStatus(account);

        // 3. Validate password via Keycloak
        AuthTokenResponse tokens = authenticateWithKeycloak(account, request.getUsername(), request.getPassword(), effectiveTenant);

        // Reset failed attempt counter on successful Keycloak auth
        account.setFailedAttemptCount(0);
        account.setLastFailedAt(null);

        // 4. Role-based & Step-up MFA verification
        validateMfaOrStepUp(account, request, ipAddress, loginTime, effectiveTenant);

        // 5. Device binding enforcement
        validateAndBindDevice(account, request.getDeviceFingerprint(), effectiveTenant, ipAddress);

        // Save account updates (failed attempt reset + optional device FP binding)
        userAccountRepository.save(account);

        // 6. Single concurrent session enforcement — new login wins
        enforceSingleSession(account.getId(), effectiveTenant);

        // 7. Create new session record with dynamic timeout
        createSession(account.getId(), request.getDeviceFingerprint(), ipAddress, effectiveTenant);

        // 8. Record last login IP in Redis and publish LOGIN audit event asynchronously
        recordLoginSuccess(account.getId(), ipAddress, request.getDeviceFingerprint(), effectiveTenant);

        // 9. Return tokens obtained from Keycloak
        return tokens;
    }

    private UserAccount findAccount(String rawInput, String tenantId) {
        String hash = hashingService.sha256(rawInput.toLowerCase());
        return userAccountRepository
                .findByEmailHashAndTenantId(hash, tenantId)
                .or(() -> userAccountRepository.findByMobileHashAndTenantId(hash, tenantId))
                .or(() -> userAccountRepository.findByUsernameIgnoreCaseAndTenantId(rawInput, tenantId))
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));
    }

    private void validateAccountStatus(UserAccount account) {
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
    }

    private AuthTokenResponse authenticateWithKeycloak(UserAccount account, String username, String password, String tenantId) {
        try {
            return keycloakService.getTokens(username, password, account.getId().toString());
        } catch (Exception ex) {
            account.setFailedAttemptCount(account.getFailedAttemptCount() + 1);
            account.setLastFailedAt(LocalDateTime.now(ZoneId.systemDefault()));
            userAccountRepository.save(account);
            log.warn("Failed login for user [{}] tenant [{}]: {}", account.getId(), tenantId, ex.getMessage());
            accountLockoutService.checkAndLockIfNeeded(account, tenantId);
            throw new AuthenticationException("Invalid credentials");
        }
    }

    private void validateMfaOrStepUp(UserAccount account, AuthTokenRequest request,
                                     String ipAddress, LocalDateTime loginTime, String tenantId) {
        if (isMfaRequired(account, tenantId)) {
            verifyMfa(account, request);
        } else {
            verifyStepUpAuth(account, request, ipAddress, loginTime, tenantId);
        }
    }

    private boolean isMfaRequired(UserAccount account, String tenantId) {
        boolean globalMfaEnforced = dynamicConfigService.getBoolean(
                "auth.mfa.enforced", tenantId, appSecurityProperties != null && appSecurityProperties.isMfaEnabled());
        if (globalMfaEnforced) {
            return true;
        }

        List<UserRoleAssignment> roles = userRoleAssignmentRepository.findByUserIdAndTenantId(account.getId(), tenantId);
        boolean isAdminUser = roles != null && roles.stream().anyMatch(r -> r.getRole() != UserRole.CANDIDATE);
        String policyKey = isAdminUser ? "auth.mfa.admin.policy" : "auth.mfa.candidate.policy";
        String policy = dynamicConfigService.getString(policyKey, tenantId, DEFAULT_MFA_POLICY);

        if ("ENFORCED".equalsIgnoreCase(policy)) {
            return true;
        }
        if ("DISABLED".equalsIgnoreCase(policy)) {
            return false;
        }
        return account.isMfaEnabled() || account.getTotpSecret() != null;
    }

    private void verifyMfa(UserAccount account, AuthTokenRequest request) {
        String otpCode = request.getOtpCode();
        if (otpCode == null || otpCode.isBlank()) {
            if (account.getTotpSecret() == null) {
                String mobileHash = resolveMobileHash(account, request.getUsername());
                otpService.sendOtp(account.getId(), mobileHash, null);
            }
            throw new MfaRequiredException("2FA / MFA required. Please provide your authenticator OTP code.");
        }

        boolean mfaValid = verifyTotpOrBackup(account, otpCode);
        if (!mfaValid) {
            String mobileHash = resolveMobileHash(account, request.getUsername());
            mfaValid = otpService.verifyOtp(mobileHash, otpCode);
        }

        if (!mfaValid) {
            throw new AuthenticationException("Invalid MFA code / 2FA verification failed.");
        }
    }

    private boolean verifyTotpOrBackup(UserAccount account, String otpCode) {
        if (account.getTotpSecret() == null || account.getTotpSecret().isBlank()) {
            return false;
        }
        boolean mfaValid = totpService.verifyTotpCode(account.getTotpSecret(), otpCode);
        if (!mfaValid && account.getBackupCodes() != null) {
            String remainingBackupCodes = totpService.validateAndConsumeBackupCode(otpCode, account.getBackupCodes());
            if (remainingBackupCodes != null) {
                account.setBackupCodes(remainingBackupCodes);
                log.info("Backup recovery code used for user [{}]", account.getId());
                return true;
            }
        }
        return mfaValid;
    }

    private String resolveMobileHash(UserAccount account, String username) {
        return account.getMobileHash() != null
                ? account.getMobileHash()
                : hashingService.sha256(username.toLowerCase().trim());
    }

    private void verifyStepUpAuth(UserAccount account, AuthTokenRequest request,
                                  String ipAddress, LocalDateTime loginTime, String tenantId) {
        boolean stepUpEnabled = dynamicConfigService.getBoolean("auth.stepup.enforced", tenantId, false);
        LocalDateTime effectiveLoginTime = loginTime != null ? loginTime : LocalDateTime.now(ZoneId.systemDefault());
        if (!stepUpEnabled || riskAssessmentService == null || !riskAssessmentService.isStepUpRequired(
                account, request.getDeviceFingerprint(), ipAddress, effectiveLoginTime)) {
            return;
        }

        String otpCode = request.getOtpCode();
        String mobileHash = resolveMobileHash(account, request.getUsername());
        if (otpCode == null || otpCode.isBlank()) {
            userAccountRepository.save(account);
            otpService.sendOtp(account.getId(), mobileHash, null);
            throw new MfaRequiredException("Step-up authentication required. Please provide OTP code.");
        }
        if (!otpService.verifyOtp(mobileHash, otpCode)) {
            throw new AuthenticationException("Invalid MFA code / 2FA verification failed.");
        }
    }

    private void validateAndBindDevice(UserAccount account, String requestedFingerprint, String tenantId, String ipAddress) {
        String storedFingerprint = account.getDeviceFingerprint();
        if (storedFingerprint != null && !storedFingerprint.isBlank()) {
            if (requestedFingerprint != null && !requestedFingerprint.equals(storedFingerprint)) {
                log.warn("Device fingerprint mismatch for user [{}] tenant [{}] ip [{}]",
                        account.getId(), tenantId, ipAddress);
                publishAuditEvent(
                        AuditEventType.DENIED_ACCESS,
                        account.getId().toString(),
                        tenantId,
                        ipAddress,
                        requestedFingerprint
                );
                throw new AuthenticationException("Device not recognised.");
            }
        } else if (requestedFingerprint != null && !requestedFingerprint.isBlank()) {
            // Bind device fingerprint on first login that provides one
            account.setDeviceFingerprint(requestedFingerprint);
            log.info("Device fingerprint bound for user [{}] tenant [{}]", account.getId(), tenantId);
        }
    }

    private void enforceSingleSession(UUID userId, String tenantId) {
        if (activeSessionRepository.existsByUserIdAndTenantId(userId, tenantId)) {
            log.info("Invalidating existing session for user [{}] tenant [{}]", userId, tenantId);
            activeSessionRepository.deleteByUserIdAndTenantId(userId, tenantId);
        }
    }

    private void createSession(UUID userId, String requestedFingerprint, String ipAddress, String tenantId) {
        int timeoutMinutes = dynamicConfigService.getInt(
                "auth.session.timeout.minutes",
                tenantId,
                appSecurityProperties != null ? Math.max(1, appSecurityProperties.getSessionIdleTimeoutSeconds() / 60) : 30
        );
        String sessionToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(timeoutMinutes);

        ActiveSession session = ActiveSession.builder()
                .userId(userId)
                .sessionToken(sessionToken)
                .deviceFp(requestedFingerprint)
                .ipAddress(ipAddress)
                .expiresAt(expiresAt)
                .build();
        session.setTenantId(tenantId);
        activeSessionRepository.save(session);
    }

    private void recordLoginSuccess(UUID userId, String ipAddress, String requestedFingerprint, String tenantId) {
        if (riskAssessmentService != null && ipAddress != null) {
            riskAssessmentService.recordSuccessfulLoginIp(userId, ipAddress, tenantId);
        }
        publishAuditEvent(
                AuditEventType.LOGIN,
                userId.toString(),
                tenantId,
                ipAddress,
                requestedFingerprint
        );
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
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : SharedConstants.DEFAULT_TENANT;
        log.debug("Refreshing token for tenant [{}]", effectiveTenant);
        try {
            AuthTokenResponse tokens = keycloakService.refreshToken(request.getRefreshToken());
            if (tokens != null && tokens.getUserId() != null) {
                refreshUserSession(tokens.getUserId(), effectiveTenant);
            }
            return tokens;
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Token refresh failed for tenant [{}]: {}", effectiveTenant, ex.getMessage());
            throw new AuthenticationException("Invalid or expired refresh token");
        }
    }

    private void refreshUserSession(String userIdStr, String effectiveTenant) {
        try {
            UUID userId = UUID.fromString(userIdStr);
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
                session.setExpiresAt(LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(
                        dynamicConfigService.getInt("auth.session.timeout.minutes", effectiveTenant, 30)));
                activeSessionRepository.save(session);
            });
        } catch (IllegalArgumentException e) {
            log.debug("Ignore malformed UUID in token subject: {}", e.getMessage());
        }
    }

    /**
     * Terminate active session and log out user.
     */
    public void logout(String userIdStr, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : SharedConstants.DEFAULT_TENANT;
        try {
            UUID userId = UUID.fromString(userIdStr);
            activeSessionRepository.deleteByUserIdAndTenantId(userId, effectiveTenant);
            publishAuditEvent(
                    AuditEventType.LOGOUT,
                    userIdStr,
                    effectiveTenant,
                    null,
                    null
            );
            log.info("User [{}] logged out successfully from tenant [{}]", userIdStr, effectiveTenant);
        } catch (IllegalArgumentException _) {
            log.warn("Invalid userId format during logout: {}", userIdStr);
        }
    }

    /**
     * Change password for the user.
     */
    public void changePassword(String userIdStr, String newPassword, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : SharedConstants.DEFAULT_TENANT;
        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException _) {
            throw new AccountNotFoundException("Invalid user ID: " + userIdStr);
        }
        UserAccount account = userAccountRepository.findByIdAndTenantId(userId, effectiveTenant)
                .or(() -> userAccountRepository.findById(userId))
                .orElseThrow(() -> new AccountNotFoundException("User not found: " + userId + " in tenant: " + effectiveTenant));

        // Update password in Keycloak
        if (account.getKeycloakUserId() != null) {
            keycloakService.resetPassword(account.getKeycloakUserId(), newPassword);
        }

        publishAuditEvent(
                AuditEventType.ROLE_CHANGE,
                account.getId().toString(),
                effectiveTenant,
                null,
                null
        );
        log.info("Password changed successfully for user [{}] in tenant [{}]", userId, effectiveTenant);
    }

    private void publishAuditEvent(AuditEventType eventType, String actorId,
                                   String tenantId, String ipAddress, String deviceFingerprint) {
        try {
            if (auditEventPublisher != null) {
                auditEventPublisher.publishAsync(
                        eventType,
                        actorId,
                        "identity:auth/token",
                        ipAddress,
                        deviceFingerprint,
                        Map.of(SharedConstants.TENANT_ID, tenantId != null ? tenantId : SharedConstants.DEFAULT_TENANT)
                );
            }
        } catch (Exception e) {
            log.warn("Failed to publish audit event async for actorId={}: {}", actorId, e.getMessage());
        }
    }
}
