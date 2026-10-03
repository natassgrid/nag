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

package com.examplatform.papergenerator.ledger;

import com.examplatform.papergenerator.crypto.MerkleTree;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Deterministic In-Memory Mock Ledger Adapter for local development, CI/CD, and integration tests.
 * Implements full cryptographic simulation of Hedera Consensus Service / DLT with transaction hashing,
 * consensus timestamps, and receipt persistence.
 *
 * Validates: Issue #156
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "nag.ledger.provider", havingValue = "MOCK", matchIfMissing = true)
@RequiredArgsConstructor
public class MockLedgerAdapter implements LedgerAdapter {

    private final LedgerProperties properties;
    private final Map<String, LedgerAttestationReceipt> attestationStore = new ConcurrentHashMap<>();
    private final AtomicLong blockCounter = new AtomicLong(1_849_000L);

    @Override
    public LedgerAttestationReceipt anchorPaper(LedgerAttestationPayload payload) {
        log.info("Anchoring paper to Mock Ledger: examId={}, variant={}, rootHash={}",
                payload.getExamId(), payload.getVariant(), payload.getPaperRootHash());

        long block = blockCounter.incrementAndGet();
        Instant now = Instant.now();
        String consensusTimestamp = String.format("%d.%09d", now.getEpochSecond(), now.getNano());

        String txPayload = String.format("%s:%s:%s:%s:%s",
                payload.getExamId(),
                payload.getVariant(),
                payload.getPaperRootHash(),
                payload.getManifestDigest(),
                consensusTimestamp);
        String txHash = "0x" + MerkleTree.sha256Hex(txPayload);

        String explorerUrl = properties.getExplorerBaseUrl() + txHash;

        LedgerAttestationReceipt receipt = LedgerAttestationReceipt.builder()
                .txHash(txHash)
                .consensusTimestamp(consensusTimestamp)
                .blockNumber(block)
                .ledgerExplorerUrl(explorerUrl)
                .network(getNetworkName())
                .status("CONFIRMED")
                .paperRootHash(payload.getPaperRootHash())
                .manifestDigest(payload.getManifestDigest())
                .anchoredAt(now)
                .build();

        String key = buildStoreKey(payload.getExamId(), payload.getPaperRootHash());
        attestationStore.put(key, receipt);

        // Also store by paperId if present
        if (payload.getPaperId() != null) {
            attestationStore.put("PAPER:" + payload.getPaperId(), receipt);
        }
        attestationStore.put("HASH:" + payload.getPaperRootHash(), receipt);

        log.info("Paper successfully anchored to Mock Ledger: txHash={}, block={}, consensusTs={}",
                txHash, block, consensusTimestamp);

        return receipt;
    }

    @Override
    public Optional<LedgerAttestationReceipt> verifyAnchoredPaper(UUID examId, String paperRootHash) {
        if (paperRootHash == null || paperRootHash.isBlank()) {
            return Optional.empty();
        }

        if (examId != null) {
            String key = buildStoreKey(examId, paperRootHash);
            LedgerAttestationReceipt receipt = attestationStore.get(key);
            if (receipt != null) {
                return Optional.of(receipt);
            }
        }

        LedgerAttestationReceipt byHash = attestationStore.get("HASH:" + paperRootHash);
        return Optional.ofNullable(byHash);
    }

    @Override
    public String getNetworkName() {
        return "NAG_MOCK_HEDERA_TESTNET";
    }

    private String buildStoreKey(UUID examId, String paperRootHash) {
        return String.format("%s:%s", examId != null ? examId.toString() : "NONE", paperRootHash);
    }
}
