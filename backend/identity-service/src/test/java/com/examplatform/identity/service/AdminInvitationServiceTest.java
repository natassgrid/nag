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

import com.examplatform.identity.domain.AdminInvitation;
import com.examplatform.identity.domain.UserAccount;
import com.examplatform.identity.domain.enums.AccountStatus;
import com.examplatform.identity.domain.enums.InvitationStatus;
import com.examplatform.identity.dto.AcceptInviteRequest;
import com.examplatform.identity.dto.AdminInviteRequest;
import com.examplatform.identity.dto.AdminInviteResponse;
import com.examplatform.identity.dto.AuthTokenResponse;
import com.examplatform.identity.exception.DuplicateIdentityException;
import com.examplatform.identity.repository.AdminInvitationRepository;
import com.examplatform.identity.repository.UserAccountRepository;
import com.examplatform.identity.repository.UserRoleAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInvitationServiceTest {

    @Mock
    private AdminInvitationRepository invitationRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private UserRoleAssignmentRepository userRoleAssignmentRepository;
    @Mock
    private IdentityEmailService emailService;
    @Mock
    private TotpService totpService;
    @Mock
    private KeycloakService keycloakService;
    @Mock
    private HashingService hashingService;
    @Mock
    private AuditEventPublisher auditEventPublisher;

    private AdminInvitationService invitationService;

    @BeforeEach
    void setUp() {
        invitationService = new AdminInvitationService(
                invitationRepository,
                userAccountRepository,
                userRoleAssignmentRepository,
                emailService,
                totpService,
                keycloakService,
                hashingService,
                auditEventPublisher
        );
    }

    @Test
    @DisplayName("Should invite admin, create invitation, and dispatch onboarding email")
    void shouldInviteAdmin() {
        AdminInviteRequest request = new AdminInviteRequest();
        request.setEmail("priya@example.gov.in");
        request.setFullName("Dr. Priya Sharma");
        request.setRoles(List.of("QUESTION_AUTHOR", "REVIEWER"));
        request.setSpecialization("Computer Science");

        UUID invitedBy = UUID.randomUUID();
        String tenantId = "default";

        when(hashingService.sha256(anyString())).thenReturn("mockHash");
        when(userAccountRepository.findByUsernameAndTenantId("priya@example.gov.in", tenantId))
                .thenReturn(Optional.empty());
        when(invitationRepository.save(any(AdminInvitation.class))).thenAnswer(inv -> {
            AdminInvitation i = inv.getArgument(0);
            ReflectionTestUtils.setField(i, "id", UUID.randomUUID());
            return i;
        });
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount acc = inv.getArgument(0);
            ReflectionTestUtils.setField(acc, "id", UUID.randomUUID());
            return acc;
        });

        AdminInviteResponse response = invitationService.inviteAdmin(request, invitedBy, tenantId);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("priya@example.gov.in");
        assertThat(response.getRoles()).contains("QUESTION_AUTHOR", "REVIEWER");
        verify(emailService).sendAdminInvitationEmail(eq("priya@example.gov.in"), eq("Dr. Priya Sharma"), eq("QUESTION_AUTHOR,REVIEWER"), anyString());
    }

    @Test
    @DisplayName("Should reject invite if user already exists and is active")
    void shouldRejectDuplicateActiveAdmin() {
        AdminInviteRequest request = new AdminInviteRequest();
        request.setEmail("active@example.gov.in");
        request.setFullName("Active User");
        request.setRoles(List.of("QUESTION_AUTHOR"));

        UserAccount activeAcc = UserAccount.builder()
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(hashingService.sha256(anyString())).thenReturn("mockHash");
        when(userAccountRepository.findByUsernameAndTenantId("active@example.gov.in", "default"))
                .thenReturn(Optional.of(activeAcc));

        assertThatThrownBy(() -> invitationService.inviteAdmin(request, UUID.randomUUID(), "default"))
                .isInstanceOf(DuplicateIdentityException.class);
    }

    @Test
    @DisplayName("Should accept invitation, verify TOTP, activate account, and return tokens")
    void shouldAcceptInvitation() {
        String rawToken = "my-secret-invitation-token-123456";
        AdminInvitation invitation = AdminInvitation.builder()
                .email("priya@example.gov.in")
                .fullName("Dr. Priya")
                .roles("QUESTION_AUTHOR")
                .status(InvitationStatus.PENDING.name())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        invitation.setTenantId("default");
        ReflectionTestUtils.setField(invitation, "id", UUID.randomUUID());

        UUID userId = UUID.randomUUID();
        UserAccount account = UserAccount.builder()
                .username("priya@example.gov.in")
                .accountStatus(AccountStatus.PENDING_SETUP)
                .build();
        account.setTenantId("default");
        ReflectionTestUtils.setField(account, "id", userId);

        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(invitation));
        when(totpService.verifyTotpCode(eq("SECRET_KEY"), eq("123456"))).thenReturn(true);
        when(userAccountRepository.findByUsernameAndTenantId("priya@example.gov.in", "default")).thenReturn(Optional.of(account));
        when(totpService.encodeHashedBackupCodes(any())).thenReturn("hashed1,hashed2");
        when(keycloakService.getTokens(anyString(), anyString(), anyString())).thenReturn(
                AuthTokenResponse.builder().accessToken("token123").userId(userId.toString()).build()
        );

        AcceptInviteRequest request = new AcceptInviteRequest();
        request.setToken(rawToken);
        request.setPassword("P@ssword123!");
        request.setTotpSecret("SECRET_KEY");
        request.setTotpCode("123456");
        request.setBackupCodes(List.of("BC1-1234", "BC2-5678"));

        AuthTokenResponse tokens = invitationService.acceptInvitation(request, "default");

        assertThat(tokens).isNotNull();
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.isMfaEnabled()).isTrue();
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED.name());
        verify(userAccountRepository).save(account);
        verify(invitationRepository).save(invitation);
    }
}
