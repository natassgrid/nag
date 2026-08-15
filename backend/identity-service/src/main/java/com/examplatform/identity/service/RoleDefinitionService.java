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

import com.examplatform.identity.domain.Permission;
import com.examplatform.identity.domain.RoleDefinition;
import com.examplatform.identity.dto.CreateRoleRequest;
import com.examplatform.identity.dto.PermissionResponse;
import com.examplatform.identity.dto.RoleDefinitionResponse;
import com.examplatform.identity.dto.UpdateRoleRequest;
import com.examplatform.identity.exception.AccountNotFoundException;
import com.examplatform.identity.exception.DuplicateIdentityException;
import com.examplatform.identity.repository.PermissionRepository;
import com.examplatform.identity.repository.RoleDefinitionRepository;
import com.examplatform.shared.audit.AuditEventType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing role definitions (CRUD) and their permission assignments.
 * Only SUPER_ADMIN users can invoke these operations (enforced at controller level).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoleDefinitionService {

    private final RoleDefinitionRepository roleDefinitionRepository;
    private final PermissionRepository permissionRepository;
    private final AuditEventPublisher auditEventPublisher;

    /**
     * List all role definitions for a tenant with pagination and search.
     */
    public Page<RoleDefinitionResponse> listRoles(String tenantId, int page, int size, String search) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<RoleDefinition> roles = roleDefinitionRepository.findByTenantIdAndSearch(tenantId, search, pageRequest);
        return roles.map(this::toResponse);
    }

    /**
     * Get a single role definition by ID.
     */
    public RoleDefinitionResponse getRole(UUID roleId, String tenantId) {
        RoleDefinition role = roleDefinitionRepository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new AccountNotFoundException("Role not found: " + roleId));
        return toResponse(role);
    }

    /**
     * Create a new custom role definition.
     */
    public RoleDefinitionResponse createRole(CreateRoleRequest request, String actorId, String tenantId) {
        // Check for code uniqueness
        if (roleDefinitionRepository.existsByCodeAndTenantId(request.getCode(), tenantId)) {
            throw new DuplicateIdentityException("Role with code '" + request.getCode() + "' already exists.");
        }

        RoleDefinition role = RoleDefinition.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .active(true)
                .systemRole(false)
                .build();
        role.setTenantId(tenantId);

        // Assign permissions if provided
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            List<Permission> permissions = permissionRepository.findByIdInAndTenantId(
                    request.getPermissionIds(), tenantId);
            role.setPermissions(new HashSet<>(permissions));
        }

        RoleDefinition saved = roleDefinitionRepository.save(role);
        log.info("Role created: [{}] '{}' by actor [{}] in tenant [{}]",
                saved.getId(), saved.getCode(), actorId, tenantId);

        auditEventPublisher.publish(
                AuditEventType.ROLE_CHANGE,
                actorId,
                "identity:role-definitions/" + saved.getId(),
                null, null,
                Map.of("action", "CREATE",
                        "roleCode", saved.getCode(),
                        "tenantId", tenantId)
        );

        return toResponse(saved);
    }

    /**
     * Update an existing role definition. System roles cannot have their code changed.
     */
    public RoleDefinitionResponse updateRole(UUID roleId, UpdateRoleRequest request, String actorId, String tenantId) {
        RoleDefinition role = roleDefinitionRepository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new AccountNotFoundException("Role not found: " + roleId));

        if (request.getName() != null) {
            role.setName(request.getName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getActive() != null) {
            role.setActive(request.getActive());
        }

        // Update permissions if provided
        if (request.getPermissionIds() != null) {
            List<Permission> permissions = permissionRepository.findByIdInAndTenantId(
                    request.getPermissionIds(), tenantId);
            role.setPermissions(new HashSet<>(permissions));
        }

        RoleDefinition saved = roleDefinitionRepository.save(role);
        log.info("Role updated: [{}] '{}' by actor [{}] in tenant [{}]",
                saved.getId(), saved.getCode(), actorId, tenantId);

        auditEventPublisher.publish(
                AuditEventType.ROLE_CHANGE,
                actorId,
                "identity:role-definitions/" + saved.getId(),
                null, null,
                Map.of("action", "UPDATE",
                        "roleCode", saved.getCode(),
                        "tenantId", tenantId)
        );

        return toResponse(saved);
    }

    /**
     * Delete a role definition. System roles cannot be deleted.
     */
    public void deleteRole(UUID roleId, String actorId, String tenantId) {
        RoleDefinition role = roleDefinitionRepository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new AccountNotFoundException("Role not found: " + roleId));

        if (role.isSystemRole()) {
            throw new IllegalStateException("System roles cannot be deleted.");
        }

        roleDefinitionRepository.delete(role);
        log.info("Role deleted: [{}] '{}' by actor [{}] in tenant [{}]",
                roleId, role.getCode(), actorId, tenantId);

        auditEventPublisher.publish(
                AuditEventType.ROLE_CHANGE,
                actorId,
                "identity:role-definitions/" + roleId,
                null, null,
                Map.of("action", "DELETE",
                        "roleCode", role.getCode(),
                        "tenantId", tenantId)
        );
    }

    /**
     * Get all available permissions for a tenant (for assignment UI).
     */
    public Page<PermissionResponse> listPermissions(String tenantId, int page, int size, String search) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "module", "name"));
        Page<Permission> permissions = permissionRepository.findByTenantIdAndSearch(tenantId, search, pageRequest);
        return permissions.map(this::toPermissionResponse);
    }

    // -----------------------------------------------------------------------
    // Mapping helpers
    // -----------------------------------------------------------------------

    private RoleDefinitionResponse toResponse(RoleDefinition role) {
        List<PermissionResponse> permissionResponses = role.getPermissions().stream()
                .map(this::toPermissionResponse)
                .collect(Collectors.toList());

        return RoleDefinitionResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .code(role.getCode())
                .description(role.getDescription())
                .active(role.isActive())
                .systemRole(role.isSystemRole())
                .permissions(permissionResponses)
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }

    private PermissionResponse toPermissionResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .name(permission.getName())
                .description(permission.getDescription())
                .module(permission.getModule())
                .build();
    }
}
