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

import com.examplatform.identity.domain.PersonalAccessToken;
import com.examplatform.identity.dto.CreatePersonalAccessTokenRequest;
import com.examplatform.identity.dto.PersonalAccessTokenResponse;
import com.examplatform.identity.repository.PersonalAccessTokenRepository;
import com.examplatform.shared.audit.AuditEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalAccessTokenService {

    private final PersonalAccessTokenRepository tokenRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public PersonalAccessTokenResponse createToken(UUID userId, CreatePersonalAccessTokenRequest request, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";

        // Generate 32 bytes of secure random data -> 64 hex characters
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawSecret = HexFormat.of().formatHex(randomBytes);
        String fullToken = "nag_pat_" + rawSecret;

        String prefix = fullToken.substring(0, 14) + "...";
        String tokenHash = hashToken(fullToken);

        int days = (request.getExpiresInDays() != null && request.getExpiresInDays() > 0)
                ? request.getExpiresInDays()
                : 90;
        Instant expiresAt = Instant.now().plus(Duration.ofDays(days));

        String scopesCsv = String.join(",", request.getScopes() != null ? request.getScopes() : List.of("admin:read"));

        PersonalAccessToken pat = PersonalAccessToken.builder()
                .userId(userId)
                .name(request.getName().trim())
                .tokenHash(tokenHash)
                .tokenPrefix(prefix)
                .scopes(scopesCsv)
                .ipWhitelist(request.getIpWhitelist() != null ? request.getIpWhitelist().trim() : null)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
        pat.setTenantId(effectiveTenant);

        PersonalAccessToken saved = tokenRepository.save(pat);

        auditEventPublisher.publish(
                AuditEventType.CONFIG_CHANGED,
                userId.toString(),
                "identity:tokens/" + saved.getId(),
                null, null,
                Map.of("action", "CREATE_PAT", "tokenName", request.getName(), "tenantId", effectiveTenant)
        );

        log.info("Created Personal Access Token id={} for user={} tenant={}", saved.getId(), userId, effectiveTenant);

        return PersonalAccessTokenResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .token(fullToken) // Plaintext secret displayed only once
                .tokenPrefix(saved.getTokenPrefix())
                .scopes(Arrays.asList(saved.getScopes().split(",")))
                .ipWhitelist(saved.getIpWhitelist())
                .expiresAt(saved.getExpiresAt())
                .lastUsedAt(saved.getLastUsedAt())
                .revoked(saved.isRevoked())
                .createdAt(saved.getCreatedAt() != null ? saved.getCreatedAt() : Instant.now())
                .build();
    }

    @Transactional(readOnly = true)
    public List<PersonalAccessTokenResponse> listTokens(UUID userId, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        List<PersonalAccessToken> tokens = tokenRepository
                .findAllByUserIdAndTenantIdOrderByCreatedAtDesc(userId, effectiveTenant);

        return tokens.stream()
                .map(t -> PersonalAccessTokenResponse.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .tokenPrefix(t.getTokenPrefix())
                        .scopes(Arrays.asList(t.getScopes().split(",")))
                        .ipWhitelist(t.getIpWhitelist())
                        .expiresAt(t.getExpiresAt())
                        .lastUsedAt(t.getLastUsedAt())
                        .revoked(t.isRevoked())
                        .createdAt(t.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void revokeToken(UUID tokenId, UUID userId, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        tokenRepository.findByIdAndUserIdAndTenantId(tokenId, userId, effectiveTenant)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    tokenRepository.save(token);
                    auditEventPublisher.publish(
                            AuditEventType.CONFIG_CHANGED,
                            userId.toString(),
                            "identity:tokens/" + tokenId,
                            null, null,
                            Map.of("action", "REVOKE_PAT", "tokenId", tokenId.toString(), "tenantId", effectiveTenant)
                    );
                    log.info("Revoked Personal Access Token id={} for user={} tenant={}", tokenId, userId, effectiveTenant);
                });
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not found", e);
        }
    }
}
