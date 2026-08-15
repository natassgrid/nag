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
import com.examplatform.identity.dto.RoleAction;
import com.examplatform.identity.dto.RoleAssignmentRequest;
import com.examplatform.identity.dto.RoleAssignmentResponse;
import com.examplatform.identity.exception.AccountNotFoundException;
import com.examplatform.identity.exception.AuthenticationException;
import com.examplatform.identity.repository.UserAccountRepository;
import com.examplatform.identity.repository.UserRoleAssignmentRepository;
import com.examplatform.shared.audit.AuditEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RoleManagementService}.
 *
 * <p><strong>Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6</strong>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleManagementService")
class RoleManagementServiceTest {

    @Mock
    UserAccountRepository userAccountRepository;

    @Mock
    UserRoleAssignmentRepository roleAssignmentRepository;

    @Mock
    AuditEventPublisher auditEventPublisher;

    @InjectMocks
    RoleManagementService roleManagementService;

    private static final String TENANT_ID = "default";
    private static final UUID TARGET_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String ACTOR_ID = "22222222-2222-2222-2222-222222222222";

    private UserAccount targetAccount() {
        UserAccount account = UserAccount.builder()
                .username("candidate@example.com")
                .emailHash("hash-email")
                .mobileHash("hash-mobile")
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        account.setTenantId(TENANT_ID);
        ReflectionTestUtils.setField(account, "id", TARGET_USER_ID);
        return account;
    }

    private RoleAssignmentRequest assignRequest(UserRole role) {
        return RoleAssignmentRequest.builder()
                .role(role)
                .action(RoleAction.ASSIGN)
                .build();
    }

    private RoleAssignmentRequest revokeRequest(UserRole role) {
        return RoleAssignmentRequest.builder()
                .role(role)
                .action(RoleAction.REVOKE)
                .build();
    }

    @Nested
    @DisplayName("Assign role")
    class AssignRole {

        @Test
        @DisplayName("assigns role to user and publishes audit event")
        void assignsRoleToUserAndPublishesAuditEvent() {
            when(userAccountRepository.findById(TARGET_USER_ID))
                    .thenReturn(Optional.of(targetAccount()));
            when(roleAssignmentRepository.findByUserIdAndTenantId(TARGET_USER_ID, TENANT_ID))
                    .thenReturn(List.of());

            RoleAssignmentResponse response = roleManagementService.manageRole(
                    TARGET_USER_ID, assignRequest(UserRole.EVALUATOR), ACTOR_ID, TENANT_ID);

            assertThat(response.getUserId()).isEqualTo(TARGET_USER_ID);
            assertThat(response.getRole()).isEqualTo(UserRole.EVALUATOR);
            assertThat(response.getAction()).isEqualTo(RoleAction.ASSIGN);
            assertThat(response.getMessage()).contains("assigned successfully");

            ArgumentCaptor<UserRoleAssignment> captor = ArgumentCaptor.forClass(UserRoleAssignment.class);
            verify(roleAssignmentRepository).save(captor.capture());
            UserRoleAssignment saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(TARGET_USER_ID);
            assertThat(saved.getRole()).isEqualTo(UserRole.EVALUATOR);
            assertThat(saved.getAssignedBy()).isEqualTo(UUID.fromString(ACTOR_ID));
            assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);

            verify(auditEventPublisher).publish(
                    eq(AuditEventType.ROLE_CHANGE), eq(ACTOR_ID),
                    eq("identity:roles/" + TARGET_USER_ID),
                    isNull(), isNull(), any());
        }

