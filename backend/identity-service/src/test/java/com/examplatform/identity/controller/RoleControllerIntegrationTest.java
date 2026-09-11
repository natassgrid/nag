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

import com.examplatform.identity.domain.enums.UserRole;
import com.examplatform.identity.dto.*;
import com.examplatform.identity.service.RoleDefinitionService;
import com.examplatform.identity.service.RoleManagementService;
import com.examplatform.identity.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("RoleController REST Endpoints E2E Tests (MockMvc)")
class RoleControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private RoleManagementService roleManagementService;

    @MockitoBean
    private RoleDefinitionService roleDefinitionService;

    private static final String TENANT_ID = "default";
    private static final UUID ROLE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CALLER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    // =========================================================================
    // 1. GET /api/v1/identity/roles/definitions (Role Definition Listing)
    // =========================================================================
    @Nested
    @DisplayName("GET /api/v1/identity/roles/definitions")
    class ListRolesEndpoint {

        @Test
        @DisplayName("+ve: SUPER_ADMIN lists role definitions - returns 200 OK")
        void superAdminCanListRoles() throws Exception {
            RoleDefinitionResponse role = RoleDefinitionResponse.builder()
                    .id(ROLE_ID)
                    .name("Exam Supervisor")
                    .code("EXAM_SUPERVISOR")
                    .description("Supervises exam sessions")
                    .active(true)
                    .systemRole(false)
                    .createdAt(Instant.now())
                    .build();

            when(roleDefinitionService.listRoles(eq(TENANT_ID), eq(0), eq(20), eq("")))
                    .thenReturn(new PageImpl<>(List.of(role), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/v1/identity/roles/definitions")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.content[0].id").value(ROLE_ID.toString()))
                    .andExpect(jsonPath("$.data.content[0].code").value("EXAM_SUPERVISOR"));
        }

        @Test
        @DisplayName("-ve: CANDIDATE receives 403 Forbidden")
        void candidateReceivesForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/identity/roles/definitions")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated receives 401 Unauthorized")
        void unauthenticatedReceivesUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/identity/roles/definitions")
                            .header("X-Tenant-Id", TENANT_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // 2. GET /api/v1/identity/roles/definitions/{roleId} (Get Role by ID)
    // =========================================================================
    @Nested
    @DisplayName("GET /api/v1/identity/roles/definitions/{roleId}")
    class GetRoleEndpoint {

        @Test
        @DisplayName("+ve: SECURITY_ADMIN retrieves role definition - returns 200 OK")
        void securityAdminCanGetRole() throws Exception {
            RoleDefinitionResponse role = RoleDefinitionResponse.builder()
                    .id(ROLE_ID)
                    .name("Auditor")
                    .code("AUDITOR")
                    .active(true)
                    .build();

            when(roleDefinitionService.getRole(eq(ROLE_ID), eq(TENANT_ID))).thenReturn(role);

            mockMvc.perform(get("/api/v1/identity/roles/definitions/{roleId}", ROLE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SECURITY_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.id").value(ROLE_ID.toString()))
                    .andExpect(jsonPath("$.data.code").value("AUDITOR"));
        }

        @Test
        @DisplayName("-ve: CANDIDATE receives 403 Forbidden")
        void candidateReceivesForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/identity/roles/definitions/{roleId}", ROLE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 3. POST /api/v1/identity/roles/definitions (Create Role Definition)
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/identity/roles/definitions")
    class CreateRoleEndpoint {

        @Test
        @DisplayName("+ve: SUPER_ADMIN creates role definition - returns 201 Created")
        void superAdminCanCreateRole() throws Exception {
            CreateRoleRequest request = CreateRoleRequest.builder()
                    .name("Exam Evaluator")
                    .code("EXAM_EVALUATOR")
                    .description("Evaluates exam answer scripts")
                    .permissionIds(Set.of(UUID.randomUUID()))
                    .build();

            RoleDefinitionResponse response = RoleDefinitionResponse.builder()
                    .id(ROLE_ID)
                    .name(request.getName())
                    .code(request.getCode())
                    .description(request.getDescription())
                    .active(true)
                    .build();

            when(roleDefinitionService.createRole(any(CreateRoleRequest.class), any(), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/identity/roles/definitions")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")).jwt(j -> j.subject(CALLER_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.code").value("EXAM_EVALUATOR"));
        }

        @Test
        @DisplayName("-ve: Invalid role code pattern returns 400 Bad Request")
        void invalidRoleCodeReturnsBadRequest() throws Exception {
            CreateRoleRequest request = CreateRoleRequest.builder()
                    .name("Invalid Role")
                    .code("invalid_code_lowercase") // Violates ^[A-Z][A-Z0-9_]*$
                    .build();

            mockMvc.perform(post("/api/v1/identity/roles/definitions")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")).jwt(j -> j.subject(CALLER_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Failed"))
                    .andExpect(jsonPath("$.fieldErrors.code").exists());
        }

        @Test
        @DisplayName("-ve: SECURITY_ADMIN cannot create role definitions (requires SUPER_ADMIN) - returns 403 Forbidden")
        void securityAdminCannotCreateRole() throws Exception {
            CreateRoleRequest request = CreateRoleRequest.builder()
                    .name("Exam Evaluator")
                    .code("EXAM_EVALUATOR")
                    .build();

            mockMvc.perform(post("/api/v1/identity/roles/definitions")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SECURITY_ADMIN")).jwt(j -> j.subject(CALLER_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 4. PUT /api/v1/identity/roles/definitions/{roleId} (Update Role)
    // =========================================================================
    @Nested
    @DisplayName("PUT /api/v1/identity/roles/definitions/{roleId}")
    class UpdateRoleEndpoint {

        @Test
        @DisplayName("+ve: SUPER_ADMIN updates role definition - returns 200 OK")
        void superAdminCanUpdateRole() throws Exception {
            UpdateRoleRequest request = UpdateRoleRequest.builder()
                    .name("Updated Evaluator Title")
                    .description("Updated evaluator description")
                    .active(true)
                    .permissionIds(Set.of())
                    .build();

            RoleDefinitionResponse response = RoleDefinitionResponse.builder()
                    .id(ROLE_ID)
                    .name("Updated Evaluator Title")
                    .code("EXAM_EVALUATOR")
                    .build();

            when(roleDefinitionService.updateRole(eq(ROLE_ID), any(UpdateRoleRequest.class), any(), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(put("/api/v1/identity/roles/definitions/{roleId}", ROLE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")).jwt(j -> j.subject(CALLER_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.name").value("Updated Evaluator Title"));
        }

        @Test
        @DisplayName("-ve: CANDIDATE cannot update role - returns 403 Forbidden")
        void candidateCannotUpdateRole() throws Exception {
            UpdateRoleRequest request = UpdateRoleRequest.builder()
                    .name("Hacked Role")
                    .build();

            mockMvc.perform(put("/api/v1/identity/roles/definitions/{roleId}", ROLE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 5. DELETE /api/v1/identity/roles/definitions/{roleId} (Delete Role)
    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/v1/identity/roles/definitions/{roleId}")
    class DeleteRoleEndpoint {

        @Test
        @DisplayName("+ve: SUPER_ADMIN deletes custom role - returns 200 OK")
        void superAdminCanDeleteRole() throws Exception {
            doNothing().when(roleDefinitionService).deleteRole(eq(ROLE_ID), any(), eq(TENANT_ID));

            mockMvc.perform(delete("/api/v1/identity/roles/definitions/{roleId}", ROLE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")).jwt(j -> j.subject(CALLER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Role deleted successfully."));
        }

        @Test
        @DisplayName("-ve: CANDIDATE cannot delete role - returns 403 Forbidden")
        void candidateCannotDeleteRole() throws Exception {
            mockMvc.perform(delete("/api/v1/identity/roles/definitions/{roleId}", ROLE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 6. GET /api/v1/identity/roles/permissions (Permission Listing)
    // =========================================================================
    @Nested
    @DisplayName("GET /api/v1/identity/roles/permissions")
    class ListPermissionsEndpoint {

        @Test
        @DisplayName("+ve: SECURITY_ADMIN lists available permissions - returns 200 OK")
        void securityAdminCanListPermissions() throws Exception {
            PermissionResponse perm = PermissionResponse.builder()
                    .id(UUID.randomUUID())
                    .code("EXAM_CREATE")
                    .name("Create Examination")
                    .module("EXAMINATION")
                    .build();

            when(roleDefinitionService.listPermissions(eq(TENANT_ID), eq(0), eq(50), eq("")))
                    .thenReturn(new PageImpl<>(List.of(perm), PageRequest.of(0, 50), 1));

            mockMvc.perform(get("/api/v1/identity/roles/permissions")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SECURITY_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.content[0].code").value("EXAM_CREATE"));
        }

        @Test
        @DisplayName("-ve: CANDIDATE cannot view permissions - returns 403 Forbidden")
        void candidateCannotListPermissions() throws Exception {
            mockMvc.perform(get("/api/v1/identity/roles/permissions")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 7. POST /api/v1/identity/roles/assignments/{userId} (Assign/Revoke Role)
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/identity/roles/assignments/{userId}")
    class ManageRoleAssignmentEndpoint {

        @Test
        @DisplayName("+ve: SUPER_ADMIN assigns role to user - returns 200 OK")
        void superAdminCanAssignRole() throws Exception {
            RoleAssignmentRequest request = RoleAssignmentRequest.builder()
                    .role(UserRole.EVALUATOR)
                    .action(RoleAction.ASSIGN)
                    .build();

            RoleAssignmentResponse response = RoleAssignmentResponse.builder()
                    .userId(USER_ID)
                    .role(UserRole.EVALUATOR)
                    .action(RoleAction.ASSIGN)
                    .message("Role assigned successfully.")
                    .build();

            when(roleManagementService.manageRole(eq(USER_ID), any(RoleAssignmentRequest.class), any(), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/identity/roles/assignments/{userId}", USER_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")).jwt(j -> j.subject(CALLER_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()))
                    .andExpect(jsonPath("$.data.role").value("EVALUATOR"))
                    .andExpect(jsonPath("$.data.action").value("ASSIGN"));
        }

        @Test
        @DisplayName("-ve: Null role or action returns 400 Bad Request")
        void nullFieldsReturnBadRequest() throws Exception {
            RoleAssignmentRequest request = RoleAssignmentRequest.builder()
                    .role(null)
                    .action(null)
                    .build();

            mockMvc.perform(post("/api/v1/identity/roles/assignments/{userId}", USER_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")).jwt(j -> j.subject(CALLER_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Failed"));
        }

        @Test
        @DisplayName("-ve: CANDIDATE cannot assign roles - returns 403 Forbidden")
        void candidateCannotAssignRoles() throws Exception {
            RoleAssignmentRequest request = RoleAssignmentRequest.builder()
                    .role(UserRole.SUPER_ADMIN)
                    .action(RoleAction.ASSIGN)
                    .build();

            mockMvc.perform(post("/api/v1/identity/roles/assignments/{userId}", USER_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CALLER_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 8. GET /api/v1/identity/roles/assignments/{userId} (Get User Roles)
    // =========================================================================
    @Nested
    @DisplayName("GET /api/v1/identity/roles/assignments/{userId}")
    class GetUserRolesEndpoint {

        @Test
        @DisplayName("+ve: SUPER_ADMIN retrieves user roles - returns 200 OK")
        void superAdminCanGetUserRoles() throws Exception {
            when(roleManagementService.getRoles(eq(USER_ID), eq(TENANT_ID)))
                    .thenReturn(List.of(UserRole.CANDIDATE, UserRole.EVALUATOR));

            mockMvc.perform(get("/api/v1/identity/roles/assignments/{userId}", USER_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data[0]").value("CANDIDATE"))
                    .andExpect(jsonPath("$.data[1]").value("EVALUATOR"));
        }

        @Test
        @DisplayName("+ve: Target user can retrieve their own roles - returns 200 OK")
        void userCanGetOwnRoles() throws Exception {
            when(roleManagementService.getRoles(eq(USER_ID), eq(TENANT_ID)))
                    .thenReturn(List.of(UserRole.CANDIDATE));

            mockMvc.perform(get("/api/v1/identity/roles/assignments/{userId}", USER_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(USER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data[0]").value("CANDIDATE"));
        }

        @Test
        @DisplayName("-ve: Different candidate cannot view another user's roles - returns 403 Forbidden")
        void otherCandidateCannotViewRoles() throws Exception {
            mockMvc.perform(get("/api/v1/identity/roles/assignments/{userId}", USER_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CALLER_ID.toString()))))
                    .andExpect(status().isForbidden());
        }
    }
}
