package com.examplatform.identity.controller;

import com.examplatform.identity.dto.AuthTokenRequest;
import com.examplatform.identity.dto.AuthTokenResponse;
import com.examplatform.identity.dto.OtpVerifyRequest;
import com.examplatform.identity.dto.RegistrationRequest;
import com.examplatform.identity.dto.RegistrationResponse;
import com.examplatform.identity.service.OtpVerificationService;
import com.examplatform.identity.service.RegistrationService;
import com.examplatform.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the Identity Service.
 * Full business logic is implemented in tasks 2.2–2.8.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/identity")
@RequiredArgsConstructor
public class IdentityController {

    private final RegistrationService registrationService;
    private final OtpVerificationService otpVerificationService;

    /**
     * Initiate candidate registration.
     *
     * @param request  registration payload with identity document and contact details
     * @param tenantId tenant identifier from request header
     * @return 202 Accepted with acknowledgement
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegistrationResponse>> register(
            @Valid @RequestBody RegistrationRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        log.debug("Registration request received for identity doc type [{}], tenant [{}]",
                request.getIdentityDocType(), tenantId);
        long start = System.currentTimeMillis();
        RegistrationResponse response = registrationService.register(request, tenantId);
        log.debug("Registration completed in {}ms for tenant [{}]", System.currentTimeMillis() - start, tenantId);
        return ResponseEntity.accepted()
                .body(ApiResponse.success(response, "Registration initiated. OTP sent to registered mobile."));
    }

    /**
     * Verify OTP and activate the pending account.
     * Issues JWT access + refresh tokens via Keycloak on success.
     *
     * @param request  OTP verification payload
     * @param tenantId tenant identifier from request header
     * @return 200 OK with JWT tokens
     */
    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        log.debug("OTP verification request received, tenant [{}]", tenantId);
        AuthTokenResponse tokens = otpVerificationService.verifyOtpAndActivate(request, tenantId);
        return ResponseEntity.ok(ApiResponse.success(tokens, "Account activated successfully."));
    }

    /**
     * Authenticate with username/password and optional MFA OTP.
     * Full implementation: task 2.4.
     *
     * @param request authentication credentials
     * @return 200 OK with JWT access and refresh tokens
     */
    @PostMapping("/auth/token")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> token(
            @Valid @RequestBody AuthTokenRequest request) {
        log.debug("Token request received for username [{}]", request.getUsername());
        return ResponseEntity.ok(ApiResponse.success(
                AuthTokenResponse.builder().build(),
                "Authentication successful"));
    }

    /**
     * Authenticate using WebAuthn / FIDO2 assertion.
     * Full implementation: task 2.5.
     *
     * @return 200 OK with JWT access and refresh tokens
     */
    @PostMapping("/auth/webauthn")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> webAuthn() {
        log.debug("WebAuthn authentication request received");
        return ResponseEntity.ok(ApiResponse.success(
                AuthTokenResponse.builder().build(),
                "WebAuthn authentication successful"));
    }
}
