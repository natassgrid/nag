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

import com.examplatform.identity.config.SmsProperties;
import com.examplatform.identity.domain.UserAccount;
import com.examplatform.identity.domain.enums.AccountStatus;
import com.examplatform.identity.dto.EmailVerifyRequest;
import com.examplatform.identity.dto.MobileVerifyRequest;
import com.examplatform.identity.dto.VerificationStatusResponse;
import com.examplatform.identity.exception.AccountNotFoundException;
import com.examplatform.identity.exception.InvalidOtpException;
import com.examplatform.identity.repository.UserAccountRepository;
import com.examplatform.shared.audit.AuditEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateVerificationService {

    private final UserAccountRepository userAccountRepository;
    private final OtpService otpService;
    private final Msg91SmsService msg91SmsService;
    private final KeycloakService keycloakService;
    private final AuditEventPublisher auditEventPublisher;
    private final SmsProperties smsProperties;

    /**
     * Verifies candidate's email OTP.
     * Marks emailVerified = true. If mobile verification is optional or mobile is already verified,
     * activates account and enables the user.
     */
    @Transactional
    public VerificationStatusResponse verifyEmailOtp(EmailVerifyRequest request, String tenantId) {
        UUID userId = parseUserId(request.getUserId());
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for user: " + userId));

        if (!tenantId.equals(account.getTenantId())) {
            throw new AccountNotFoundException("Account not found in this tenant.");
        }

        boolean valid = otpService.verifyEmailOtp(userId, account.getEmailHash(), request.getOtp());
        if (!valid) {
            throw new InvalidOtpException("Invalid or expired email verification OTP.");
        }

        account.setEmailVerified(true);

        checkAndActivateAccount(account, tenantId, "email-otp-verified");

        userAccountRepository.save(account);

        return buildStatusResponse(account);
    }

    /**
     * Verifies candidate's mobile OTP.
     * Marks mobileVerified = true. If emailVerified is also true, activates account.
     */
    @Transactional
    public VerificationStatusResponse verifyMobileOtp(MobileVerifyRequest request, String tenantId) {
        UUID userId = parseUserId(request.getUserId());
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for user: " + userId));

        if (!tenantId.equals(account.getTenantId())) {
            throw new AccountNotFoundException("Account not found in this tenant.");
        }

        boolean valid = otpService.verifyMobileOtp(userId, account.getMobileHash(), request.getOtp());
        if (!valid) {
            throw new InvalidOtpException("Invalid or expired mobile verification OTP.");
        }

        account.setMobileVerified(true);

        checkAndActivateAccount(account, tenantId, "mobile-otp-verified");

        userAccountRepository.save(account);

        return buildStatusResponse(account);
    }

    /**
     * Gets the full candidate verification status and weekly SMS remaining count.
     */
    @Transactional(readOnly = true)
    public VerificationStatusResponse getVerificationStatus(UUID userId, String tenantId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for user: " + userId));

        if (!tenantId.equals(account.getTenantId())) {
            throw new AccountNotFoundException("Account not found in this tenant.");
        }

        return buildStatusResponse(account);
    }

    private void checkAndActivateAccount(UserAccount account, String tenantId, String eventSource) {
        boolean mobileRequired = smsProperties.isMobileVerificationRequired();
        boolean canActivate = account.isEmailVerified() && (!mobileRequired || account.isMobileVerified());

        if (canActivate) {
            account.setAccountStatus(AccountStatus.ACTIVE);
            keycloakService.activateUser(account.getKeycloakUserId());
            log.info("Candidate account {} verified and activated (mobileRequired: {})", account.getId(), mobileRequired);

            auditEventPublisher.publish(
                    AuditEventType.LOGIN,
                    account.getId().toString(),
                    "identity:candidate-verified",
                    null, null,
                    Map.of("tenantId", tenantId, "source", eventSource)
            );
        }
    }

    private VerificationStatusResponse buildStatusResponse(UserAccount account) {
        int smsRemaining = msg91SmsService.getRemainingSmsCount(account.getId(), account.getMobileHash());
        var nextSmsAt = msg91SmsService.getNextSmsAvailableAt(account.getId(), account.getMobileHash());
        boolean mobileRequired = smsProperties.isMobileVerificationRequired();
        boolean fullyVerified = account.isEmailVerified() && (!mobileRequired || account.isMobileVerified());

        return VerificationStatusResponse.builder()
                .userId(account.getId().toString())
                .emailVerified(account.isEmailVerified())
                .mobileVerified(account.isMobileVerified())
                .accountStatus(account.getAccountStatus().name())
                .smsRemainingThisWeek(smsRemaining)
                .nextSmsAvailableAt(nextSmsAt)
                .fullyVerified(fullyVerified)
                .build();
    }

    private UUID parseUserId(String userIdStr) {
        try {
            return UUID.fromString(userIdStr.trim());
        } catch (IllegalArgumentException e) {
            throw new AccountNotFoundException("Invalid user ID format: " + userIdStr);
        }
    }
}
