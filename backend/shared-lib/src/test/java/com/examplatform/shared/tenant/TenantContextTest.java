/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

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
            assertThat(TenantContext.getRequiredTenantId()).isEqualTo("tenant-abc");
            assertThat(TenantContext.getRequired()).isEqualTo("tenant-abc");
        }

        @Test
        @DisplayName("throws IllegalStateException on getRequiredTenantId when not set")
        void throwsOnRequiredWhenNull() {
            assertThatThrownBy(TenantContext::getRequiredTenantId)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No tenantId bound to the current thread");
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

    @Nested
    @DisplayName("resolution helpers")
    class ResolutionHelpers {

        @Test
        @DisplayName("resolveTenant returns header when present")
        void resolveTenantHeaderPresent() {
            TenantContext.set("ambient-tenant");
            assertThat(TenantContext.resolveTenant("header-tenant")).isEqualTo("header-tenant");
        }

        @Test
        @DisplayName("resolveTenant returns ambient context when header is empty")
        void resolveTenantAmbientFallback() {
            TenantContext.set("ambient-tenant");
            assertThat(TenantContext.resolveTenant(null)).isEqualTo("ambient-tenant");
            assertThat(TenantContext.resolveTenant("")).isEqualTo("ambient-tenant");
        }

        @Test
        @DisplayName("resolveTenant falls back to default tenant when both empty")
        void resolveTenantDefaultFallback() {
            assertThat(TenantContext.resolveTenant(null)).isEqualTo("default");
        }

        @Test
        @DisplayName("resolveRequiredTenant throws IllegalArgumentException when missing")
        void resolveRequiredTenantThrows() {
            assertThatThrownBy(() -> TenantContext.resolveRequiredTenant(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Missing required request header: X-Tenant-Id");
        }

        @Test
        @DisplayName("resolveRequiredTenant succeeds when header or ambient context is present")
        void resolveRequiredTenantSuccess() {
            assertThat(TenantContext.resolveRequiredTenant("explicit-tenant")).isEqualTo("explicit-tenant");
            TenantContext.set("context-tenant");
            assertThat(TenantContext.resolveRequiredTenant(null)).isEqualTo("context-tenant");
        }
    }
}
