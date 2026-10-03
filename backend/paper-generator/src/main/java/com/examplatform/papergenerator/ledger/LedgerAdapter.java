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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.\n */

package com.examplatform.papergenerator.ledger;

import java.util.Optional;
import java.util.UUID;

/**
 * Interface for decentralized ledger anchoring adapters (Hedera HCS, Ethereum L2, Mock).
 *
 * Validates: Issue #156
 */
public interface LedgerAdapter {

    /**
     * Submits an immutable paper attestation transaction to the ledger.
     */
    LedgerAttestationReceipt anchorPaper(LedgerAttestationPayload payload);

    /**
     * Queries the ledger for an existing attestation by examId and paperRootHash.
     */
    Optional<LedgerAttestationReceipt> verifyAnchoredPaper(UUID examId, String paperRootHash);

    /**
     * Returns the name of the ledger network (e.g., "HEDERA_TESTNET", "MOCK_DLT", "ETHEREUM_SEPOLIA").
     */
    String getNetworkName();
}
