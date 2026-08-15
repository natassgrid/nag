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

package com.examplatform.identity.service;

import com.examplatform.identity.config.AppSecurityProperties;
import com.examplatform.identity.domain.ActiveSession;
import com.examplatform.identity.domain.UserAccount;
import com.examplatform.identity.domain.enums.AccountStatus;
import com.examplatform.identity.dto.AuthTokenRequest;
import com.examplatform.identity.dto.AuthTokenResponse;
import com.examplatform.identity.exception.AuthenticationException;
import com.examplatform.identity.exception.MfaRequiredException;
import com.examplatform.identity.repository.ActiveSessionRepository;
import com.examplatform.identity.repository.UserAccountRepository;
import com.examplatform.shared.audit.AuditEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Handles password + MFA authentication with device binding and
 * single concurrent session enforcement.
 *
 * <p><strong>Validates: Requirements 2.1, 2.2, 2.5, 2.7</strong>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationService {

    private final UserAccountRepository userAccountRepository;
    private final ActiveSessionRepository activeSessionRepository;
    private final HashingService hashingService;
    private final KeycloakService keycloakService;
    private final OtpService otpService;
    private final AuditEventPublisher auditEventPublisher;
    private final AppSecurityProperties appSecurityProperties;
    private final AccountLockoutService accountLockoutService;
    private final RiskAssessmentService riskAssessmentService;

    /**
     * Authenticate a user with username/password and optional MFA OTP.
     *
     * @param request   authentication credentials (username, password, optional OTP, optional device FP)
     * @param tenantId  the tenant the request belongs to
     * @param ipAddress the originating client IP address
     * @return JWT access and refresh tokens on success
     * @throws AuthenticationException if any authentication check fails
     * @throws MfaRequiredException    if MFA is required but OTP was not supplied
     */
    public AuthTokenResponse authenticate(AuthTokenRequest request, String tenantId, String ipAddress) {

        // 1. Find account by email hash
        String emailHash = hashingService.sha256(request.getUsername().toLowerCase().trim());
        UserAccount account = userAccountRepository
                .findByEmailHashAndTenantId(emailHash, tenantId)
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        // 2. Check account status
        AccountStatus status = account.getAccountStatus();
        switch (status) {
            case LOCKED ->
                throw new AuthenticationException("Account is locked. Please contact support.");
            case DEACTIVATED ->
                throw new AuthenticationException("Account has been deactivated.");
            case PENDING_VERIFICATION ->
                throw new AuthenticationException("Account not yet verified. Please complete OTP verification.");
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
            log.warn("Failed login for user [{}] tenant [{}]: {}", account.getId(), tenantId, ex.getMessage());
            // Check if lockout threshold reached
            accountLockoutService.checkAndLockIfNeeded(account, tenantId);
            throw new AuthenticationException("Invalid credentials");
        }

        // Reset failed attempt counter on successful Keycloak auth
        account.setFailedAttemptCount(0);
        account.setLastFailedAt(null);

        // 3b. Step-up authentication on risk signal (new device / unusual time)
        if (appSecurityProperties.isMfaEnabled() && !account.isMfaEnabled()) {
            boolean stepUpRequired = riskAssessmentService.isStepUpRequired(
                    account, request.getDeviceFingerprint(), ipAddress, LocalDateTime.now());
            if (stepUpRequired) {
                String otpCode = request.getOtpCode();
                if (otpCode == null || otpCode.isBlank()) {
                    // Save reset of failed attempts before throwing
                    userAccountRepository.save(account);
                    throw new MfaRequiredException("Step-up authentication required. Please provide OTP code.");
                }
                // Verify the step-up OTP
                String mobileHash = hashingService.sha256(request.getUsername().toLowerCase().trim());
                boolean otpValid = otpService.verifyOtp(mobileHash, otpCode);
                if (!otpValid) {
                    throw new AuthenticationException("Invalid MFA code.");
                }
            }
        }

        // 4. Per-account MFA enforcement.
        // Enforced whenever the account has MFA enabled, regardless of the global
        // appSecurityProperties.isMfaEnabled() flag. The global flag only controls
        // whether the platform mandates MFA for all accounts (step-up, step 3b).
        // An account that has voluntarily enrolled MFA must always present an OTP.
        if (account.isMfaEnabled()) {
            String otpCode = request.getOtpCode();
            if (otpCode == null || otpCode.isBlank()) {
                throw new MfaRequiredException("MFA required. Please provide OTP code.");
            }
            String mobileHash = hashingService.sha256(request.getUsername().toLowerCase().trim());
            boolean otpValid = otpService.verifyOtp(mobileHash, otpCode);
            if (!otpValid) {
                throw new AuthenticationException("Invalid MFA code.");
            }
        }

        // 5. Device binding enforcement
        String requestedFingerprint = request.getDeviceFingerprint();
        String storedFingerprint = account.getDeviceFingerprint();
        if (storedFingerprint != null && !storedFingerprint.isBlank()) {
            if (requestedFingerprint != null && !requestedFingerprint.equals(storedFingerprint)) {
                log.warn("Device fingerprint mismatch for user [{}] tenant [{}] ip [{}]",
                        account.getId(), tenantId, ipAddress);
                publishAuditEventAsync(
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

        // Save account updates (failed attempt reset + optional device FP binding)
        userAccountRepository.save(account);

        // 6. Single concurrent session enforcement — new login wins
        if (activeSessionRepository.existsByUserIdAndTenantId(account.getId(), tenantId)) {
            log.info("Invalidating existing session for user [{}] tenant [{}]", account.getId(), tenantId);
            activeSessionRepository.deleteByUserIdAndTenantId(account.getId(), tenantId);
        }

        // 7. Create new session record
        String sessionToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(appSecurityProperties.getSessionIdleTimeoutSeconds());

        ActiveSession session = ActiveSession.builder()
                .userId(account.getId())
                .sessionToken(sessionToken)
                .deviceFp(requestedFingerprint)
                .ipAddress(ipAddress)
                .expiresAt(expiresAt)
                .build();
        session.setTenantId(tenantId);
        activeSessionRepository.save(session);

        // 8. Publish LOGIN audit event asynchronously
        publishAuditEventAsync(
                AuditEventType.LOGIN,
                account.getId().toString(),
                tenantId,
                ipAddress,
                requestedFingerprint
        );

        // 9. Return tokens obtained from Keycloak
        return tokens;
    }

    @Async
    public void publishAuditEventAsync(AuditEventType type, String actorId, String tenantId,
                                        String ip, String deviceFingerprint) {
        auditEventPublisher.publish(
                type,
                actorId,
                "identity:auth/token",
                ip,
                deviceFingerprint,
                Map.of("tenantId", tenantId)
        );
    }
}
