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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for public ledger anchoring.
 *
 * Validates: Issue #156
 */
@Data
@Component
@ConfigurationProperties(prefix = "nag.ledger")
public class LedgerProperties {

    /**
     * Whether public ledger anchoring is enabled.
     */
    private boolean enabled = true;

    /**
     * Provider mode: MOCK, HEDERA, ETHEREUM.
     */
    private String provider = "MOCK";

    /**
     * Base explorer URL template for viewing transactions.
     */
    private String explorerBaseUrl = "https://hashscan.io/testnet/transaction/";

    /**
     * Hedera Topic ID or Ethereum Contract Address.
     */
    private String topicOrContractId = "0.0.4829104";

    /**
     * Maximum retry attempts for ledger attestation submissions.
     */
    private int maxRetries = 3;

    /**
     * Initial backoff delay in milliseconds.
     */
    private long retryBackoffMs = 100;
}
