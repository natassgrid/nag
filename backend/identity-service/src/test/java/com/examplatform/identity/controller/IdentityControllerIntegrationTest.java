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

package com.examplatform.identity.controller;

import com.examplatform.identity.domain.enums.IdentityDocType;
import com.examplatform.identity.dto.*;
import com.examplatform.identity.exception.AccountNotFoundException;
import com.examplatform.identity.exception.AuthenticationException;
import com.examplatform.identity.exception.DuplicateIdentityException;
import com.examplatform.identity.exception.InvalidOtpException;
import com.examplatform.identity.exception.MfaRequiredException;
import com.examplatform.identity.service.*;
import com.examplatform.identity.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("IdentityController REST Endpoints E2E Tests (MockMvc)")
class IdentityControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private OtpVerificationService otpVerificationService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private WebAuthnService webAuthnService;

    @MockitoBean
    private RoleManagementService roleManagementService;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    private static final String TENANT_ID = "default";
    private static final UUID TEST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUpRateLimiting() {
        when(rateLimiterService.isAllowed(any(), anyInt())).thenReturn(true);
    }

    // =========================================================================
    // 1. GET /api/v1/identity/users (Admin user listing)
    // =========================================================================
    @Nested
    @DisplayName("GET /api/v1/identity/users")
    class ListUsersEndpoint {

        @Test
        @DisplayName("+ve: SUPER_ADMIN retrieves user list successfully - returns 200 OK")
        void superAdminCanListUsers() throws Exception {
            UserAccountResponse user = UserAccountResponse.builder()
                    .id(TEST_USER_ID)
                    .username("admin_user")
                    .accountStatus("ACTIVE")
                    .mfaEnabled(true)
                    .roles(List.of("SUPER_ADMIN"))
                    .createdAt(Instant.now())
                    .build();

            when(roleManagementService.listAllUsers(TENANT_ID)).thenReturn(List.of(user));

            mockMvc.perform(get("/api/v1/identity/users")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data[0].id").value(TEST_USER_ID.toString()))
                    .andExpect(jsonPath("$.data[0].username").value("admin_user"))
                    .andExpect(jsonPath("$.data[0].roles[0]").value("SUPER_ADMIN"));
        }

        @Test
        @DisplayName("+ve: SECURITY_ADMIN retrieves user list successfully - returns 200 OK")
        void securityAdminCanListUsers() throws Exception {
            when(roleManagementService.listAllUsers(TENANT_ID)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/identity/users")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SECURITY_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("-ve: CANDIDATE role receives 403 Forbidden")
        void candidateReceivesForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/identity/users")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request receives 401 Unauthorized")
        void unauthenticatedReceivesUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/identity/users")
                            .header("X-Tenant-Id", TENANT_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // 2. POST /api/v1/identity/register (Candidate Registration)
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/identity/register")
    class RegisterEndpoint {

        @Test
        @DisplayName("+ve: Valid candidate registration returns 202 Accepted")
        void validRegistrationReturnsAccepted() throws Exception {
            RegistrationRequest request = RegistrationRequest.builder()
                    .fullName("Priya Sharma")
                    .email("priya.sharma@nag.gov.in")
                    .mobile("9876543210")
                    .identityDocType(IdentityDocType.AADHAAR)
                    .identityDocNumber("123456789012")
                    .password("SecurePass123#")
                    .build();

            RegistrationResponse response = RegistrationResponse.builder()
                    .userId(TEST_USER_ID.toString())
                    .message("Registration initiated. OTP sent to registered mobile.")
                    .build();

            when(registrationService.register(any(RegistrationRequest.class), eq(TENANT_ID))).thenReturn(response);

            mockMvc.perform(post("/api/v1/identity/register")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID.toString()))
                    .andExpect(jsonPath("$.data.message").value("Registration initiated. OTP sent to registered mobile."));
        }

        @Test
        @DisplayName("-ve: Missing required fields and invalid mobile format returns 400 Bad Request")
        void invalidPayloadReturnsBadRequest() throws Exception {
            RegistrationRequest invalidRequest = RegistrationRequest.builder()
                    .fullName("") // Blank: violates @NotBlank & @Size
                    .email("not-an-email") // Invalid email
                    .mobile("1234") // Invalid pattern
                    .identityDocType(null) // Violates @NotNull
                    .password("short") // Violates @Size(min = 8)
                    .build();

            mockMvc.perform(post("/api/v1/identity/register")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Failed"))
                    .andExpect(jsonPath("$.fieldErrors.email").exists())
                    .andExpect(jsonPath("$.fieldErrors.mobile").exists())
                    .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                    .andExpect(jsonPath("$.fieldErrors.password").exists());
        }

        @Test
        @DisplayName("-ve: Duplicate email / identity doc returns 409 Conflict")
        void duplicateIdentityReturnsConflict() throws Exception {
            RegistrationRequest request = RegistrationRequest.builder()
                    .fullName("Priya Sharma")
                    .email("duplicate@nag.gov.in")
                    .mobile("9876543210")
                    .identityDocType(IdentityDocType.AADHAAR)
                    .identityDocNumber("123456789012")
                    .password("SecurePass123#")
                    .build();

            when(registrationService.register(any(RegistrationRequest.class), eq(TENANT_ID)))
                    .thenThrow(new DuplicateIdentityException("An account with this email address already exists."));

            mockMvc.perform(post("/api/v1/identity/register")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Duplicate Identity"))
                    .andExpect(jsonPath("$.detail").value("An account with this email address already exists."));
        }
    }

    // =========================================================================
    // 3. POST /api/v1/identity/otp/verify (OTP Verification & Account Activation)
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/identity/otp/verify")
    class OtpVerificationEndpoint {

        @Test
        @DisplayName("+ve: Valid OTP verification returns 200 OK with AuthTokenResponse")
        void validOtpReturnsOkWithTokens() throws Exception {
            OtpVerifyRequest request = OtpVerifyRequest.builder()
                    .userId(TEST_USER_ID.toString())
                    .mobile("9876543210")
                    .otp("123456")
                    .build();

            AuthTokenResponse tokenResponse = AuthTokenResponse.builder()
                    .accessToken("mock-access-token")
                    .refreshToken("mock-refresh-token")
                    .tokenType("Bearer")
                    .expiresIn(900L)
                    .userId(TEST_USER_ID.toString())
                    .build();

            when(otpVerificationService.verifyOtpAndActivate(any(OtpVerifyRequest.class), eq(TENANT_ID)))
                    .thenReturn(tokenResponse);

            mockMvc.perform(post("/api/v1/identity/otp/verify")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.accessToken").value("mock-access-token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID.toString()));
        }

        @Test
        @DisplayName("-ve: Invalid OTP length returns 400 Bad Request")
        void invalidOtpLengthReturnsBadRequest() throws Exception {
            OtpVerifyRequest invalidRequest = OtpVerifyRequest.builder()
                    .userId(TEST_USER_ID.toString())
                    .mobile("9876543210")
                    .otp("123") // Must be 6 digits
                    .build();

            mockMvc.perform(post("/api/v1/identity/otp/verify")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Failed"))
                    .andExpect(jsonPath("$.fieldErrors.otp").exists());
        }

        @Test
        @DisplayName("-ve: Expired or wrong OTP returns 422 Unprocessable Entity")
        void expiredOtpReturnsUnprocessableEntity() throws Exception {
            OtpVerifyRequest request = OtpVerifyRequest.builder()
                    .userId(TEST_USER_ID.toString())
                    .mobile("9876543210")
                    .otp("999999")
                    .build();

            when(otpVerificationService.verifyOtpAndActivate(any(OtpVerifyRequest.class), eq(TENANT_ID)))
                    .thenThrow(new InvalidOtpException("Invalid or expired OTP. Please request a new OTP."));

            mockMvc.perform(post("/api/v1/identity/otp/verify")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.title").value("Invalid OTP"))
                    .andExpect(jsonPath("$.detail").value("Invalid or expired OTP. Please request a new OTP."));
        }
    }

    // =========================================================================
    // 4. POST /api/v1/identity/auth/token (Username/Password Authentication)
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/identity/auth/token")
    class AuthenticationEndpoint {

        @Test
        @DisplayName("+ve: Valid credentials return 200 OK with tokens")
        void validCredentialsReturnOk() throws Exception {
            AuthTokenRequest request = AuthTokenRequest.builder()
                    .username("candidate01")
                    .password("CorrectPass123#")
                    .build();

            AuthTokenResponse tokenResponse = AuthTokenResponse.builder()
                    .accessToken("jwt-access-token")
                    .refreshToken("jwt-refresh-token")
                    .tokenType("Bearer")
                    .expiresIn(900L)
                    .userId(TEST_USER_ID.toString())
                    .build();

            when(authenticationService.authenticate(any(AuthTokenRequest.class), eq(TENANT_ID), any()))
                    .thenReturn(tokenResponse);

            mockMvc.perform(post("/api/v1/identity/auth/token")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.accessToken").value("jwt-access-token"));
        }

        @Test
        @DisplayName("-ve: Invalid credentials return 401 Unauthorized")
        void invalidCredentialsReturnUnauthorized() throws Exception {
            AuthTokenRequest request = AuthTokenRequest.builder()
                    .username("candidate01")
                    .password("WrongPassword")
                    .build();

            when(authenticationService.authenticate(any(AuthTokenRequest.class), eq(TENANT_ID), any()))
                    .thenThrow(new AuthenticationException("Invalid username or password."));

            mockMvc.perform(post("/api/v1/identity/auth/token")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.title").value("Unauthorized"))
                    .andExpect(jsonPath("$.detail").value("Invalid username or password."));
        }

        @Test
        @DisplayName("-ve: Account requiring MFA returns 403 Forbidden with mfaRequired true")
        void mfaRequiredReturnsForbidden() throws Exception {
            AuthTokenRequest request = AuthTokenRequest.builder()
                    .username("admin01")
                    .password("CorrectPassword")
                    .build();

            when(authenticationService.authenticate(any(AuthTokenRequest.class), eq(TENANT_ID), any()))
                    .thenThrow(new MfaRequiredException("MFA OTP required for this account."));

            mockMvc.perform(post("/api/v1/identity/auth/token")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.title").value("MFA Required"))
                    .andExpect(jsonPath("$.mfaRequired").value(true));
        }

        @Test
        @DisplayName("-ve: Blank username and password return 400 Bad Request")
        void blankCredentialsReturnBadRequest() throws Exception {
            AuthTokenRequest request = AuthTokenRequest.builder()
                    .username("")
                    .password("")
                    .build();

            mockMvc.perform(post("/api/v1/identity/auth/token")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Failed"));
        }
    }

    // =========================================================================
    // 5. POST & PUT /api/v1/identity/auth/change-password
    // =========================================================================
    @Nested
    @DisplayName("POST & PUT change-password")
    class ChangePasswordEndpoint {

        @Test
        @DisplayName("+ve: Authenticated user with valid payload returns 200 OK via POST")
        void authenticatedUserCanChangePasswordPost() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("OldSecurePassword123#");
            request.setNewPassword("NewSecurePassword123#");

            mockMvc.perform(post("/api/v1/identity/auth/change-password")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().jwt(builder -> builder.subject(TEST_USER_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Password changed successfully."));

            verify(authenticationService).changePassword(
                    eq(TEST_USER_ID.toString()), eq("OldSecurePassword123#"), eq("NewSecurePassword123#"), eq(TENANT_ID));
        }

        @Test
        @DisplayName("+ve: Authenticated user with valid payload returns 200 OK via PUT")
        void authenticatedUserCanChangePasswordPut() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("OldSecurePassword123#");
            request.setNewPassword("NewSecurePassword123#");

            mockMvc.perform(put("/api/v1/identity/auth/change-password")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().jwt(builder -> builder.subject(TEST_USER_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Password changed successfully."));

            verify(authenticationService).changePassword(
                    eq(TEST_USER_ID.toString()), eq("OldSecurePassword123#"), eq("NewSecurePassword123#"), eq(TENANT_ID));
        }

        @Test
        @DisplayName("-ve: Invalid current password returns 401 Unauthorized")
        void invalidCurrentPasswordReturnsUnauthorized() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("WrongPassword");
            request.setNewPassword("NewSecurePassword123#");

            doThrow(new AuthenticationException("Current password is incorrect."))
                    .when(authenticationService).changePassword(any(), any(), any(), any());

            mockMvc.perform(post("/api/v1/identity/auth/change-password")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().jwt(builder -> builder.subject(TEST_USER_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedCannotChangePassword() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("OldPassword");
            request.setNewPassword("NewPassword123");

            mockMvc.perform(post("/api/v1/identity/auth/change-password")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("-ve: Short new password returns 400 Bad Request")
        void shortNewPasswordReturnsBadRequest() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("OldSecurePassword123#");
            request.setNewPassword("short"); // min 8

            mockMvc.perform(post("/api/v1/identity/auth/change-password")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().jwt(builder -> builder.subject(TEST_USER_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
        }
    }

    // =========================================================================
    // 6. POST /api/v1/identity/otp/resend
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/identity/otp/resend")
    class ResendOtpEndpoint {

        @Test
        @DisplayName("+ve: Valid resend request returns 200 OK")
        void validResendReturnsOk() throws Exception {
            OtpResendRequest request = new OtpResendRequest();
            request.setUserId(TEST_USER_ID.toString());

            mockMvc.perform(post("/api/v1/identity/otp/resend")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("OTP resent successfully."));

            verify(registrationService).resendOtp(eq(TEST_USER_ID), eq(TENANT_ID));
        }

        @Test
        @DisplayName("-ve: Account not found returns 404 Not Found")
        void accountNotFoundReturnsNotFound() throws Exception {
            OtpResendRequest request = new OtpResendRequest();
            request.setUserId(TEST_USER_ID.toString());

            doThrow(new AccountNotFoundException("Account not found for user: " + TEST_USER_ID))
                    .when(registrationService).resendOtp(any(), any());

            mockMvc.perform(post("/api/v1/identity/otp/resend")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("-ve: Already active account returns 422 Unprocessable Entity")
        void alreadyActiveReturnsUnprocessableEntity() throws Exception {
            OtpResendRequest request = new OtpResendRequest();
            request.setUserId(TEST_USER_ID.toString());

            doThrow(new InvalidOtpException("Account is already verified. Please login instead."))
                    .when(registrationService).resendOtp(any(), any());

            mockMvc.perform(post("/api/v1/identity/otp/resend")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("-ve: Blank userId returns 400 Bad Request")
        void blankUserIdReturnsBadRequest() throws Exception {
            OtpResendRequest request = new OtpResendRequest();
            request.setUserId("");

            mockMvc.perform(post("/api/v1/identity/otp/resend")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.userId").exists());
        }
    }

    // =========================================================================
    // 7. DELETE /api/v1/identity/auth/logout
    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/v1/identity/auth/logout")
    class LogoutEndpoint {

        @Test
        @DisplayName("+ve: Authenticated logout returns 200 OK")
        void authenticatedUserCanLogout() throws Exception {
            mockMvc.perform(delete("/api/v1/identity/auth/logout")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().jwt(builder -> builder.subject(TEST_USER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Logged out successfully."));

            verify(authenticationService).logout(eq(TEST_USER_ID.toString()), eq(TENANT_ID));
        }

        @Test
        @DisplayName("-ve: Unauthenticated logout returns 401 Unauthorized")
        void unauthenticatedLogoutReturnsUnauthorized() throws Exception {
            mockMvc.perform(delete("/api/v1/identity/auth/logout"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
