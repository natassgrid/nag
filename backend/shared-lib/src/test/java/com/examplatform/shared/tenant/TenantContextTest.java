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

package com.examplatform.shared.tenant;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantContext")
class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("set and get")
    class SetAndGet {

        @Test
        @DisplayName("returns null before any value is set")
        void returnsNullByDefault() {
            assertThat(TenantContext.getTenantId()).isNull();
        }

        @Test
        @DisplayName("returns the value that was set")
        void returnsSetValue() {
            TenantContext.setTenantId("tenant-abc");
            assertThat(TenantContext.getTenantId()).isEqualTo("tenant-abc");
        }

        @Test
        @DisplayName("clears the value")
        void clearsValue() {
            TenantContext.setTenantId("tenant-xyz");
            TenantContext.clear();
            assertThat(TenantContext.getTenantId()).isNull();
        }
    }

    @Nested
    @DisplayName("thread isolation")
    class ThreadIsolation {

        @Test
        @DisplayName("different threads have independent tenant values")
        void differentThreadsHaveIndependentValues() throws InterruptedException {
            TenantContext.setTenantId("tenant-main");
            String[] otherThreadValue = {null};

            Thread other = Thread.ofVirtual().start(() -> {
                // Virtual thread should NOT inherit (InheritableThreadLocal propagates
                // to child threads started FROM a thread with a value; here we start
                // from test thread but then overwrite)
                TenantContext.setTenantId("tenant-other");
                otherThreadValue[0] = TenantContext.getTenantId();
                TenantContext.clear();
            });
            other.join();

            assertAll(
                () -> assertThat(TenantContext.getTenantId()).isEqualTo("tenant-main"),
                () -> assertThat(otherThreadValue[0]).isEqualTo("tenant-other")
            );
        }
    }
}
