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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AccountLockoutService}.
 *
 * <p><strong>Validates: Requirements 2.4, 2.6</strong>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountLockoutService")
class AccountLockoutServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private AppSecurityProperties securityProperties;

    @InjectMocks
    private AccountLockoutService accountLockoutService;

    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String TENANT_ID = "default";

    private UserAccount buildAccount(int failedAttempts, LocalDateTime lastFailedAt) {
        UserAccount account = UserAccount.builder()
                .username("testuser")
                .emailHash("hash123")
                .mobileHash("mobile123")
                .accountStatus(AccountStatus.ACTIVE)
                .failedAttemptCount(failedAttempts)
                .lastFailedAt(lastFailedAt)
                .build();
        account.setTenantId(TENANT_ID);
        ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
        return account;
    }

    @BeforeEach
    void setUp() {
        when(securityProperties.getMaxFailedAttempts()).thenReturn(5);
        when(securityProperties.getLockoutWindowSeconds()).thenReturn(600);
    }

    @Nested
    @DisplayName("checkAndLockIfNeeded — threshold reached within window")
    class LockoutTriggered {

        @Test
        @DisplayName("locks account when failedAttemptCount >= 5 and lastFailedAt within window")
        void locksAccountWhenThresholdReachedWithinWindow() {
            LocalDateTime recentFailure = LocalDateTime.now().minusMinutes(2);
            UserAccount account = buildAccount(5, recentFailure);
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());

            boolean result = accountLockoutService.checkAndLockIfNeeded(account, TENANT_ID);

            assertThat(result).isTrue();
            assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.LOCKED);
            assertThat(account.getLockedAt()).isNotNull();
            verify(userAccountRepository).save(account);
        }

        @Test
        @DisplayName("publishes audit event on lockout")
        void publishesAuditEventOnLockout() {
            LocalDateTime recentFailure = LocalDateTime.now().minusMinutes(1);
            UserAccount account = buildAccount(6, recentFailure);
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());

            accountLockoutService.checkAndLockIfNeeded(account, TENANT_ID);

            verify(auditEventPublisher).publish(
                    eq(AuditEventType.ACCOUNT_LOCK),
                    eq(ACCOUNT_ID.toString()),
                    eq("identity:lockout"),
                    any(),
                    any(),
                    any(Map.class)
            );
        }

        @Test
        @DisplayName("publishes Kafka notification on lockout")
        @SuppressWarnings("unchecked")
        void publishesKafkaNotificationOnLockout() {
            LocalDateTime recentFailure = LocalDateTime.now().minusSeconds(30);
            UserAccount account = buildAccount(5, recentFailure);
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());

            accountLockoutService.checkAndLockIfNeeded(account, TENANT_ID);

            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            verify(kafkaTemplate).send(
                    eq("exam.notifications.outbound"),
                    eq(ACCOUNT_ID.toString()),
                    captor.capture()
            );
            Map<String, Object> notification = captor.getValue();
            assertThat(notification.get("eventType")).isEqualTo("ACCOUNT_LOCKED");
            assertThat(notification.get("userId")).isEqualTo(ACCOUNT_ID.toString());
            assertThat(notification.get("tenantId")).isEqualTo(TENANT_ID);
            assertThat(notification.get("message")).isEqualTo(
                    "Your account has been locked due to multiple failed login attempts.");
        }
    }

    @Nested
    @DisplayName("checkAndLockIfNeeded — threshold NOT reached")
    class LockoutNotTriggered {

        @Test
        @DisplayName("does NOT lock when failedAttemptCount < 5")
        void doesNotLockWhenBelowThreshold() {
            LocalDateTime recentFailure = LocalDateTime.now().minusMinutes(1);
            UserAccount account = buildAccount(4, recentFailure);

            boolean result = accountLockoutService.checkAndLockIfNeeded(account, TENANT_ID);

            assertThat(result).isFalse();
            assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
            verify(userAccountRepository, never()).save(any());
        }

        @Test
        @DisplayName("does NOT lock when lastFailedAt is outside the 10-minute window")
        void doesNotLockWhenOutsideWindow() {
            LocalDateTime oldFailure = LocalDateTime.now().minusMinutes(15);
            UserAccount account = buildAccount(5, oldFailure);

            boolean result = accountLockoutService.checkAndLockIfNeeded(account, TENANT_ID);

            assertThat(result).isFalse();
            assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
            verify(userAccountRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }
    }
}
