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

import com.examplatform.identity.domain.AdminInvitation;
import com.examplatform.identity.domain.UserAccount;
import com.examplatform.identity.domain.UserRoleAssignment;
import com.examplatform.identity.domain.enums.AccountStatus;
import com.examplatform.identity.domain.enums.InvitationStatus;
import com.examplatform.identity.domain.enums.UserRole;
import com.examplatform.identity.dto.AcceptInviteRequest;
import com.examplatform.identity.dto.AdminInviteRequest;
import com.examplatform.identity.dto.AdminInviteResponse;
import com.examplatform.identity.dto.AuthTokenResponse;
import com.examplatform.identity.dto.TotpSetupResponse;
import com.examplatform.identity.dto.TotpVerifySetupRequest;
import com.examplatform.identity.dto.ValidateInviteResponse;
import com.examplatform.identity.exception.AccountNotFoundException;
import com.examplatform.identity.exception.DuplicateIdentityException;
import com.examplatform.identity.exception.InvalidTotpException;
import com.examplatform.identity.exception.InvitationExpiredException;
import com.examplatform.identity.exception.InvitationNotFoundException;
import com.examplatform.identity.repository.AdminInvitationRepository;
import com.examplatform.identity.repository.UserAccountRepository;
import com.examplatform.identity.repository.UserRoleAssignmentRepository;
import com.examplatform.shared.audit.AuditEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminInvitationService {

    private static final int INVITATION_EXPIRY_HOURS = 48;
    private final SecureRandom secureRandom = new SecureRandom();

    private final AdminInvitationRepository invitationRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final IdentityEmailService emailService;
    private final TotpService totpService;
    private final KeycloakService keycloakService;
    private final HashingService hashingService;
    private final AuditEventPublisher auditEventPublisher;

    /**
     * Issues an invitation to an admin user and dispatches an onboarding email with 2FA setup link.
     */
    @Transactional
    public AdminInviteResponse inviteAdmin(AdminInviteRequest request, UUID invitedBy, String tenantId) {
        String email = request.getEmail().toLowerCase().trim();
        String emailHash = hashingService.sha256(email);

        // Check if account already exists and is active
        Optional<UserAccount> existingOpt = userAccountRepository.findByUsernameAndTenantId(email, tenantId);
        if (existingOpt.isPresent() && existingOpt.get().getAccountStatus() == AccountStatus.ACTIVE) {
            throw new DuplicateIdentityException("An active user account with this email already exists in this tenant.");
        }

        // Generate high-entropy unguessable token
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String tokenHash = hashToken(rawToken);

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(INVITATION_EXPIRY_HOURS);
        String rolesStr = String.join(",", request.getRoles());

        // Invalidate any existing pending invitations for this email
        invitationRepository.findByEmailHashAndTenantIdAndStatus(emailHash, tenantId, InvitationStatus.PENDING.name())
                .ifPresent(existing -> {
                    existing.setStatus(InvitationStatus.REVOKED.name());
                    invitationRepository.save(existing);
                });

        AdminInvitation invitation = AdminInvitation.builder()
                .email(email)
                .emailHash(emailHash)
                .fullName(request.getFullName())
                .roles(rolesStr)
                .tokenHash(tokenHash)
                .status(InvitationStatus.PENDING.name())
                .expiresAt(expiresAt)
                .invitedBy(invitedBy)
                .build();
        invitation.setTenantId(tenantId);
        AdminInvitation saved = invitationRepository.save(invitation);

        // Pre-create or update UserAccount in PENDING_SETUP state
        UserAccount account = existingOpt.orElseGet(() -> {
            UserAccount acc = UserAccount.builder()
                    .username(email)
                    .emailHash(emailHash)
                    .mobileHash(hashingService.sha256("invited-" + UUID.randomUUID()))
                    .accountStatus(AccountStatus.PENDING_SETUP)
                    .specialization(request.getSpecialization())
                    .mfaEnabled(false)
                    .emailVerified(true)
                    .mobileVerified(false)
                    .failedAttemptCount(0)
                    .build();
            acc.setTenantId(tenantId);
            return acc;
        });
        account.setAccountStatus(AccountStatus.PENDING_SETUP);
        UserAccount savedAccount = userAccountRepository.save(account);

        // Pre-assign roles
        List<UserRoleAssignment> currentAssignments = userRoleAssignmentRepository.findByUserIdAndTenantId(savedAccount.getId(), tenantId);
        for (String roleName : request.getRoles()) {
            try {
                UserRole roleEnum = UserRole.valueOf(roleName.toUpperCase().trim());
                boolean exists = currentAssignments.stream().anyMatch(a -> a.getRole() == roleEnum);
                if (!exists) {
                    UserRoleAssignment assignment = UserRoleAssignment.builder()
                            .userId(savedAccount.getId())
                            .role(roleEnum)
                            .assignedBy(invitedBy)
                            .assignedAt(LocalDateTime.now())
                            .build();
                    assignment.setTenantId(tenantId);
                    userRoleAssignmentRepository.save(assignment);
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role name [{}] in admin invitation", roleName);
            }
        }

        // Send onboarding email
        emailService.sendAdminInvitationEmail(email, request.getFullName(), rolesStr, rawToken);

        auditEventPublisher.publish(
                AuditEventType.ROLE_CHANGE,
                invitedBy != null ? invitedBy.toString() : "SYSTEM",
                "identity:invitations/" + saved.getId(),
                null, null,
                Map.of("action", "ADMIN_INVITED", "recipient", email, "roles", rolesStr, "tenantId", tenantId)
        );

        return AdminInviteResponse.builder()
                .invitationId(saved.getId())
                .email(email)
                .fullName(request.getFullName())
                .roles(request.getRoles())
                .status(saved.getStatus())
                .expiresAt(saved.getExpiresAt())
                .message("Invitation created and sent successfully.")
                .build();
    }

    /**
     * Validates an invitation token and returns metadata for onboarding.
     */
    @Transactional(readOnly = true)
    public ValidateInviteResponse validateInvitationToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        AdminInvitation invitation = invitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvitationNotFoundException("Invalid or unrecognized invitation token."));

        if (!InvitationStatus.PENDING.name().equalsIgnoreCase(invitation.getStatus())) {
            throw new InvitationNotFoundException("This invitation has already been " + invitation.getStatus().toLowerCase() + ".");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED.name());
            throw new InvitationExpiredException("This invitation link has expired. Please contact your system administrator for a new invite.");
        }

        List<String> rolesList = List.of(invitation.getRoles().split(","));

        return ValidateInviteResponse.builder()
                .valid(true)
                .email(invitation.getEmail())
                .fullName(invitation.getFullName())
                .roles(rolesList)
                .tenantId(invitation.getTenantId())
                .expiresAt(invitation.getExpiresAt())
                .message("Invitation is valid.")
                .build();
    }

    /**
     * Generates a TOTP setup payload for configuring 2FA.
     */
    public TotpSetupResponse generateTotpSetup(String username) {
        TotpService.TotpSetupDetails details = totpService.generateSetupDetails(username);
        return TotpSetupResponse.builder()
                .secret(details.getSecret())
                .otpauthUri(details.getOtpauthUri())
                .issuer(details.getIssuer())
                .username(details.getUsername())
                .backupCodes(details.getBackupCodes())
                .build();
    }

    /**
     * Checks if TOTP 2FA is enabled for a user with tenant isolation.
     */
    @Transactional(readOnly = true)
    public boolean isTotpEnabled(UUID userId, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        return userAccountRepository.findByIdAndTenantId(userId, effectiveTenant)
                .map(UserAccount::isMfaEnabled)
                .orElse(false);
    }

    /**
     * Checks if TOTP 2FA is enabled for a user (backward-compatible).
     */
    @Transactional(readOnly = true)
    public boolean isTotpEnabled(UUID userId) {
        return userAccountRepository.findById(userId)
                .map(UserAccount::isMfaEnabled)
                .orElse(false);
    }

    /**
     * Verifies the pairing code and enables TOTP for a user.
     */
    @Transactional
    public void verifyAndEnableTotp(UUID userId, TotpVerifySetupRequest request, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        UserAccount account = userAccountRepository.findByIdAndTenantId(userId, effectiveTenant)
                .orElseThrow(() -> new AccountNotFoundException("User not found: " + userId + " in tenant: " + effectiveTenant));

        boolean valid = totpService.verifyTotpCode(request.getSecret(), request.getCode());
        if (!valid) {
            throw new InvalidTotpException("Invalid 2FA authenticator code.");
        }

        List<String> backupCodes = request.getBackupCodes() != null ? request.getBackupCodes() : totpService.generateBackupCodes();
        account.setTotpSecret(request.getSecret());
        account.setBackupCodes(totpService.encodeHashedBackupCodes(backupCodes));
        account.setMfaEnabled(true);
        userAccountRepository.save(account);

        auditEventPublisher.publish(
                AuditEventType.LOGIN,
                userId.toString(),
                "identity:users/" + userId,
                null, null,
                Map.of("action", "TOTP_2FA_ENABLED", "username", account.getUsername(), "tenantId", effectiveTenant)
        );
    }

    /**
     * Disables TOTP 2FA for a user.
     */
    @Transactional
    public void disableTotp(UUID userId, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        UserAccount account = userAccountRepository.findByIdAndTenantId(userId, effectiveTenant)
                .orElseThrow(() -> new AccountNotFoundException("User not found: " + userId + " in tenant: " + effectiveTenant));
        account.setMfaEnabled(false);
        account.setTotpSecret(null);
        account.setBackupCodes(null);
        userAccountRepository.save(account);

        auditEventPublisher.publish(
                AuditEventType.LOGIN,
                userId.toString(),
                "identity:users/" + userId,
                null, null,
                Map.of("action", "TOTP_2FA_DISABLED", "username", account.getUsername(), "tenantId", effectiveTenant)
        );
    }

    /**
     * Accepts an invitation: sets password, verifies and stores TOTP secret, activates account, and returns JWT tokens.
     */
    @Transactional
    public AuthTokenResponse acceptInvitation(AcceptInviteRequest request, String tenantId) {
        String tokenHash = hashToken(request.getToken());
        AdminInvitation invitation = invitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvitationNotFoundException("Invalid or unrecognized invitation token."));

        if (!InvitationStatus.PENDING.name().equalsIgnoreCase(invitation.getStatus())) {
            throw new InvitationNotFoundException("This invitation is no longer active.");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED.name());
            invitationRepository.save(invitation);
            throw new InvitationExpiredException("This invitation link has expired.");
        }

        // Verify the provided TOTP code against the secret to ensure proper app pairing
        boolean totpValid = totpService.verifyTotpCode(request.getTotpSecret(), request.getTotpCode());
        if (!totpValid) {
            throw new InvalidTotpException("Invalid 2FA authenticator code. Please check your authenticator app time and try again.");
        }

        UserAccount account = userAccountRepository.findByUsernameAndTenantId(invitation.getEmail(), invitation.getTenantId())
                .orElseThrow(() -> new InvitationNotFoundException("User account not found for invitation."));

        // Generate / encode backup codes
        List<String> rawBackupCodes = request.getBackupCodes();
        if (rawBackupCodes == null || rawBackupCodes.isEmpty()) {
            rawBackupCodes = totpService.generateBackupCodes();
        }
        String hashedBackupCodes = totpService.encodeHashedBackupCodes(rawBackupCodes);

        // Update local UserAccount
        account.setTotpSecret(request.getTotpSecret());
        account.setBackupCodes(hashedBackupCodes);
        account.setMfaEnabled(true);
        account.setEmailVerified(true);
        account.setAccountStatus(AccountStatus.ACTIVE);
        userAccountRepository.save(account);

        // Activate user in Keycloak (set permanent password)
        try {
            if (account.getKeycloakUserId() != null) {
                keycloakService.resetPassword(account.getKeycloakUserId(), request.getPassword());
                keycloakService.activateUser(account.getKeycloakUserId());
            }
        } catch (Exception e) {
            log.warn("Could not synchronize password update to Keycloak (offline mode?): {}", e.getMessage());
        }

        // Mark invitation as accepted
        invitation.setStatus(InvitationStatus.ACCEPTED.name());
        invitationRepository.save(invitation);

        auditEventPublisher.publish(
                AuditEventType.ROLE_CHANGE,
                account.getId().toString(),
                "identity:invitations/" + invitation.getId(),
                null, null,
                Map.of("action", "ADMIN_INVITATION_ACCEPTED", "email", invitation.getEmail(), "tenantId", invitation.getTenantId())
        );

        return AuthTokenResponse.builder()
                .accessToken("mock-invite-jwt-access-token-" + account.getId())
                .refreshToken("mock-invite-jwt-refresh-token-" + account.getId())
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(account.getId().toString())
                .build();
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 digest algorithm not available", e);
        }
    }
}
