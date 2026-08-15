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
import com.examplatform.identity.dto.CreateRoleRequest;
import com.examplatform.identity.dto.PermissionResponse;
import com.examplatform.identity.dto.RoleAssignmentRequest;
import com.examplatform.identity.dto.RoleAssignmentResponse;
import com.examplatform.identity.dto.RoleDefinitionResponse;
import com.examplatform.identity.dto.UpdateRoleRequest;
import com.examplatform.identity.service.RoleDefinitionService;
import com.examplatform.identity.service.RoleManagementService;
import com.examplatform.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for role management operations:
 * - Role definitions CRUD (create, list, get, update, delete)
 * - Permission listing
 * - User role assignment/revocation (existing functionality)
 *
 * Only SUPER_ADMIN users can manage role definitions.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/identity/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleManagementService roleManagementService;
    private final RoleDefinitionService roleDefinitionService;

    // ===================================================================
    // Role Definition CRUD Endpoints
    // ===================================================================

    /**
     * List all role definitions with pagination and search.
     */
    @GetMapping("/definitions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN')")
    public ResponseEntity<ApiResponse<Page<RoleDefinitionResponse>>> listRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        log.debug("List roles request: tenant [{}], page [{}], size [{}], search [{}]",
                tenantId, page, size, search);
        Page<RoleDefinitionResponse> roles = roleDefinitionService.listRoles(tenantId, page, size, search);
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    /**
     * Get a specific role definition by ID.
     */
    @GetMapping("/definitions/{roleId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN')")
    public ResponseEntity<ApiResponse<RoleDefinitionResponse>> getRole(
            @PathVariable UUID roleId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        log.debug("Get role request: roleId [{}], tenant [{}]", roleId, tenantId);
        RoleDefinitionResponse role = roleDefinitionService.getRole(roleId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(role));
    }

    /**
     * Create a new role definition.
     */
    @PostMapping("/definitions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<RoleDefinitionResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            Authentication authentication) {
        String actorId = authentication.getName();
        log.debug("Create role request: actor [{}], code [{}], tenant [{}]",
                actorId, request.getCode(), tenantId);
        RoleDefinitionResponse response = roleDefinitionService.createRole(request, actorId, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Role created successfully."));
    }

    /**
     * Update an existing role definition.
     */
    @PutMapping("/definitions/{roleId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<RoleDefinitionResponse>> updateRole(
            @PathVariable UUID roleId,
            @Valid @RequestBody UpdateRoleRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            Authentication authentication) {
        String actorId = authentication.getName();
        log.debug("Update role request: actor [{}], roleId [{}], tenant [{}]", actorId, roleId, tenantId);
        RoleDefinitionResponse response = roleDefinitionService.updateRole(roleId, request, actorId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Role updated successfully."));
    }

    /**
     * Delete a role definition. System roles cannot be deleted.
     */
    @DeleteMapping("/definitions/{roleId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @PathVariable UUID roleId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            Authentication authentication) {
        String actorId = authentication.getName();
        log.debug("Delete role request: actor [{}], roleId [{}], tenant [{}]", actorId, roleId, tenantId);
        roleDefinitionService.deleteRole(roleId, actorId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "Role deleted successfully."));
    }

    // ===================================================================
    // Permission Endpoints
    // ===================================================================

    /**
     * List all available permissions with pagination and search.
     */
    @GetMapping("/permissions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN')")
    public ResponseEntity<ApiResponse<Page<PermissionResponse>>> listPermissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        log.debug("List permissions request: tenant [{}], page [{}], size [{}], search [{}]",
                tenantId, page, size, search);
        Page<PermissionResponse> permissions = roleDefinitionService.listPermissions(tenantId, page, size, search);
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    // ===================================================================
    // User Role Assignment Endpoints (existing functionality)
    // ===================================================================

    /**
     * Assign or revoke a role for a user (Super_Admin only).
     *
     * @param userId         target user UUID
     * @param request        role + action (ASSIGN/REVOKE)
     * @param tenantId       tenant identifier from request header
     * @param authentication current authenticated user
     * @return response with confirmation
     */
    @PostMapping("/assignments/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<RoleAssignmentResponse>> manageRole(
            @PathVariable UUID userId,
            @Valid @RequestBody RoleAssignmentRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            Authentication authentication) {
        String actorId = authentication.getName();
        log.debug("Role management request: actor [{}] -> target [{}], role [{}], action [{}], tenant [{}]",
                actorId, userId, request.getRole(), request.getAction(), tenantId);
        RoleAssignmentResponse response = roleManagementService.manageRole(userId, request, actorId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * List roles for a user (Super_Admin or the user themselves).
     *
     * @param userId   target user UUID
     * @param tenantId tenant identifier from request header
     * @return list of roles assigned to the user
     */
    @GetMapping("/assignments/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or #userId.toString() == authentication.name")
    public ResponseEntity<ApiResponse<List<UserRole>>> getUserRoles(
            @PathVariable("userId") UUID userId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        log.debug("Get roles request for user [{}], tenant [{}]", userId, tenantId);
        List<UserRole> roles = roleManagementService.getRoles(userId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    // Keep legacy endpoint for backward compatibility
    /**
     * @deprecated Use POST /assignments/{userId} instead
     */
    @PostMapping("/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<RoleAssignmentResponse>> manageRoleLegacy(
            @PathVariable UUID userId,
            @Valid @RequestBody RoleAssignmentRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            Authentication authentication) {
        return manageRole(userId, request, tenantId, authentication);
    }

    /**
     * @deprecated Use GET /assignments/{userId} instead
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or #userId.toString() == authentication.name")
    public ResponseEntity<ApiResponse<List<UserRole>>> getRolesLegacy(
            @PathVariable("userId") UUID userId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        return getUserRoles(userId, tenantId);
    }
}