        @Test
        @DisplayName("returns 'already assigned' when role already exists")
        void returnsAlreadyAssignedWhenRoleExists() {
            when(userAccountRepository.findById(TARGET_USER_ID))
                    .thenReturn(Optional.of(targetAccount()));

            UserRoleAssignment existing = UserRoleAssignment.builder()
                    .userId(TARGET_USER_ID)
                    .role(UserRole.EVALUATOR)
                    .build();
            existing.setTenantId(TENANT_ID);
            when(roleAssignmentRepository.findByUserIdAndTenantId(TARGET_USER_ID, TENANT_ID))
                    .thenReturn(List.of(existing));

            RoleAssignmentResponse response = roleManagementService.manageRole(
                    TARGET_USER_ID, assignRequest(UserRole.EVALUATOR), ACTOR_ID, TENANT_ID);

            assertThat(response.getMessage()).isEqualTo("Role already assigned.");
            assertThat(response.getAction()).isEqualTo(RoleAction.ASSIGN);
            verify(roleAssignmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Revoke role")
    class RevokeRole {

        @Test
        @DisplayName("revokes role and publishes audit event")
        void revokesRoleAndPublishesAuditEvent() {
            when(userAccountRepository.findById(TARGET_USER_ID))
                    .thenReturn(Optional.of(targetAccount()));

            RoleAssignmentResponse response = roleManagementService.manageRole(
                    TARGET_USER_ID, revokeRequest(UserRole.AUDITOR), ACTOR_ID, TENANT_ID);

            assertThat(response.getUserId()).isEqualTo(TARGET_USER_ID);
            assertThat(response.getRole()).isEqualTo(UserRole.AUDITOR);
            assertThat(response.getAction()).isEqualTo(RoleAction.REVOKE);
            assertThat(response.getMessage()).contains("revoked successfully");

            verify(roleAssignmentRepository).deleteByUserIdAndRoleAndTenantId(
                    TARGET_USER_ID, UserRole.AUDITOR, TENANT_ID);

            verify(auditEventPublisher).publish(
                    eq(AuditEventType.ROLE_CHANGE), eq(ACTOR_ID),
                    eq("identity:roles/" + TARGET_USER_ID),
                    isNull(), isNull(), any());
        }
    }

    @Nested
    @DisplayName("Error cases")
    class ErrorCases {

        @Test
        @DisplayName("throws AccountNotFoundException when target user does not exist")
        void throwsWhenUserNotFound() {
            when(userAccountRepository.findById(TARGET_USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleManagementService.manageRole(
                    TARGET_USER_ID, assignRequest(UserRole.CANDIDATE), ACTOR_ID, TENANT_ID))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("throws AuthenticationException on cross-tenant attempt")
        void throwsOnCrossTenantAttempt() {
            UserAccount account = targetAccount();
            account.setTenantId("other-tenant");
            when(userAccountRepository.findById(TARGET_USER_ID))
                    .thenReturn(Optional.of(account));

            assertThatThrownBy(() -> roleManagementService.manageRole(
                    TARGET_USER_ID, assignRequest(UserRole.CANDIDATE), ACTOR_ID, TENANT_ID))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Cross-tenant");
        }
    }

    @Nested
    @DisplayName("Get roles")
    class GetRoles {

        @Test
        @DisplayName("returns list of roles for user")
        void returnsListOfRoles() {
            UserRoleAssignment assignment1 = UserRoleAssignment.builder()
                    .userId(TARGET_USER_ID)
                    .role(UserRole.CANDIDATE)
                    .build();
            assignment1.setTenantId(TENANT_ID);

            UserRoleAssignment assignment2 = UserRoleAssignment.builder()
                    .userId(TARGET_USER_ID)
                    .role(UserRole.EVALUATOR)
                    .build();
            assignment2.setTenantId(TENANT_ID);

            when(roleAssignmentRepository.findByUserIdAndTenantId(TARGET_USER_ID, TENANT_ID))
                    .thenReturn(List.of(assignment1, assignment2));

            List<UserRole> roles = roleManagementService.getRoles(TARGET_USER_ID, TENANT_ID);

            assertThat(roles).containsExactly(UserRole.CANDIDATE, UserRole.EVALUATOR);
        }
    }
}
