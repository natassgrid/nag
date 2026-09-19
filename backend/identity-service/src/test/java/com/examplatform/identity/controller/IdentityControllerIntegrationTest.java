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
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].username").value("admin_user"))
                    .andExpect(jsonPath("$.data[0].roles[0]").value("SUPER_ADMIN"));

            verify(roleManagementService).listAllUsers(TENANT_ID);
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
        @DisplayName("-ve: CANDIDATE is forbidden - returns 403 Forbidden")
        void candidateForbiddenFromListingUsers() throws Exception {
            mockMvc.perform(get("/api/v1/identity/users")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedCannotListUsers() throws Exception {
            mockMvc.perform(get("/api/v1/identity/users")
                            .header("X-Tenant-Id", TENANT_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // 2. POST /api/v1/identity/register
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/identity/register")
    class RegisterEndpoint {

        private RegistrationRequest validRequest() {
            RegistrationRequest req = new RegistrationRequest();
            req.setIdentityDocType(IdentityDocType.AADHAAR);
            req.setIdentityDocNumber("123456789012");
            req.setFullName("Ramesh Kumar");
            req.setEmail("ramesh.kumar@example.com");
            req.setMobile("9876543210");
            req.setPassword("SecurePassword123#");
            return req;
        }

        @Test
        @DisplayName("+ve: Valid registration returns 202 Accepted")
        void validRegistrationReturnsAccepted() throws Exception {
            RegistrationRequest request = validRequest();
            RegistrationResponse response = RegistrationResponse.builder()
                    .userId(TEST_USER_ID.toString())
                    .message("Registration initiated")
                    .build();

            when(registrationService.register(any(RegistrationRequest.class), eq(TENANT_ID))).thenReturn(response);

            mockMvc.perform(post("/api/v1/identity/register")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID.toString()));

            verify(registrationService).register(any(RegistrationRequest.class), eq(TENANT_ID));
        }

        @Test
        @DisplayName("-ve: Duplicate identity returns 409 Conflict")
        void duplicateIdentityReturnsConflict() throws Exception {
            RegistrationRequest request = validRequest();

            when(registrationService.register(any(RegistrationRequest.class), eq(TENANT_ID)))
                    .thenThrow(new DuplicateIdentityException("Identity document already registered"));

            mockMvc.perform(post("/api/v1/identity/register")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Duplicate Identity"));
        }

        @Test
        @DisplayName("-ve: Invalid email format returns 400 Bad Request")
        void invalidEmailReturnsBadRequest() throws Exception {
            RegistrationRequest request = validRequest();
            request.setEmail("not-an-email");

            mockMvc.perform(post("/api/v1/identity/register")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Failed"));
        }

        @Test
        @DisplayName("-ve: Missing identityDocType returns 400 Bad Request")
        void missingDocTypeReturnsBadRequest() throws Exception {
            RegistrationRequest request = validRequest();
            request.setIdentityDocType(null);

            mockMvc.perform(post("/api/v1/identity/register")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Failed"));
        }
    }

    // =========================================================================
    // 3. POST /api/v1/identity/otp/verify
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/identity/otp/verify")
    class VerifyOtpEndpoint {

        @Test
        @DisplayName("+ve: Valid OTP returns 200 OK with tokens")
        void validOtpReturnsOkWithTokens() throws Exception {
            OtpVerifyRequest request = new OtpVerifyRequest();
            request.setUserId(TEST_USER_ID.toString());
            request.setOtp("123456");

            AuthTokenResponse tokens = AuthTokenResponse.builder()
                    .accessToken("sample.access.token")
                    .refreshToken("sample.refresh.token")
                    .expiresIn(900L)
                    .tokenType("Bearer")
                    .userId(TEST_USER_ID.toString())
                    .build();

            when(otpVerificationService.verifyOtpAndActivate(any(OtpVerifyRequest.class), eq(TENANT_ID)))
                    .thenReturn(tokens);

            mockMvc.perform(post("/api/v1/identity/otp/verify")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.accessToken").value("sample.access.token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID.toString()));
        }

        @Test
        @DisplayName("-ve: Invalid OTP returns 422 Unprocessable Entity")
        void invalidOtpReturnsUnprocessableEntity() throws Exception {
            OtpVerifyRequest request = new OtpVerifyRequest();
            request.setUserId(TEST_USER_ID.toString());
            request.setOtp("000000");

            when(otpVerificationService.verifyOtpAndActivate(any(OtpVerifyRequest.class), eq(TENANT_ID)))
                    .thenThrow(new InvalidOtpException("Invalid or expired OTP"));

            mockMvc.perform(post("/api/v1/identity/otp/verify")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.title").value("Invalid OTP"));
        }

        @Test
        @DisplayName("-ve: Blank OTP returns 400 Bad Request")
        void blankOtpReturnsBadRequest() throws Exception {
            OtpVerifyRequest request = new OtpVerifyRequest();
            request.setUserId(TEST_USER_ID.toString());
            request.setOtp("");

            mockMvc.perform(post("/api/v1/identity/otp/verify")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Failed"));
        }
    }

    // =========================================================================
    // 4. POST /api/v1/identity/auth/token
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/identity/auth/token")
    class TokenEndpoint {

        @Test
        @DisplayName("+ve: Valid credentials return 200 OK with tokens")
        void validCredentialsReturnTokens() throws Exception {
            AuthTokenRequest request = AuthTokenRequest.builder()
                    .username("user@example.com")
                    .password("Password123#")
                    .build();

            AuthTokenResponse tokens = AuthTokenResponse.builder()
                    .accessToken("valid.jwt.token")
                    .refreshToken("refresh.jwt.token")
                    .expiresIn(900L)
                    .tokenType("Bearer")
                    .userId(TEST_USER_ID.toString())
                    .build();

            when(authenticationService.authenticate(any(AuthTokenRequest.class), eq(TENANT_ID), any()))
                    .thenReturn(tokens);

            mockMvc.perform(post("/api/v1/identity/auth/token")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.accessToken").value("valid.jwt.token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("-ve: MFA required returns 403 Forbidden with MFA error code")
        void mfaRequiredReturnsForbidden() throws Exception {
            AuthTokenRequest request = AuthTokenRequest.builder()
                    .username("user@example.com")
                    .password("Password123#")
                    .build();

            when(authenticationService.authenticate(any(AuthTokenRequest.class), eq(TENANT_ID), any()))
                    .thenThrow(new MfaRequiredException("MFA required. Please provide OTP code."));

            mockMvc.perform(post("/api/v1/identity/auth/token")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.title").value("MFA Required"));
        }

        @Test
        @DisplayName("-ve: Invalid credentials return 401 Unauthorized")
        void invalidCredentialsReturnUnauthorized() throws Exception {
            AuthTokenRequest request = AuthTokenRequest.builder()
                    .username("user@example.com")
                    .password("WrongPassword")
                    .build();

            when(authenticationService.authenticate(any(AuthTokenRequest.class), eq(TENANT_ID), any()))
                    .thenThrow(new AuthenticationException("Invalid credentials"));

            mockMvc.perform(post("/api/v1/identity/auth/token")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.title").value("Unauthorized"));
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
    // 4b. POST /api/v1/identity/auth/token/refresh
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/identity/auth/token/refresh")
    class RefreshTokenEndpoint {

        @Test
        @DisplayName("+ve: Valid refresh token returns 200 OK with rotated tokens")
        void validRefreshTokenReturnsTokens() throws Exception {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("sample-valid-refresh-token")
                    .deviceFingerprint("fp-device-1")
                    .build();

            AuthTokenResponse tokens = AuthTokenResponse.builder()
                    .accessToken("new.access.token")
                    .refreshToken("new.refresh.token")
                    .expiresIn(900L)
                    .tokenType("Bearer")
                    .userId(TEST_USER_ID.toString())
                    .build();

            when(authenticationService.refreshToken(any(RefreshTokenRequest.class), eq(TENANT_ID), any()))
                    .thenReturn(tokens);

            mockMvc.perform(post("/api/v1/identity/auth/token/refresh")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.accessToken").value("new.access.token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("new.refresh.token"))
                    .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID.toString()));
        }

        @Test
        @DisplayName("-ve: Blank refresh token returns 400 Bad Request")
        void blankRefreshTokenReturnsBadRequest() throws Exception {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("")
                    .build();

            mockMvc.perform(post("/api/v1/identity/auth/token/refresh")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Failed"));
        }

        @Test
        @DisplayName("-ve: Invalid or expired refresh token returns 401 Unauthorized")
        void invalidRefreshTokenReturnsUnauthorized() throws Exception {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("invalid-token")
                    .build();

            when(authenticationService.refreshToken(any(RefreshTokenRequest.class), eq(TENANT_ID), any()))
                    .thenThrow(new AuthenticationException("Invalid or expired refresh token"));

            mockMvc.perform(post("/api/v1/identity/auth/token/refresh")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.title").value("Unauthorized"));
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
