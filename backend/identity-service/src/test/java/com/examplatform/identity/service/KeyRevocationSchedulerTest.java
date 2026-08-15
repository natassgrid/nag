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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link KeyRevocationScheduler}.
 *
 * Validates: Requirements 16.4, 16.5
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KeyRevocationScheduler")
class KeyRevocationSchedulerTest {

    @Mock
    private VaultCryptoService vaultCryptoService;

    private KeyRevocationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new KeyRevocationScheduler(vaultCryptoService);
    }

    @Nested
    @DisplayName("scheduleRevocation")
    class ScheduleRevocation {

        @Test
        @DisplayName("adds key to queue and increments pending count")
        void addsKeyToQueueAndIncrementsPendingCount() {
            scheduler.scheduleRevocation("key-1");

            assertThat(scheduler.pendingCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("multiple keys are queued independently")
        void multipleKeysAreQueuedIndependently() {
            scheduler.scheduleRevocation("key-1");
            scheduler.scheduleRevocation("key-2");
            scheduler.scheduleRevocation("key-3");

            assertThat(scheduler.pendingCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("processPendingRevocations")
    class ProcessPendingRevocations {

        @Test
        @DisplayName("calls revokeKey for each queued key")
        void callsRevokeKeyForEachQueuedKey() {
            scheduler.scheduleRevocation("key-alpha");
            scheduler.scheduleRevocation("key-beta");

            scheduler.processPendingRevocations();

            verify(vaultCryptoService).revokeKey("key-alpha");
            verify(vaultCryptoService).revokeKey("key-beta");
        }

        @Test
        @DisplayName("empties the queue after processing")
        void emptiesTheQueueAfterProcessing() {
            scheduler.scheduleRevocation("key-1");
            scheduler.scheduleRevocation("key-2");

            scheduler.processPendingRevocations();

            assertThat(scheduler.pendingCount()).isZero();
        }

        @Test
        @DisplayName("does nothing when queue is empty")
        void doesNothingWhenQueueIsEmpty() {
            scheduler.processPendingRevocations();

            verifyNoInteractions(vaultCryptoService);
        }

        @Test
        @DisplayName("failed revocation does not block other revocations")
        void failedRevocationDoesNotBlockOtherRevocations() {
            doThrow(new RuntimeException("Vault error")).when(vaultCryptoService).revokeKey("bad-key");
            scheduler.scheduleRevocation("bad-key");
            scheduler.scheduleRevocation("good-key");

            scheduler.processPendingRevocations();

            verify(vaultCryptoService).revokeKey("bad-key");
            verify(vaultCryptoService).revokeKey("good-key");
            assertThat(scheduler.pendingCount()).isZero();
        }
    }
}
