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
import com.examplatform.identity.domain.UserAccount;
import com.examplatform.identity.domain.enums.AccountStatus;
import com.examplatform.identity.dto.RegistrationRequest;
import com.examplatform.identity.dto.RegistrationResponse;
import com.examplatform.identity.exception.DuplicateIdentityException;
import com.examplatform.identity.repository.UserAccountRepository;
import com.examplatform.shared.audit.AuditEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final String HMAC_KEY_PREFIX = "identity-doc-hmac-";

    private final UserAccountRepository userAccountRepository;
    private final HashingService hashingService;
    private final OtpService otpService;
    private final AuditEventPublisher auditEventPublisher;
    private final AppSecurityProperties securityProperties;

    /**
     * Registers a new candidate account. Must complete within 2 seconds.
     * Audit event is published asynchronously to stay within SLA.
     */
    @Transactional
    public RegistrationResponse register(RegistrationRequest request, String tenantId) {
        long start = System.currentTimeMillis();

        // 1. Hash sensitive fields
        String emailHash = hashingService.sha256(request.getEmail().toLowerCase().trim());
        String mobileHash = hashingService.sha256(request.getMobile().trim());
        String docHash = hashingService.sha256(request.getIdentityDocNumber().trim().toUpperCase());
        String docHmac = hashingService.hmac(request.getIdentityDocNumber().trim().toUpperCase(),
            HMAC_KEY_PREFIX + tenantId);

        // 2. Duplicate checks
        if (userAccountRepository.existsByEmailHashAndTenantId(emailHash, tenantId)) {
            throw new DuplicateIdentityException(
                "An account with this email address already exists.");
        }
        if (userAccountRepository.existsByIdentityDocHashAndTenantId(docHash, tenantId)) {
            throw new DuplicateIdentityException(
                "An account with this identity document already exists.");
        }

        // 3. Persist account in PENDING_VERIFICATION state
        // Note: tenantId is on BaseEntity and not in Lombok @Builder — set explicitly after build
        UserAccount account = UserAccount.builder()
            .username(request.getEmail().toLowerCase().trim())
            .emailHash(emailHash)
            .mobileHash(mobileHash)
            .identityDocType(request.getIdentityDocType())
            .identityDocHash(docHash)
            .identityDocHmac(docHmac)
            .accountStatus(AccountStatus.PENDING_VERIFICATION)
            .mfaEnabled(false)
            .failedAttemptCount(0)
            .build();
        account.setTenantId(tenantId);

        UserAccount saved = userAccountRepository.save(account);

        // 4. Send OTP (synchronous — required for 2-second response)
        otpService.sendOtp(saved.getId(), mobileHash, request.getMobile());

        // 5. Publish audit event asynchronously to avoid blocking
        publishAuditEventAsync(saved.getId().toString(), tenantId);

        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > 1500) {
            log.warn("Registration for tenant {} took {}ms — approaching 2s SLA", tenantId, elapsed);
        }

        return RegistrationResponse.builder()
            .message("Registration successful. OTP sent to registered mobile number.")
            .userId(saved.getId().toString())
            .build();
    }

    @Async
    public void publishAuditEventAsync(String userId, String tenantId) {
        auditEventPublisher.publish(
            AuditEventType.CANDIDATE_PROFILE_CREATED,
            userId, "identity:registration", null, null,
            Map.of("tenantId", tenantId)
        );
    }
}
