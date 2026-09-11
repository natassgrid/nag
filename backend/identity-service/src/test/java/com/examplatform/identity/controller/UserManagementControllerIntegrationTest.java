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

import com.examplatform.identity.domain.enums.AccountStatus;
import com.examplatform.identity.domain.enums.UserRole;
import com.examplatform.identity.dto.AdminCreateUserRequest;
import com.examplatform.identity.dto.AdminUpdateUserRequest;
import com.examplatform.identity.dto.UserAccountResponse;
import com.examplatform.identity.service.UserManagementService;
import com.examplatform.identity.support.AbstractIntegrationTest;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("UserManagementController REST Endpoints E2E Tests (MockMvc)")
class UserManagementControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private UserManagementService userManagementService;

    private static final String TENANT_ID = "default";
    private static final UUID TARGET_USER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID ADMIN_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    // =========================================================================
    // 1. POST /api/v1/identity/users (Admin-initiated user creation)
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/identity/users")
    class CreateUserEndpoint {

        @Test
        @DisplayName("+ve: SUPER_ADMIN creates user successfully - returns 201 Created")
        void superAdminCanCreateUser() throws Exception {
            AdminCreateUserRequest request = AdminCreateUserRequest.builder()
                    .fullName("Rohan Verma")
                    .email("rohan.verma@nag.gov.in")
                    .password("SuperSecretPass123#")
                    .roles(List.of(UserRole.EVALUATOR))
                    .build();

            UserAccountResponse response = UserAccountResponse.builder()
                    .id(TARGET_USER_ID)
                    .username("rohan.verma@nag.gov.in")
                    .accountStatus("ACTIVE")
                    .roles(List.of("EVALUATOR"))
                    .createdAt(Instant.now())
                    .build();

            when(userManagementService.createUser(any(AdminCreateUserRequest.class), any(), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/identity/users")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")).jwt(j -> j.subject(ADMIN_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.id").value(TARGET_USER_ID.toString()))
                    .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));
        }

        @Test
        @DisplayName("+ve: SECURITY_ADMIN creates user successfully - returns 201 Created")
        void securityAdminCanCreateUser() throws Exception {
            AdminCreateUserRequest request = AdminCreateUserRequest.builder()
                    .fullName("Security Auditor")
                    .email("auditor@nag.gov.in")
                    .password("SuperSecretPass123#")
                    .roles(List.of(UserRole.SECURITY_ADMIN))
                    .build();

            UserAccountResponse response = UserAccountResponse.builder()
                    .id(TARGET_USER_ID)
                    .username("auditor@nag.gov.in")
                    .accountStatus("ACTIVE")
                    .roles(List.of("SECURITY_ADMIN"))
                    .createdAt(Instant.now())
                    .build();

            when(userManagementService.createUser(any(AdminCreateUserRequest.class), any(), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/identity/users")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SECURITY_ADMIN")).jwt(j -> j.subject(ADMIN_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        @DisplayName("-ve: Invalid email format and blank name return 400 Bad Request")
        void invalidUserPayloadReturnsBadRequest() throws Exception {
            AdminCreateUserRequest invalidRequest = AdminCreateUserRequest.builder()
                    .fullName("") // Blank
                    .email("not-an-email") // Invalid email
                    .password("short") // Min 8
                    .build();

            mockMvc.perform(post("/api/v1/identity/users")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")).jwt(j -> j.subject(ADMIN_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Failed"))
                    .andExpect(jsonPath("$.fieldErrors.email").exists())
                    .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                    .andExpect(jsonPath("$.fieldErrors.password").exists());
        }

        @Test
        @DisplayName("-ve: CANDIDATE role receives 403 Forbidden")
        void candidateCannotCreateUser() throws Exception {
            AdminCreateUserRequest request = AdminCreateUserRequest.builder()
                    .fullName("Rohan Verma")
                    .email("rohan.verma@nag.gov.in")
                    .password("SuperSecretPass123#")
                    .build();

            mockMvc.perform(post("/api/v1/identity/users")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(ADMIN_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request receives 401 Unauthorized")
        void unauthenticatedCannotCreateUser() throws Exception {
            AdminCreateUserRequest request = AdminCreateUserRequest.builder()
                    .fullName("Rohan Verma")
                    .email("rohan.verma@nag.gov.in")
                    .password("SuperSecretPass123#")
                    .build();

            mockMvc.perform(post("/api/v1/identity/users")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // 2. PUT /api/v1/identity/users/{userId} (Update User Account)
    // =========================================================================
    @Nested
    @DisplayName("PUT /api/v1/identity/users/{userId}")
    class UpdateUserEndpoint {

        @Test
        @DisplayName("+ve: SUPER_ADMIN updates user account - returns 200 OK")
        void superAdminCanUpdateUser() throws Exception {
            AdminUpdateUserRequest request = AdminUpdateUserRequest.builder()
                    .fullName("Rohan Updated Verma")
                    .accountStatus(AccountStatus.LOCKED)
                    .mfaEnabled(true)
                    .build();

            UserAccountResponse response = UserAccountResponse.builder()
                    .id(TARGET_USER_ID)
                    .accountStatus("LOCKED")
                    .mfaEnabled(true)
                    .build();

            when(userManagementService.updateUser(eq(TARGET_USER_ID), any(AdminUpdateUserRequest.class), any(), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(put("/api/v1/identity/users/{userId}", TARGET_USER_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")).jwt(j -> j.subject(ADMIN_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.accountStatus").value("LOCKED"))
                    .andExpect(jsonPath("$.data.mfaEnabled").value(true));
        }

        @Test
        @DisplayName("-ve: CANDIDATE role receives 403 Forbidden")
        void candidateCannotUpdateUser() throws Exception {
            AdminUpdateUserRequest request = AdminUpdateUserRequest.builder()
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            mockMvc.perform(put("/api/v1/identity/users/{userId}", TARGET_USER_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(ADMIN_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 3. DELETE /api/v1/identity/users/{userId} (Deactivate User)
    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/v1/identity/users/{userId}")
    class DeactivateUserEndpoint {

        @Test
        @DisplayName("+ve: SUPER_ADMIN deactivates user - returns 200 OK")
        void superAdminCanDeactivateUser() throws Exception {
            doNothing().when(userManagementService).deactivateUser(eq(TARGET_USER_ID), any(), eq(TENANT_ID));

            mockMvc.perform(delete("/api/v1/identity/users/{userId}", TARGET_USER_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")).jwt(j -> j.subject(ADMIN_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("User deactivated successfully."));
        }

        @Test
        @DisplayName("-ve: CANDIDATE receives 403 Forbidden")
        void candidateCannotDeactivateUser() throws Exception {
            mockMvc.perform(delete("/api/v1/identity/users/{userId}", TARGET_USER_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(ADMIN_ID.toString()))))
                    .andExpect(status().isForbidden());
        }
    }
}
