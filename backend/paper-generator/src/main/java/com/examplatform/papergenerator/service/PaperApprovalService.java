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
import com.examplatform.shared.messaging.EventPublisher;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Manages paper approval workflow: DRAFT → APPROVED → ENCRYPTED.
 * Encrypts the paper package with a shift-specific key via VaultCryptoService,
 * anchors the binary SHA-256 Merkle root to the public DLT ledger via PaperAnchoringService,
 * and publishes audit events.
 *
 * Validates: Requirements 8.7, Issue #156
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaperApprovalService {

    private static final String AUDIT_TOPIC = "exam.audit.events";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_ENCRYPTED = "ENCRYPTED";

    private final PaperRepository paperRepository;
    private final VaultCryptoService vaultCryptoService;
    private final PaperAnchoringService paperAnchoringService;
    private final EventPublisher eventPublisher;

    /**
     * Approve and encrypt a paper, transitioning:
     * DRAFT → APPROVED → ENCRYPTED, followed by public ledger anchoring.
     *
     * @param paperId  the paper to approve
     * @param tenantId examination authority identifier
     * @return the updated Paper entity in ENCRYPTED status with public ledger proofs
     */
    public Paper approvePaper(UUID paperId, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";

        Paper paper = paperRepository.findByIdAndTenantId(paperId, effectiveTenant)
                .or(() -> paperRepository.findById(paperId))
                .orElseThrow(() -> new EntityNotFoundException("Paper not found: " + paperId));

        // Validate current state is DRAFT
        if (!STATUS_DRAFT.equals(paper.getStatus())) {
            throw new IllegalStateException(
                    "Paper must be in DRAFT status to approve. Current: " + paper.getStatus());
        }

        // Transition to APPROVED
        paper.setStatus(STATUS_APPROVED);
        paperRepository.save(paper);
        log.info("Paper {} transitioned to APPROVED", paperId);

        // Encrypt paper package with shift-specific key
        String shiftKeyName = "paper-shift-" + paper.getShiftId();
        String paperContent = paper.getPaperDefinitionJson();

        if (paperContent != null && !paperContent.isBlank()) {
            String encryptedRef = vaultCryptoService.encrypt(shiftKeyName, paperContent);
            paper.setEncryptedPackageRef(encryptedRef);
            paper.setEncryptionKeyId(shiftKeyName);
        }

        // Transition to ENCRYPTED
        paper.setStatus(STATUS_ENCRYPTED);
        Paper encryptedPaper = paperRepository.save(paper);
        log.info("Paper {} encrypted with key [{}] and transitioned to ENCRYPTED", paperId, shiftKeyName);

        // Cryptographic Public Ledger Anchoring (Issue #156)
        Paper anchoredPaper = paperAnchoringService.anchorPaperToLedger(encryptedPaper, effectiveTenant);

        // Publish PAPER_APPROVED audit event
        publishPaperApprovedEvent(anchoredPaper, effectiveTenant);

        return anchoredPaper;
    }

    private void publishPaperApprovedEvent(Paper paper, String tenantId) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", "PAPER_APPROVED",
                    "paperId", paper.getId().toString(),
                    "examId", paper.getExamId().toString(),
                    "shiftId", paper.getShiftId(),
                    "paperRootHash", paper.getPaperRootHash() != null ? paper.getPaperRootHash() : "",
                    "ledgerTxHash", paper.getLedgerTxHash() != null ? paper.getLedgerTxHash() : "",
                    "encryptionKeyId", paper.getEncryptionKeyId() != null ? paper.getEncryptionKeyId() : "",
                    "tenantId", tenantId,
                    "occurredAt", Instant.now().toString()
            );
            eventPublisher.publish(AUDIT_TOPIC, paper.getId().toString(), event);
        } catch (Exception e) {
            log.error("Unexpected error publishing PAPER_APPROVED audit event: {}", e.getMessage());
        }
    }
}
