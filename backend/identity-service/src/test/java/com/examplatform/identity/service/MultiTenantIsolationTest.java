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

import com.examplatform.shared.crypto.HashingService;

import com.examplatform.identity.domain.UserAccount;
import com.examplatform.identity.domain.UserRoleAssignment;
import com.examplatform.identity.domain.enums.AccountStatus;
import com.examplatform.identity.domain.enums.UserRole;
import com.examplatform.identity.dto.AuthTokenRequest;
import com.examplatform.identity.dto.RefreshTokenRequest;
import com.examplatform.identity.dto.ReviewerResponse;
import com.examplatform.identity.dto.UserAccountResponse;
import com.examplatform.identity.exception.AccountNotFoundException;
import com.examplatform.identity.exception.AuthenticationException;
import com.examplatform.identity.repository.ActiveSessionRepository;
import com.examplatform.identity.repository.OtpVerificationRepository;
import com.examplatform.identity.repository.UserAccountRepository;
import com.examplatform.identity.repository.UserRoleAssignmentRepository;
import com.examplatform.shared.config.DynamicConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Multi-Tenant Isolation Unit Tests")
class MultiTenantIsolationTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserRoleAssignmentRepository roleAssignmentRepository;

    @Mock
    private ActiveSessionRepository activeSessionRepository;

    @Mock
    private KeycloakService keycloakService;

    @Mock
    private HashingService hashingService;

    @Mock
    private OtpService otpService;

    @Mock
    private TotpService totpService;

    @Mock
    private RiskAssessmentService riskAssessmentService;

    @Mock
    private AccountLockoutService accountLockoutService;

    @Mock
    private DynamicConfigService dynamicConfigService;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @InjectMocks
    private RoleManagementService roleManagementService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("RoleManagementService lists users strictly partitioned by tenantId")
    void listsUsersStrictlyPartitionedByTenant() {
        UUID u1 = UUID.randomUUID();
        UserAccount acc1 = UserAccount.builder()
                .username("upsc-admin@example.com")
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        acc1.setTenantId("upsc");
        ReflectionTestUtils.setField(acc1, "id", u1);

        when(userAccountRepository.findByTenantId("upsc")).thenReturn(List.of(acc1));
        when(roleAssignmentRepository.findByUserIdInAndTenantId(List.of(u1), "upsc"))
                .thenReturn(List.of(
                        UserRoleAssignment.builder().userId(u1).role(UserRole.SUPER_ADMIN).build()
                ));

        List<UserAccountResponse> users = roleManagementService.listAllUsers("upsc");
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUsername()).isEqualTo("upsc-admin@example.com");
        assertThat(users.get(0).getRoles()).contains("SUPER_ADMIN");
    }

    @Test
    @DisplayName("RoleManagementService findReviewers strictly partitioned by tenantId")
    void findReviewersStrictlyPartitionedByTenant() {
        UUID u1 = UUID.randomUUID();
        UserAccount acc1 = UserAccount.builder()
                .username("upsc-reviewer@example.com")
                .accountStatus(AccountStatus.ACTIVE)
                .specialization("General Studies")
                .build();
        acc1.setTenantId("upsc");
        ReflectionTestUtils.setField(acc1, "id", u1);

        when(userAccountRepository.findByTenantId("upsc")).thenReturn(List.of(acc1));
        when(roleAssignmentRepository.findByUserIdInAndTenantId(List.of(u1), "upsc"))
                .thenReturn(List.of(
                        UserRoleAssignment.builder().userId(u1).role(UserRole.REVIEWER).build()
                ));

        List<ReviewerResponse> reviewers = roleManagementService.findReviewers("General Studies", "upsc");
        assertThat(reviewers).hasSize(1);
        assertThat(reviewers.get(0).getUsername()).isEqualTo("upsc-reviewer@example.com");
    }

    @Test
    @DisplayName("AuthenticationService fails to find user when credentials exist in different tenant")
    void failsAuthWhenUserInDifferentTenant() {
        when(hashingService.sha256("test@example.com")).thenReturn("hash-123");
        when(userAccountRepository.findByEmailHashAndTenantId("hash-123", "ssc"))
                .thenReturn(Optional.empty());
        when(userAccountRepository.findByMobileHashAndTenantId("hash-123", "ssc"))
                .thenReturn(Optional.empty());
        when(userAccountRepository.findByUsernameIgnoreCaseAndTenantId("test@example.com", "ssc"))
                .thenReturn(Optional.empty());

        AuthTokenRequest request = new AuthTokenRequest();
        request.setUsername("test@example.com");
        request.setPassword("password123");

        assertThatThrownBy(() -> authenticationService.authenticate(request, "ssc", "127.0.0.1"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid credentials");
    }
}
