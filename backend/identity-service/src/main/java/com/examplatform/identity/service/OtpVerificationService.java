package com.examplatform.identity.service;

import com.examplatform.identity.domain.UserAccount;
import com.examplatform.identity.domain.enums.AccountStatus;
import com.examplatform.identity.dto.AuthTokenResponse;
import com.examplatform.identity.dto.OtpVerifyRequest;
import com.examplatform.identity.exception.AccountNotFoundException;
import com.examplatform.identity.exception.InvalidOtpException;
import com.examplatform.identity.repository.UserAccountRepository;
import com.examplatform.shared.audit.AuditEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpVerificationService {

    private final UserAccountRepository userAccountRepository;
    private final HashingService hashingService;
    private final OtpService otpService;
    private final KeycloakService keycloakService;
    private final AuditEventPublisher auditEventPublisher;

    /**
     * Full OTP verification and account activation flow:
     * 1. Hash mobile → find PENDING_VERIFICATION account
     * 2. Verify OTP (throws InvalidOtpException on failure)
     * 3. Set account status to ACTIVE
     * 4. Activate user in Keycloak
     * 5. Issue JWT tokens
     * 6. Publish LOGIN audit event
     */
    @Transactional
    public AuthTokenResponse verifyOtpAndActivate(OtpVerifyRequest request, String tenantId) {
        String mobileHash = hashingService.sha256(request.getMobile().trim());

        // 1. Find pending account
        UserAccount account = userAccountRepository
            .findByMobileHashAndTenantId(mobileHash, tenantId)
            .orElseThrow(() -> new AccountNotFoundException(
                "No pending account found for the provided mobile number."));

        if (account.getAccountStatus() == AccountStatus.ACTIVE) {
            throw new InvalidOtpException("Account is already activated. Please login instead.");
        }

        if (account.getAccountStatus() != AccountStatus.PENDING_VERIFICATION) {
            throw new AccountNotFoundException(
                "Account cannot be activated in its current state: " + account.getAccountStatus());
        }

        // 2. Verify OTP
        boolean valid = otpService.verifyOtp(mobileHash, request.getOtp());
        if (!valid) {
            throw new InvalidOtpException("Invalid or expired OTP. Please request a new OTP.");
        }

        // 3. Activate account locally
        account.setAccountStatus(AccountStatus.ACTIVE);
        userAccountRepository.save(account);
        log.info("Account {} activated for tenant {}", account.getId(), tenantId);

        // 4. Activate in Keycloak (best-effort — account is already active locally)
        keycloakService.activateUser(account.getKeycloakUserId());

        // 5. Issue tokens
        AuthTokenResponse tokens = keycloakService.getTokens(
            account.getUsername(),
            // Password not stored in plaintext — use a one-time activation grant
            // For now, fall back to client_credentials until password grant is wired
            ""
        );

        // 6. Publish audit event
        auditEventPublisher.publish(
            AuditEventType.LOGIN,
            account.getId().toString(),
            "identity:otp-activation",
            null, null,
            Map.of("tenantId", tenantId, "event", "otp-activation")
        );

        return tokens;
    }
}
