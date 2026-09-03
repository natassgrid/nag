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
import com.examplatform.identity.repository.UserAccountRepository;
import com.examplatform.shared.audit.AuditEventType;
import com.examplatform.shared.config.DynamicConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Manages account lockout logic after consecutive failed authentication attempts.
 *
 * <p>An account is locked when the failed attempt count reaches the configured
 * dynamic threshold ({@code auth.max.login.attempts}, fallback 5) and the most recent failure
 * occurred within the lockout window ({@code auth.lockout.duration.minutes}, fallback 15m).
 *
 * <p>On lockout, a notification event is published to the
 * {@code exam.notifications.outbound} Kafka topic so downstream services can
 * alert the user.
 *
 * <p><strong>Validates: Requirements 2.4, 2.6</strong>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountLockoutService {

    private final UserAccountRepository userAccountRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AppSecurityProperties securityProperties;
    private final DynamicConfigService dynamicConfigService;

    private static final String NOTIFICATIONS_TOPIC = "exam.notifications.outbound";

    /**
     * Checks whether the account should be locked based on failed attempt count
     * and the lockout time window. If threshold is met, locks the account,
     * publishes an audit event, and sends a notification to Kafka.
     *
     * @param account  the user account to evaluate
     * @param tenantId the tenant context
     * @return {@code true} if the account was locked by this call; {@code false} otherwise
     */
    public boolean checkAndLockIfNeeded(UserAccount account, String tenantId) {
        int maxAttempts = dynamicConfigService.getInt(
                "auth.max.login.attempts", tenantId, securityProperties.getMaxFailedAttempts());
        int lockoutMinutes = dynamicConfigService.getInt(
                "auth.lockout.duration.minutes", tenantId, securityProperties.getLockoutWindowSeconds() / 60);
        int windowSeconds = lockoutMinutes * 60;

        if (account.getFailedAttemptCount() >= maxAttempts) {
            LocalDateTime windowStart = LocalDateTime.now().minusSeconds(windowSeconds);
            if (account.getLastFailedAt() != null && account.getLastFailedAt().isAfter(windowStart)) {
                // Lock the account
                account.setAccountStatus(AccountStatus.LOCKED);
                account.setLockedAt(LocalDateTime.now());
                userAccountRepository.save(account);

                log.warn("SECURITY: Account {} locked after {} failed attempts within {}s window (tenant: {})",
                        account.getId(), account.getFailedAttemptCount(), windowSeconds, tenantId);

                // Publish audit event
                auditEventPublisher.publish(
                        AuditEventType.ACCOUNT_LOCK,
                        String.valueOf(account.getId()),
                        "identity:lockout",
                        null,
                        null,
                        Map.of(
                                "tenantId", tenantId,
                                "failedAttempts", account.getFailedAttemptCount(),
                                "maxAttempts", maxAttempts,
                                "lockoutMinutes", lockoutMinutes
                        )
                );

                // Publish notification event for alerting user
                Map<String, Object> notification = Map.of(
                        "eventType", "ACCOUNT_LOCKED",
                        "userId", String.valueOf(account.getId()),
                        "tenantId", tenantId,
                        "lockedAt", account.getLockedAt().toString(),
                        "message", "Your account has been locked due to multiple failed login attempts."
                );
                kafkaTemplate.send(NOTIFICATIONS_TOPIC, String.valueOf(account.getId()), notification);

                return true;
            }
        }
        return false;
    }
}
