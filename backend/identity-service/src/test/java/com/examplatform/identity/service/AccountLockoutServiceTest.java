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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

    @Mock
    private DynamicConfigService dynamicConfigService;

    @InjectMocks
    private AccountLockoutService accountLockoutService;

    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String TENANT_ID = "default";

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(securityProperties.getMaxFailedAttempts()).thenReturn(5);
        Mockito.lenient().when(securityProperties.getLockoutWindowSeconds()).thenReturn(600);
        Mockito.lenient().when(dynamicConfigService.getInt(eq("auth.max.login.attempts"), anyString(), anyInt()))
                .thenReturn(5);
        Mockito.lenient().when(dynamicConfigService.getInt(eq("auth.lockout.duration.minutes"), anyString(), anyInt()))
                .thenReturn(10);
    }

    private UserAccount buildAccount(int failedAttempts, LocalDateTime lastFailedAt) {
        UserAccount account = UserAccount.builder()
                .username("candidate@test.com")
                .accountStatus(AccountStatus.ACTIVE)
                .failedAttemptCount(failedAttempts)
                .lastFailedAt(lastFailedAt)
                .build();
        account.setTenantId(TENANT_ID);
        ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
        return account;
    }

    // ─────────────────────────────────────────────────────────────
    // checkAndLockIfNeeded
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("checkAndLockIfNeeded")
    class CheckAndLockIfNeeded {

        @Test
        @DisplayName("locks account when failed attempts reach threshold within window")
        void locksAccountWhenThresholdReachedWithinWindow() {
            LocalDateTime recentFailure = LocalDateTime.now().minusSeconds(120); // within 600s
            UserAccount account = buildAccount(5, recentFailure);

            when(kafkaTemplate.send(anyString(), anyString(), any()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            boolean locked = accountLockoutService.checkAndLockIfNeeded(account, TENANT_ID);

            assertThat(locked).isTrue();
            assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.LOCKED);
            assertThat(account.getLockedAt()).isNotNull();
            verify(userAccountRepository).save(account);

            // Verify audit event published
            verify(auditEventPublisher).publish(
                    eq(AuditEventType.ACCOUNT_LOCK),
                    eq(ACCOUNT_ID.toString()),
                    eq("identity:lockout"),
                    eq(null),
                    eq(null),
                    any()
            );

            // Verify Kafka notification sent
            ArgumentCaptor<Map<String, Object>> notifCaptor = ArgumentCaptor.forClass(Map.class);
            verify(kafkaTemplate).send(eq("exam.notifications.outbound"), eq(ACCOUNT_ID.toString()), notifCaptor.capture());
            Map<String, Object> notif = notifCaptor.getValue();
            assertThat(notif.get("eventType")).isEqualTo("ACCOUNT_LOCKED");
            assertThat(notif.get("userId")).isEqualTo(ACCOUNT_ID.toString());
        }

        @Test
        @DisplayName("does NOT lock account when failed attempts are below threshold")
        void doesNotLockWhenBelowThreshold() {
            LocalDateTime recentFailure = LocalDateTime.now().minusSeconds(60);
            UserAccount account = buildAccount(4, recentFailure); // 4 < 5

            boolean locked = accountLockoutService.checkAndLockIfNeeded(account, TENANT_ID);

            assertThat(locked).isFalse();
            assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
            verify(userAccountRepository, never()).save(any());
            verify(auditEventPublisher, never()).publish(any(), any(), any(), any(), any(), any());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("does NOT lock account when last failure was outside lockout window")
        void doesNotLockWhenOutsideWindow() {
            LocalDateTime oldFailure = LocalDateTime.now().minusSeconds(700); // outside 600s window
            UserAccount account = buildAccount(5, oldFailure);

            boolean locked = accountLockoutService.checkAndLockIfNeeded(account, TENANT_ID);

            assertThat(locked).isFalse();
            assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
            verify(userAccountRepository, never()).save(any());
            verify(auditEventPublisher, never()).publish(any(), any(), any(), any(), any(), any());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("does NOT lock account when lastFailedAt is null")
        void doesNotLockWhenLastFailedAtIsNull() {
            UserAccount account = buildAccount(5, null);

            boolean locked = accountLockoutService.checkAndLockIfNeeded(account, TENANT_ID);

            assertThat(locked).isFalse();
            assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
            verify(userAccountRepository, never()).save(any());
        }
    }
}
