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

package com.examplatform.audit.service;

import com.examplatform.audit.domain.AuditEvent;
import com.examplatform.audit.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuditIngestionService}.
 * Validates: Requirements 15.2, 15.4
 */
@ExtendWith(MockitoExtension.class)
class AuditIngestionServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private VaultCryptoService vaultCryptoService;

    @Captor
    private ArgumentCaptor<AuditEvent> eventCaptor;

    private AuditIngestionService auditIngestionService;

    @BeforeEach
    void setUp() {
        auditIngestionService = new AuditIngestionService(auditEventRepository, vaultCryptoService);
    }

    @Test
    @DisplayName("ingest() computes SHA-256 of payload and persists with correct hash")
    void ingest_computesSha256AndPersists() {
        // Given
        String payload = "{\"action\":\"LOGIN\",\"userId\":\"user-123\"}";
        String expectedHash = computeSha256(payload);
        String fakeSignature = "vault:v1:MEUCIQDfakeSignature==";
        UUID actorId = UUID.randomUUID();

        when(vaultCryptoService.sign(eq("audit-signing-key-ecdsa-p256"), eq(expectedHash)))
                .thenReturn(fakeSignature);
        when(auditEventRepository.save(any(AuditEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AuditEvent result = auditIngestionService.ingest(
                payload, "LOGIN", actorId.toString(),
                "identity:session", "192.168.1.1", "fp-abc123", "tenant-1");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPayloadHash()).isEqualTo(expectedHash);
        assertThat(result.getEventType()).isEqualTo("LOGIN");
        assertThat(result.getActorId()).isEqualTo(actorId);
        assertThat(result.getResource()).isEqualTo("identity:session");
        assertThat(result.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(result.getDeviceFingerprint()).isEqualTo("fp-abc123");
        assertThat(result.getTenantId()).isEqualTo("tenant-1");
        assertThat(result.getEventPayload()).isEqualTo(payload);
    }

    @Test
    @DisplayName("ingest() signs the payload hash using VaultCryptoService")
    void ingest_signsPayloadHashWithVault() {
        // Given
        String payload = "{\"action\":\"ROLE_CHANGE\"}";
        String expectedHash = computeSha256(payload);
        String expectedSignature = "vault:v1:MEYCIQDrealSignature==";
        UUID actorId = UUID.randomUUID();

        when(vaultCryptoService.sign("audit-signing-key-ecdsa-p256", expectedHash))
                .thenReturn(expectedSignature);
        when(auditEventRepository.save(any(AuditEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AuditEvent result = auditIngestionService.ingest(
                payload, "ROLE_CHANGE", actorId.toString(),
                "identity:role", null, null, "tenant-2");

        // Then
        assertThat(result.getHsmSignature()).isEqualTo(expectedSignature);
        assertThat(result.getSigningKeyId()).isEqualTo("audit-signing-key-ecdsa-p256");
        verify(vaultCryptoService).sign("audit-signing-key-ecdsa-p256", expectedHash);
    }

    @Test
    @DisplayName("ingest() persists event via repository save()")
    void ingest_persistsViaRepository() {
        // Given
        String payload = "{\"action\":\"DENIED_ACCESS\"}";
        UUID actorId = UUID.randomUUID();

        when(vaultCryptoService.sign(any(), any())).thenReturn("sig");
        when(auditEventRepository.save(any(AuditEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auditIngestionService.ingest(
                payload, "DENIED_ACCESS", actorId.toString(),
                "exam:paper:123", "10.0.0.1", null, "tenant-3");

        // Then
        verify(auditEventRepository).save(eventCaptor.capture());
        AuditEvent captured = eventCaptor.getValue();
        assertThat(captured.getEventType()).isEqualTo("DENIED_ACCESS");
        assertThat(captured.getActorId()).isEqualTo(actorId);
        assertThat(captured.getSigningKeyId()).isEqualTo("audit-signing-key-ecdsa-p256");
    }

    @Test
    @DisplayName("recordTamperAttempt() creates TAMPER_ATTEMPT event type with target event reference")
    void recordTamperAttempt_createsTamperAttemptEvent() {
        // Given
        UUID targetEventId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        String tenantId = "tenant-security";

        when(vaultCryptoService.sign(any(), any())).thenReturn("tamper-sig");
        when(auditEventRepository.save(any(AuditEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AuditEvent result = auditIngestionService.recordTamperAttempt(
                targetEventId, actorId.toString(), tenantId);

        // Then
        assertThat(result.getEventType()).isEqualTo("TAMPER_ATTEMPT");
        assertThat(result.getResource()).isEqualTo("audit:event:" + targetEventId);
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.getEventPayload()).contains(targetEventId.toString());
        assertThat(result.getEventPayload()).contains(actorId.toString());
        assertThat(result.getEventPayload()).contains("TAMPER_ATTEMPT");
    }

    @Test
    @DisplayName("payloadHash matches independently computed SHA-256 of input")
    void ingest_payloadHashMatchesSha256() {
        // Given
        String payload = "任意のテスト文字列 with special chars: <>&\"'\\n\\t";
        String expectedHash = computeSha256(payload);
        UUID actorId = UUID.randomUUID();

        when(vaultCryptoService.sign(any(), any())).thenReturn("sig");
        when(auditEventRepository.save(any(AuditEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AuditEvent result = auditIngestionService.ingest(
                payload, "TEST", actorId.toString(),
                "test:resource", null, null, "tenant-x");

        // Then
        assertThat(result.getPayloadHash()).isEqualTo(expectedHash);
        assertThat(result.getPayloadHash()).hasSize(64); // SHA-256 produces 64 hex chars
    }

    /**
     * Helper: compute SHA-256 hex digest for verification in tests.
     */
    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
