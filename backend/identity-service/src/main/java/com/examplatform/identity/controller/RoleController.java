package com.examplatform.identity.controller;

import com.examplatform.identity.domain.enums.UserRole;
import com.examplatform.identity.dto.RoleAssignmentRequest;
import com.examplatform.identity.dto.RoleAssignmentResponse;
import com.examplatform.identity.service.RoleManagementService;
import com.examplatform.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for role assignment and revocation.
 * Only Super_Admin users can assign/revoke roles.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/identity/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleManagementService roleManagementService;

    /**
     * Assign or revoke a role for a user (Super_Admin only).
     *
     * @param userId         target user UUID
     * @param request        role + action (ASSIGN/REVOKE)
     * @param tenantId       tenant identifier from request header
     * @param authentication current authenticated user
     * @return response with confirmation
     */
    @PostMapping("/{userId}")
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
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or #userId.toString() == authentication.name")
    public ResponseEntity<ApiResponse<List<UserRole>>> getRoles(
            @PathVariable UUID userId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        log.debug("Get roles request for user [{}], tenant [{}]", userId, tenantId);
        List<UserRole> roles = roleManagementService.getRoles(userId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(roles));
    }
}
