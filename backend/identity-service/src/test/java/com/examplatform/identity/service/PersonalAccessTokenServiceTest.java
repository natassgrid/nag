/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.identity.service;

import com.examplatform.identity.domain.PersonalAccessToken;
import com.examplatform.identity.dto.CreatePersonalAccessTokenRequest;
import com.examplatform.identity.dto.PersonalAccessTokenResponse;
import com.examplatform.identity.repository.PersonalAccessTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalAccessTokenServiceTest {

    @Mock
    private PersonalAccessTokenRepository tokenRepository;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    private PersonalAccessTokenService tokenService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        tokenService = new PersonalAccessTokenService(tokenRepository, auditEventPublisher);
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should create personal access token with SHA-256 hash and return raw secret only once")
    void shouldCreatePersonalAccessToken() {
        when(tokenRepository.save(any(PersonalAccessToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreatePersonalAccessTokenRequest req = CreatePersonalAccessTokenRequest.builder()
                .name("CI/CD Pipeline")
                .scopes(List.of("READ", "WRITE"))
                .expiresInDays(30)
                .ipWhitelist("10.0.0.1/24")
                .build();

        PersonalAccessTokenResponse result = tokenService.createToken(userId, req, "tenant-01");

        assertThat(result).isNotNull();
        assertThat(result.getToken()).startsWith("nag_pat_");
        assertThat(result.getName()).isEqualTo("CI/CD Pipeline");
        assertThat(result.getScopes()).containsExactly("READ", "WRITE");
        assertThat(result.getIpWhitelist()).isEqualTo("10.0.0.1/24");
        assertThat(result.getExpiresAt()).isNotNull();

        ArgumentCaptor<PersonalAccessToken> captor = ArgumentCaptor.forClass(PersonalAccessToken.class);
        verify(tokenRepository).save(captor.capture());
        PersonalAccessToken saved = captor.getValue();
        assertThat(saved.getTokenHash()).isNotEqualTo(result.getToken());
        assertThat(saved.getTokenHash()).hasSize(64); // SHA-256 hex string length
    }

    @Test
    @DisplayName("Should list active and revoked personal access tokens for user")
    void shouldListTokensForUser() {
        PersonalAccessToken pat = PersonalAccessToken.builder()
                .userId(userId)
                .name("Test Token")
                .tokenPrefix("nag_pat_12345678")
                .scopes("READ,WRITE")
                .revoked(false)
                .build();
        pat.setTenantId("tenant-01");

        when(tokenRepository.findAllByUserIdAndTenantIdOrderByCreatedAtDesc(userId, "tenant-01"))
                .thenReturn(List.of(pat));

        List<PersonalAccessTokenResponse> tokens = tokenService.listTokens(userId, "tenant-01");
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getName()).isEqualTo("Test Token");
        assertThat(tokens.get(0).getScopes()).containsExactly("READ", "WRITE");
    }

    @Test
    @DisplayName("Should revoke token successfully")
    void shouldRevokeToken() {
        UUID tokenId = UUID.randomUUID();

        PersonalAccessToken pat = PersonalAccessToken.builder()
                .userId(userId)
                .name("Token to revoke")
                .tokenPrefix("nag_pat_test")
                .scopes("READ")
                .revoked(false)
                .build();
        pat.setTenantId("tenant-01");

        when(tokenRepository.findByIdAndUserIdAndTenantId(tokenId, userId, "tenant-01"))
                .thenReturn(Optional.of(pat));

        tokenService.revokeToken(tokenId, userId, "tenant-01");

        assertThat(pat.isRevoked()).isTrue();
        verify(tokenRepository).save(pat);
    }
}
