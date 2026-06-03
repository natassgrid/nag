package com.examplatform.identity.service;

import com.examplatform.identity.domain.UserAccount;
import com.examplatform.identity.domain.enums.AccountStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RiskAssessmentService}.
 *
 * <p><strong>Validates: Requirements 2.6</strong>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskAssessmentService")
class RiskAssessmentServiceTest {

    @InjectMocks
    private RiskAssessmentService riskAssessmentService;

    private static final UUID ACCOUNT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String IP_ADDRESS = "192.168.1.10";

    private UserAccount buildAccount(String storedFingerprint) {
        UserAccount account = UserAccount.builder()
                .username("user@example.com")
                .emailHash("hash")
                .mobileHash("mobile")
                .accountStatus(AccountStatus.ACTIVE)
                .deviceFingerprint(storedFingerprint)
                .build();
        account.setTenantId("default");
        ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
        return account;
    }

    @Nested
    @DisplayName("Device fingerprint risk signal")
    class DeviceFingerprint {

        @Test
        @DisplayName("step-up required when device fingerprint differs from stored")
        void stepUpRequiredWhenFingerprintDiffers() {
            UserAccount account = buildAccount("stored-fp-abc");

            boolean result = riskAssessmentService.isStepUpRequired(
                    account, "new-fp-xyz", IP_ADDRESS, LocalDateTime.of(2024, 1, 15, 10, 0));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("step-up NOT required when fingerprints match")
        void stepUpNotRequiredWhenFingerprintsMatch() {
            UserAccount account = buildAccount("same-fingerprint");

            boolean result = riskAssessmentService.isStepUpRequired(
                    account, "same-fingerprint", IP_ADDRESS, LocalDateTime.of(2024, 1, 15, 10, 0));

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("step-up NOT required when account has no stored fingerprint")
        void stepUpNotRequiredWhenNoStoredFingerprint() {
            UserAccount account = buildAccount(null);

            boolean result = riskAssessmentService.isStepUpRequired(
                    account, "any-fingerprint", IP_ADDRESS, LocalDateTime.of(2024, 1, 15, 10, 0));

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Unusual time risk signal")
    class UnusualTime {

        @Test
        @DisplayName("step-up required at 3:00 AM (unusual hour)")
        void stepUpRequiredAtUnusualHour() {
            UserAccount account = buildAccount(null);

            boolean result = riskAssessmentService.isStepUpRequired(
                    account, null, IP_ADDRESS, LocalDateTime.of(2024, 1, 15, 3, 0));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("step-up NOT required at 10:00 AM (normal hour)")
        void stepUpNotRequiredAtNormalHour() {
            UserAccount account = buildAccount(null);

            boolean result = riskAssessmentService.isStepUpRequired(
                    account, null, IP_ADDRESS, LocalDateTime.of(2024, 1, 15, 10, 0));

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("step-up required at 23:30 (unusual hour)")
        void stepUpRequiredAtLateNight() {
            UserAccount account = buildAccount(null);

            boolean result = riskAssessmentService.isStepUpRequired(
                    account, null, IP_ADDRESS, LocalDateTime.of(2024, 1, 15, 23, 30));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("step-up NOT required at 06:00 (boundary - normal)")
        void stepUpNotRequiredAtEarlyBoundary() {
            UserAccount account = buildAccount(null);

            boolean result = riskAssessmentService.isStepUpRequired(
                    account, null, IP_ADDRESS, LocalDateTime.of(2024, 1, 15, 6, 0));

            assertThat(result).isFalse();
        }
    }
}
