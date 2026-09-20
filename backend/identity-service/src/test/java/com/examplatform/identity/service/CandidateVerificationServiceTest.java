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
import com.examplatform.identity.exception.InvalidOtpException;
import com.examplatform.identity.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateVerificationServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private OtpService otpService;
    @Mock
    private Msg91SmsService msg91SmsService;
    @Mock
    private KeycloakService keycloakService;
    @Mock
    private AuditEventPublisher auditEventPublisher;

    private SmsProperties smsProperties;
    private CandidateVerificationService verificationService;

    @BeforeEach
    void setUp() {
        smsProperties = new SmsProperties();
        smsProperties.setMobileVerificationRequired(false);

        verificationService = new CandidateVerificationService(
                userAccountRepository,
                otpService,
                msg91SmsService,
                keycloakService,
                auditEventPublisher,
                smsProperties
        );
    }

    @Test
    @DisplayName("Should verify email OTP and activate account directly when mobile verification is optional")
    void shouldVerifyEmailAndActivateAccountDirectly() {
        UUID userId = UUID.randomUUID();
        UserAccount account = UserAccount.builder()
                .username("candidate@example.com")
                .emailHash("emailHash123")
                .mobileHash("mobileHash123")
                .emailVerified(false)
                .mobileVerified(false)
                .accountStatus(AccountStatus.PENDING_VERIFICATION)
                .build();
        account.setTenantId("default");
        ReflectionTestUtils.setField(account, "id", userId);

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(account));
        when(otpService.verifyEmailOtp(userId, "emailHash123", "123456")).thenReturn(true);
        when(msg91SmsService.getRemainingSmsCount(any(), any())).thenReturn(3);

        EmailVerifyRequest request = new EmailVerifyRequest();
        request.setUserId(userId.toString());
        request.setOtp("123456");

        VerificationStatusResponse response = verificationService.verifyEmailOtp(request, "default");

        assertThat(response.isEmailVerified()).isTrue();
        assertThat(response.isFullyVerified()).isTrue();
        assertThat(response.getAccountStatus()).isEqualTo("ACTIVE");
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(userAccountRepository).save(account);
    }

    @Test
    @DisplayName("Should verify mobile OTP when optional mobile verification is used")
    void shouldVerifyMobileWhenOptional() {
        UUID userId = UUID.randomUUID();
        UserAccount account = UserAccount.builder()
                .username("candidate@example.com")
                .emailHash("emailHash123")
                .mobileHash("mobileHash123")
                .emailVerified(false)
                .mobileVerified(false)
                .accountStatus(AccountStatus.PENDING_VERIFICATION)
                .build();
        account.setTenantId("default");
        ReflectionTestUtils.setField(account, "id", userId);

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(account));
        when(otpService.verifyMobileOtp(userId, "mobileHash123", "654321")).thenReturn(true);
        when(msg91SmsService.getRemainingSmsCount(any(), any())).thenReturn(2);

        MobileVerifyRequest request = new MobileVerifyRequest();
        request.setUserId(userId.toString());
        request.setOtp("654321");

        VerificationStatusResponse response = verificationService.verifyMobileOtp(request, "default");

        assertThat(response.isEmailVerified()).isFalse();
        assertThat(response.isMobileVerified()).isTrue();
        assertThat(response.isFullyVerified()).isFalse();
        verify(userAccountRepository).save(account);
    }

    @Test
    @DisplayName("Should throw InvalidOtpException when OTP is incorrect")
    void shouldThrowInvalidOtpException() {
        UUID userId = UUID.randomUUID();
        UserAccount account = UserAccount.builder()
                .emailHash("emailHash123")
                .emailVerified(false)
                .build();
        account.setTenantId("default");
        ReflectionTestUtils.setField(account, "id", userId);

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(account));
        when(otpService.verifyEmailOtp(userId, "emailHash123", "000000")).thenReturn(false);

        EmailVerifyRequest request = new EmailVerifyRequest();
        request.setUserId(userId.toString());
        request.setOtp("000000");

        assertThatThrownBy(() -> verificationService.verifyEmailOtp(request, "default"))
                .isInstanceOf(InvalidOtpException.class);
    }
}
