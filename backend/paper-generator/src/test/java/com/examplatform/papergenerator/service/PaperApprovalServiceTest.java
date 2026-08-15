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

package com.examplatform.papergenerator.service;

import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.repository.PaperRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperApprovalServiceTest {

    @Mock
    private PaperRepository paperRepository;

    @Mock
    private VaultCryptoService vaultCryptoService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaperApprovalService paperApprovalService;

    private Paper draftPaper;
    private UUID paperId;

    @BeforeEach
    void setUp() {
        paperId = UUID.randomUUID();
        draftPaper = Paper.builder()
                .examId(UUID.randomUUID())
                .shiftId("shift-A")
                .status("DRAFT")
                .paperDefinitionJson("{\"questions\": []}")
                .difficultyScore(0.65)
                .build();
        // Use reflection to set id since BaseEntity.setId is protected
        try {
            var idField = draftPaper.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(draftPaper, paperId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Should transition DRAFT paper to ENCRYPTED after approval")
    void approvePaper_draftPaper_transitionsToEncrypted() {
        when(paperRepository.findById(paperId)).thenReturn(Optional.of(draftPaper));
        when(vaultCryptoService.encrypt(eq("paper-shift-shift-A"), anyString()))
                .thenReturn("vault:v1:encrypted_content");
        when(paperRepository.save(any(Paper.class))).thenAnswer(i -> i.getArgument(0));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        Paper result = paperApprovalService.approvePaper(paperId, "tenant-1");

        assertThat(result.getStatus()).isEqualTo("ENCRYPTED");
        assertThat(result.getEncryptedPackageRef()).isEqualTo("vault:v1:encrypted_content");
        assertThat(result.getEncryptionKeyId()).isEqualTo("paper-shift-shift-A");
    }

    @Test
    @DisplayName("Should reject approval of non-DRAFT paper")
    void approvePaper_approvedPaper_throwsException() {
        draftPaper.setStatus("APPROVED");
        when(paperRepository.findById(paperId)).thenReturn(Optional.of(draftPaper));

        assertThatThrownBy(() -> paperApprovalService.approvePaper(paperId, "tenant-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    @DisplayName("Should throw when paper not found")
    void approvePaper_paperNotFound_throwsException() {
        when(paperRepository.findById(paperId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paperApprovalService.approvePaper(paperId, "tenant-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Paper not found");
    }

    @Test
    @DisplayName("Should encrypt paper content with shift-specific key")
    void approvePaper_encryptsWithShiftKey() {
        when(paperRepository.findById(paperId)).thenReturn(Optional.of(draftPaper));
        when(vaultCryptoService.encrypt(eq("paper-shift-shift-A"), eq("{\"questions\": []}")))
                .thenReturn("vault:v1:abc123");
        when(paperRepository.save(any(Paper.class))).thenAnswer(i -> i.getArgument(0));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        paperApprovalService.approvePaper(paperId, "tenant-1");

        verify(vaultCryptoService).encrypt("paper-shift-shift-A", "{\"questions\": []}");
    }

    @Test
    @DisplayName("Should publish PAPER_APPROVED audit event")
    void approvePaper_publishesAuditEvent() {
        when(paperRepository.findById(paperId)).thenReturn(Optional.of(draftPaper));
        when(vaultCryptoService.encrypt(anyString(), anyString())).thenReturn("vault:v1:enc");
        when(paperRepository.save(any(Paper.class))).thenAnswer(i -> i.getArgument(0));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        paperApprovalService.approvePaper(paperId, "tenant-1");

        verify(kafkaTemplate).send(eq("exam.audit.events"), eq(paperId.toString()), any());
    }
}
