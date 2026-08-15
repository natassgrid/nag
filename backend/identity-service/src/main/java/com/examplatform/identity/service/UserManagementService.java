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

import com.examplatform.identity.domain.UserAccount;
import com.examplatform.identity.domain.UserRoleAssignment;
import com.examplatform.identity.domain.enums.AccountStatus;
import com.examplatform.identity.domain.enums.UserRole;
import com.examplatform.identity.dto.AdminCreateUserRequest;
import com.examplatform.identity.dto.AdminUpdateUserRequest;
import com.examplatform.identity.dto.UserAccountResponse;
import com.examplatform.identity.exception.AccountNotFoundException;
import com.examplatform.identity.exception.DuplicateIdentityException;
import com.examplatform.identity.repository.UserAccountRepository;
import com.examplatform.identity.repository.UserRoleAssignmentRepository;
import com.examplatform.shared.audit.AuditEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for admin-initiated user management operations:
 * create, update, and deactivate user accounts.
 *
 * Validates: Requirements 29.1, 29.2, 29.3
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserManagementService {

    private final UserAccountRepository userAccountRepository;
    private final UserRoleAssignmentRepository roleAssignmentRepository;
    private final HashingService hashingService;
    private final AuditEventPublisher auditEventPublisher;

    /**
     * Creates a new user account (admin-initiated). The account is created
     * in ACTIVE status, bypassing OTP verification.
     */
    public UserAccountResponse createUser(AdminCreateUserRequest request, String actorId, String tenantId) {
        String emailHash = hashingService.sha256(request.getEmail().toLowerCase().trim());

        // Check for duplicate email
        if (userAccountRepository.existsByEmailHashAndTenantId(emailHash, tenantId)) {
            throw new DuplicateIdentityException("An account with this email already exists.");
        }

        // Create user account in ACTIVE state
        UserAccount account = UserAccount.builder()
                .username(request.getEmail().toLowerCase().trim())
                .emailHash(emailHash)
                .mobileHash(hashingService.sha256("admin-created-" + UUID.randomUUID()))
                .accountStatus(AccountStatus.ACTIVE)
                .mfaEnabled(false)
                .failedAttemptCount(0)
                .build();
        account.setTenantId(tenantId);

        UserAccount saved = userAccountRepository.save(account);
        log.info("Admin {} created user {} in tenant {}", actorId, saved.getId(), tenantId);

        // Assign roles if provided
        List<String> assignedRoles = Collections.emptyList();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            assignedRoles = request.getRoles().stream().map(role -> {
                UserRoleAssignment assignment = UserRoleAssignment.builder()
                        .userId(saved.getId())
                        .role(role)
                        .assignedBy(UUID.fromString(actorId))
                        .assignedAt(LocalDateTime.now())
                        .build();
                assignment.setTenantId(tenantId);
                roleAssignmentRepository.save(assignment);
                return role.name();
            }).toList();
        }

        // Audit event
        auditEventPublisher.publish(
                AuditEventType.CANDIDATE_PROFILE_CREATED,
                actorId,
                "identity:users/" + saved.getId(),
                null, null,
                Map.of("tenantId", tenantId, "action", "ADMIN_CREATE_USER",
                        "targetUsername", request.getEmail())
        );

        return UserAccountResponse.builder()
                .id(saved.getId())
                .username(saved.getUsername())
                .accountStatus(saved.getAccountStatus().name())
                .mfaEnabled(saved.isMfaEnabled())
                .roles(assignedRoles)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    /**
     * Updates an existing user account (admin-initiated).
     * Only non-null fields in the request are applied.
     */
    public UserAccountResponse updateUser(UUID userId, AdminUpdateUserRequest request,
                                          String actorId, String tenantId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException("User not found: " + userId));

        if (!tenantId.equals(account.getTenantId())) {
            throw new AccountNotFoundException("User not found in this tenant.");
        }

        // Apply updates
        if (request.getFullName() != null) {
            account.setUsername(request.getFullName());
        }
        if (request.getAccountStatus() != null) {
            account.setAccountStatus(request.getAccountStatus());
        }
        if (request.getMfaEnabled() != null) {
            account.setMfaEnabled(request.getMfaEnabled());
        }

        userAccountRepository.save(account);
        log.info("Admin {} updated user {} in tenant {}", actorId, userId, tenantId);

        // Get current roles
        List<String> roles = roleAssignmentRepository.findByUserIdAndTenantId(userId, tenantId)
                .stream()
                .map(a -> a.getRole().name())
                .toList();

        // Audit event
        auditEventPublisher.publish(
                AuditEventType.ROLE_CHANGE,
                actorId,
                "identity:users/" + userId,
                null, null,
                Map.of("tenantId", tenantId, "action", "ADMIN_UPDATE_USER")
        );

        return UserAccountResponse.builder()
                .id(account.getId())
                .username(account.getUsername())
                .accountStatus(account.getAccountStatus().name())
                .mfaEnabled(account.isMfaEnabled())
                .roles(roles)
                .createdAt(account.getCreatedAt())
                .build();
    }

    /**
     * Deactivates a user account by setting status to DEACTIVATED.
     */
    public void deactivateUser(UUID userId, String actorId, String tenantId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException("User not found: " + userId));

        if (!tenantId.equals(account.getTenantId())) {
            throw new AccountNotFoundException("User not found in this tenant.");
        }

        account.setAccountStatus(AccountStatus.DEACTIVATED);
        userAccountRepository.save(account);
        log.info("Admin {} deactivated user {} in tenant {}", actorId, userId, tenantId);

        // Audit event
        auditEventPublisher.publish(
                AuditEventType.ROLE_CHANGE,
                actorId,
                "identity:users/" + userId,
                null, null,
                Map.of("tenantId", tenantId, "action", "ADMIN_DEACTIVATE_USER",
                        "targetUserId", userId.toString())
        );
    }
}
