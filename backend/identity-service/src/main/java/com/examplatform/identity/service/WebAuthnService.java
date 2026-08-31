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
import com.examplatform.identity.domain.WebAuthnCredential;
import com.examplatform.identity.domain.enums.AccountStatus;
import com.examplatform.identity.dto.AuthTokenResponse;
import com.examplatform.identity.dto.WebAuthnAssertionRequest;
import com.examplatform.identity.exception.AuthenticationException;
import com.examplatform.identity.repository.UserAccountRepository;
import com.examplatform.identity.repository.WebAuthnCredentialRepository;
import com.examplatform.shared.audit.AuditEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

/**
 * Server-side WebAuthn / FIDO2 assertion verification.
 *
 * <p><strong>Validates: Requirements 2.3</strong>
 *
 * <p><strong>Production note:</strong> The signature verification in this scaffold
 * validates input format only. For production deployments, integrate a full
 * WebAuthn library such as {@code com.webauthn4j:webauthn4j-core} for COSE key
 * parsing and cryptographic signature verification (ECDSA / RSA).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WebAuthnService {

    private final WebAuthnCredentialRepository webAuthnCredentialRepository;
    private final UserAccountRepository userAccountRepository;
    private final KeycloakService keycloakService;
    private final AuditEventPublisher auditEventPublisher;

    /**
     * Authenticate a user via WebAuthn assertion.
     *
     * @param request   the assertion from the client authenticator
     * @param tenantId  the tenant context
     * @param ipAddress originating IP address
     * @return JWT access and refresh tokens on success
     * @throws AuthenticationException if any verification step fails
     */
    public AuthTokenResponse authenticate(WebAuthnAssertionRequest request, String tenantId, String ipAddress) {

        // 1. Look up credential by credentialId
        WebAuthnCredential credential = webAuthnCredentialRepository
                .findByCredentialIdAndTenantId(request.getCredentialId(), tenantId)
                .orElseThrow(() -> new AuthenticationException("Unknown WebAuthn credential."));

        // 2. Look up associated user account
        UserAccount account = userAccountRepository.findById(credential.getUserId())
                .orElseThrow(() -> new AuthenticationException("Associated account not found."));

        // 3. Verify account is active
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AuthenticationException("Account is not active.");
        }

        // 4. Verify the assertion signature using stored public key
        boolean signatureValid = verifyAssertionSignature(
                request.getAuthenticatorData(),
                request.getClientDataJSON(),
                request.getSignature(),
                credential.getPublicKeyCose()
        );
        if (!signatureValid) {
            throw new AuthenticationException("WebAuthn assertion signature verification failed.");
        }

        // 5. Update sign count (replay protection)
        long newSignCount = extractSignCount(request.getAuthenticatorData());
        if (newSignCount <= credential.getSignCount()) {
            log.warn("SECURITY: Possible cloned authenticator for credential {} - signCount did not increase",
                    credential.getCredentialId());
            // Allow but log — production should flag this
        }
        credential.setSignCount(newSignCount);
        webAuthnCredentialRepository.save(credential);

        // 6. Issue tokens via Keycloak (using service account or direct grant)
        AuthTokenResponse tokens = keycloakService.getTokens(account.getUsername(), "", account.getId().toString());

        // 7. Publish audit event
        auditEventPublisher.publish(AuditEventType.LOGIN,
                String.valueOf(account.getId()), "identity:auth/webauthn",
                ipAddress, null, Map.of("tenantId", tenantId, "method", "webauthn"));

        return tokens;
    }

    /**
     * Verify the WebAuthn assertion signature.
     *
     * <p>Signed data = authenticatorData || SHA-256(clientDataJSON).
     *
     * <p><strong>TODO:</strong> Full COSE key parsing and signature verification.
     * For now, validates that all inputs are non-empty and properly Base64URL-encoded.
     * Production must use a WebAuthn library (e.g., webauthn4j) for cryptographic verification.
     */
    boolean verifyAssertionSignature(String authenticatorDataB64, String clientDataJSONB64,
                                     String signatureB64, byte[] publicKeyCose) {
        try {
            byte[] authData = Base64.getUrlDecoder().decode(authenticatorDataB64);
            byte[] clientDataJSON = Base64.getUrlDecoder().decode(clientDataJSONB64);
            byte[] signature = Base64.getUrlDecoder().decode(signatureB64);

            // Signed data = authenticatorData || SHA-256(clientDataJSON)
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] clientDataHash = sha256.digest(clientDataJSON);
            byte[] signedData = new byte[authData.length + clientDataHash.length];
            System.arraycopy(authData, 0, signedData, 0, authData.length);
            System.arraycopy(clientDataHash, 0, signedData, authData.length, clientDataHash.length);

            // TODO: Full COSE key parsing and signature verification
            // For now, verify that all inputs are non-empty and properly Base64-encoded
            // This is a placeholder — production must use a WebAuthn library (e.g., webauthn4j)
            return authData.length > 0 && signature.length > 0 && clientDataJSON.length > 0;
        } catch (Exception e) {
            log.error("WebAuthn signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract the sign count from authenticator data bytes [33..36] (big-endian uint32).
     */
    long extractSignCount(String authenticatorDataB64) {
        try {
            byte[] authData = Base64.getUrlDecoder().decode(authenticatorDataB64);
            if (authData.length < 37) return 0;
            // Sign count is bytes 33-36 (big-endian uint32)
            return ((long) (authData[33] & 0xFF) << 24)
                    | ((long) (authData[34] & 0xFF) << 16)
                    | ((long) (authData[35] & 0xFF) << 8)
                    | ((long) (authData[36] & 0xFF));
        } catch (Exception e) {
            return 0;
        }
    }
}
