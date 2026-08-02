package com.examplatform.identity.controller;

import com.examplatform.identity.dto.AdminCreateUserRequest;
import com.examplatform.identity.dto.AdminUpdateUserRequest;
import com.examplatform.identity.dto.UserAccountResponse;
import com.examplatform.identity.service.UserManagementService;
import com.examplatform.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for admin user management operations:
 * create, update, and deactivate users.
 *
 * Accessible only by SUPER_ADMIN and SECURITY_ADMIN roles.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/identity/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN')")
public class UserManagementController {

    private final UserManagementService userManagementService;

    /**
     * Create a new user account (admin-initiated).
     * Creates the account in ACTIVE status, bypassing OTP verification.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserAccountResponse>> createUser(
            @Valid @RequestBody AdminCreateUserRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            Authentication authentication) {
        String actorId = authentication.getName();
        log.debug("Admin [{}] creating user [{}] in tenant [{}]", actorId, request.getEmail(), tenantId);
        UserAccountResponse response = userManagementService.createUser(request, actorId, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User created successfully."));
    }

    /**
     * Update an existing user account (admin-initiated).
     */
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserAccountResponse>> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUpdateUserRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            Authentication authentication) {
        String actorId = authentication.getName();
        log.debug("Admin [{}] updating user [{}] in tenant [{}]", actorId, userId, tenantId);
        UserAccountResponse response = userManagementService.updateUser(userId, request, actorId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "User updated successfully."));
    }

    /**
     * Deactivate a user account (admin-initiated).
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @PathVariable UUID userId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            Authentication authentication) {
        String actorId = authentication.getName();
        log.debug("Admin [{}] deactivating user [{}] in tenant [{}]", actorId, userId, tenantId);
        userManagementService.deactivateUser(userId, actorId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "User deactivated successfully."));
    }
}
