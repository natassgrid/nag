package com.examplatform.identity.service;

import com.examplatform.identity.domain.UserAccount;
import com.examplatform.identity.domain.enums.AccountStatus;
import com.examplatform.identity.dto.AuthTokenResponse;
import com.examplatform.identity.dto.OtpVerifyRequest;
import com.examplatform.identity.exception.AccountNotFoundException;
import com.examplatform.identity.exception.InvalidOtpException;
import com.examplatform.identity.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OtpVerificationService}.
 *
 * <p><strong>Validates: Requirements 1.2</strong>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OtpVerificationService")
class OtpVerificationServiceTest {

    @Mock
    UserAccountRepository userAccountRepository;
    @Mock
    HashingService hashingService;
    @Mock
    OtpService otpService;
    @Mock
    KeycloakService keycloakService;
    @Mock
    AuditEventPublisher auditEventPublisher;

    @InjectMocks
    OtpVerificationService otpVerificationService;

    @Nested
    @DisplayName("verifyOtpAndActivate")
    class VerifyOtpAndActivate {

        @Test
        @DisplayName("activates account and returns tokens on valid OTP")
        void happyPath() {
            UserAccount account = UserAccount.builder()
                .accountStatus(AccountStatus.PENDING_VERIFICATION)
                .username("user@test.com")
                .build();
            account.setTenantId("default");

            OtpVerifyRequest request = new OtpVerifyRequest("9876543210", "123456");

            when(hashingService.sha256(any())).thenReturn("mobileHash");
            when(userAccountRepository.findByMobileHashAndTenantId("mobileHash", "default"))
                .thenReturn(Optional.of(account));
            when(otpService.verifyOtp("mobileHash", "123456")).thenReturn(true);
            when(keycloakService.getTokens(any(), any())).thenReturn(
                AuthTokenResponse.builder().accessToken("token").expiresIn(900).build());

            AuthTokenResponse result = otpVerificationService.verifyOtpAndActivate(request, "default");

            assertAll(
                () -> assertThat(result.getAccessToken()).isEqualTo("token"),
                () -> assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE),
                () -> verify(userAccountRepository).save(account)
            );
        }

        @Test
        @DisplayName("throws InvalidOtpException on invalid OTP")
        void invalidOtp() {
            UserAccount account = UserAccount.builder()
                .accountStatus(AccountStatus.PENDING_VERIFICATION)
                .username("user@test.com")
                .build();
            account.setTenantId("default");

            OtpVerifyRequest request = new OtpVerifyRequest("9876543210", "000000");

            when(hashingService.sha256(any())).thenReturn("mobileHash");
            when(userAccountRepository.findByMobileHashAndTenantId(any(), any()))
                .thenReturn(Optional.of(account));
            when(otpService.verifyOtp(any(), any())).thenReturn(false);

            assertThatThrownBy(() -> otpVerificationService.verifyOtpAndActivate(request, "default"))
                .isInstanceOf(InvalidOtpException.class);
        }

        @Test
        @DisplayName("throws AccountNotFoundException when no account exists")
        void accountNotFound() {
            OtpVerifyRequest request = new OtpVerifyRequest("9876543210", "123456");
            when(hashingService.sha256(any())).thenReturn("mobileHash");
            when(userAccountRepository.findByMobileHashAndTenantId(any(), any()))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> otpVerificationService.verifyOtpAndActivate(request, "default"))
                .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("throws InvalidOtpException when account already active")
        void alreadyActive() {
            UserAccount account = UserAccount.builder()
                .accountStatus(AccountStatus.ACTIVE)
                .username("user@test.com")
                .build();
            account.setTenantId("default");

            OtpVerifyRequest request = new OtpVerifyRequest("9876543210", "123456");
            when(hashingService.sha256(any())).thenReturn("mobileHash");
            when(userAccountRepository.findByMobileHashAndTenantId(any(), any()))
                .thenReturn(Optional.of(account));

            assertThatThrownBy(() -> otpVerificationService.verifyOtpAndActivate(request, "default"))
                .isInstanceOf(InvalidOtpException.class);
        }
    }
}
