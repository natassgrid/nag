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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Public Ledger Adapter Tests")
class MockLedgerAdapterTest {

    private LedgerProperties properties;
    private MockLedgerAdapter ledgerAdapter;

    @BeforeEach
    void setUp() {
        properties = new LedgerProperties();
        properties.setExplorerBaseUrl("https://hashscan.io/testnet/transaction/");
        ledgerAdapter = new MockLedgerAdapter(properties);
    }

    @Test
    @DisplayName("Anchors paper payload and returns valid cryptographic receipt")
    void testAnchorPaper() {
        UUID examId = UUID.randomUUID();
        UUID paperId = UUID.randomUUID();
        String rootHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String manifestDigest = "d41d8cd98f00b204e9800998ecf8427e";

        LedgerAttestationPayload payload = LedgerAttestationPayload.builder()
                .examId(examId)
                .paperId(paperId)
                .variant("SET-A")
                .paperRootHash(rootHash)
                .manifestDigest(manifestDigest)
                .tenantId("default")
                .generatedAt(Instant.now())
                .build();

        LedgerAttestationReceipt receipt = ledgerAdapter.anchorPaper(payload);

        assertThat(receipt).isNotNull();
        assertThat(receipt.getTxHash()).startsWith("0x");
        assertThat(receipt.getConsensusTimestamp()).isNotBlank();
        assertThat(receipt.getBlockNumber()).isGreaterThan(0);
        assertThat(receipt.getLedgerExplorerUrl()).contains(receipt.getTxHash());
        assertThat(receipt.getStatus()).isEqualTo("CONFIRMED");

        // Verify lookup by examId and rootHash
        Optional<LedgerAttestationReceipt> verified = ledgerAdapter.verifyAnchoredPaper(examId, rootHash);
        assertTrue(verified.isPresent());
        assertThat(verified.get().getTxHash()).isEqualTo(receipt.getTxHash());
    }
}
