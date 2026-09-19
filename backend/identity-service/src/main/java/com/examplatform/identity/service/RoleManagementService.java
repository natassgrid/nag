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
import com.examplatform.identity.dto.ReviewerResponse;
import com.examplatform.identity.dto.RoleAction;
import com.examplatform.identity.dto.RoleAssignmentRequest;
import com.examplatform.identity.dto.RoleAssignmentResponse;
import com.examplatform.identity.dto.UserAccountResponse;
import com.examplatform.identity.exception.AccountNotFoundException;
import com.examplatform.identity.exception.AuthenticationException;
import com.examplatform.identity.repository.UserAccountRepository;
import com.examplatform.identity.repository.UserRoleAssignmentRepository;
import com.examplatform.shared.audit.AuditEventType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for managing user role assignments, revocations,
 * and querying user pools including reviewers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoleManagementService {

    private final UserAccountRepository userAccountRepository;
    private final UserRoleAssignmentRepository roleAssignmentRepository;
    private final AuditEventPublisher auditEventPublisher;

    /**
     * Assign or revoke a role for a user. Only Super_Admin can call this.
     *
     * @param targetUserId the user to modify
     * @param request      role + action (ASSIGN/REVOKE)
     * @param actorId      the Super_Admin performing the action (from JWT)
     * @param tenantId     tenant context
     * @return response with confirmation
     */
    public RoleAssignmentResponse manageRole(UUID targetUserId, RoleAssignmentRequest request,
                                              String actorId, String tenantId) {
        // 1. Verify target user exists
        UserAccount targetAccount = userAccountRepository.findById(targetUserId)
                .orElseThrow(() -> new AccountNotFoundException("User not found: " + targetUserId));

        // 2. Verify tenant match
        if (!tenantId.equals(targetAccount.getTenantId())) {
            throw new AuthenticationException("Cross-tenant role management is not allowed.");
        }

        // 3. Execute action
        if (request.getAction() == RoleAction.ASSIGN) {
            // Check if already assigned
            List<UserRoleAssignment> existing = roleAssignmentRepository
                    .findByUserIdAndTenantId(targetUserId, tenantId);
            boolean alreadyHas = existing.stream()
                    .anyMatch(r -> r.getRole() == request.getRole());
            if (alreadyHas) {
                return RoleAssignmentResponse.builder()
                        .userId(targetUserId)
                        .role(request.getRole())
                        .action(RoleAction.ASSIGN)
                        .message("Role already assigned.")
                        .build();
            }

            UserRoleAssignment assignment = UserRoleAssignment.builder()
                    .userId(targetUserId)
                    .role(request.getRole())
                    .assignedBy(UUID.fromString(actorId))
                    .assignedAt(LocalDateTime.now())
                    .build();
            assignment.setTenantId(tenantId);
            roleAssignmentRepository.save(assignment);

        } else if (request.getAction() == RoleAction.REVOKE) {
            roleAssignmentRepository.deleteByUserIdAndRoleAndTenantId(
                    targetUserId, request.getRole(), tenantId);
        }

        // 4. Publish audit event
        auditEventPublisher.publish(
                AuditEventType.ROLE_CHANGE,
                actorId,
                "identity:roles/" + targetUserId,
                null,
                null,
                Map.of("targetUserId", targetUserId.toString(),
                        "role", request.getRole().name(),
                        "action", request.getAction().name(),
                        "tenantId", tenantId)
        );

        return RoleAssignmentResponse.builder()
                .userId(targetUserId)
                .role(request.getRole())
                .action(request.getAction())
                .message(request.getAction() == RoleAction.ASSIGN
                        ? "Role " + request.getRole() + " assigned successfully."
                        : "Role " + request.getRole() + " revoked successfully.")
                .build();
    }

    /**
     * Get all roles for a user.
     *
     * @param userId   the user whose roles to retrieve
     * @param tenantId tenant context
     * @return list of roles assigned to the user
     */
    public List<UserRole> getRoles(UUID userId, String tenantId) {
        return roleAssignmentRepository.findByUserIdAndTenantId(userId, tenantId)
                .stream()
                .map(UserRoleAssignment::getRole)
                .toList();
    }

    /**
     * List all user accounts for a tenant with their assigned roles.
     *
     * @param tenantId tenant context
     * @return list of user account responses including roles and specialization
     */
    public List<UserAccountResponse> listAllUsers(String tenantId) {
        List<UserAccount> accounts = userAccountRepository.findByTenantId(tenantId);
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> userIds = accounts.stream().map(UserAccount::getId).toList();
        List<UserRoleAssignment> allAssignments = roleAssignmentRepository.findByUserIdIn(userIds);

        Map<UUID, List<String>> rolesByUser = allAssignments.stream()
                .collect(Collectors.groupingBy(
                        UserRoleAssignment::getUserId,
                        Collectors.mapping(a -> a.getRole().name(), Collectors.toList())
                ));

        return accounts.stream()
                .map(account -> UserAccountResponse.builder()
                        .id(account.getId())
                        .username(account.getUsername())
                        .accountStatus(account.getAccountStatus() != null ? account.getAccountStatus().name() : null)
                        .specialization(account.getSpecialization())
                        .mfaEnabled(account.isMfaEnabled())
                        .roles(rolesByUser.getOrDefault(account.getId(), Collections.emptyList()))
                        .createdAt(account.getCreatedAt())
                        .build())
                .toList();
    }

    /**
     * Finds active reviewers and subject matter experts for a given subject and tenant.
     * If subject specialists are available, returns them; if none match, returns general reviewers
     * and exam controllers as escalation/fallback options.
     *
     * @param subject  target subject domain (optional)
     * @param tenantId tenant context
     * @return list of matching reviewer responses
     */
    public List<ReviewerResponse> findReviewers(String subject, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        List<UserAccount> accounts = userAccountRepository.findByTenantId(effectiveTenant);
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> activeUserIds = accounts.stream()
                .filter(a -> a.getAccountStatus() == AccountStatus.ACTIVE)
                .map(UserAccount::getId)
                .toList();

        if (activeUserIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserRoleAssignment> assignments = roleAssignmentRepository.findByUserIdIn(activeUserIds);
        Map<UUID, List<String>> rolesByUser = assignments.stream()
                .collect(Collectors.groupingBy(
                        UserRoleAssignment::getUserId,
                        Collectors.mapping(a -> a.getRole().name(), Collectors.toList())
                ));

        Set<String> reviewerRoles = Set.of(
                UserRole.REVIEWER.name(),
                UserRole.SUBJECT_MATTER_EXPERT.name(),
                UserRole.EXAM_CONTROLLER.name(),
                "QUESTION_REVIEWER"
        );

        List<ReviewerResponse> allReviewers = accounts.stream()
                .filter(a -> a.getAccountStatus() == AccountStatus.ACTIVE)
                .filter(a -> {
                    List<String> roles = rolesByUser.getOrDefault(a.getId(), Collections.emptyList());
                    return roles.stream().anyMatch(reviewerRoles::contains);
                })
                .map(a -> ReviewerResponse.builder()
                        .id(a.getId())
                        .username(a.getUsername())
                        .specialization(a.getSpecialization())
                        .accountStatus(a.getAccountStatus().name())
                        .roles(rolesByUser.getOrDefault(a.getId(), Collections.emptyList()))
                        .build())
                .toList();

        if (subject == null || subject.isBlank()) {
            return allReviewers;
        }

        String targetSubject = subject.trim().toLowerCase();

        // 1. Exact or partial match on specialization
        List<ReviewerResponse> specialists = allReviewers.stream()
                .filter(r -> r.getSpecialization() != null && matchesSubject(r.getSpecialization(), targetSubject))
                .toList();

        if (!specialists.isEmpty()) {
            return specialists;
        }

        // 2. If no specialist, return general reviewers / exam controllers as fallback pool
        return allReviewers;
    }

    private boolean matchesSubject(String specialization, String subject) {
        if (specialization == null || subject == null) return false;
        String s1 = specialization.trim().toLowerCase();
        String s2 = subject.trim().toLowerCase();
        return s1.equals(s2) || s1.contains(s2) || s2.contains(s1);
    }
}
