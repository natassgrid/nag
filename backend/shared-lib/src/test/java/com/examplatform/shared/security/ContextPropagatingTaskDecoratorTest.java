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

package com.examplatform.shared.security;

import com.examplatform.shared.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContextPropagatingTaskDecorator Unit Tests")
class ContextPropagatingTaskDecoratorTest {

    private final ContextPropagatingTaskDecorator decorator = new ContextPropagatingTaskDecorator();

    @AfterEach
    void tearDown() {
        UserContext.clear();
        TenantContext.clear();
    }

    @Test
    @DisplayName("Propagates UserContext and TenantContext to asynchronous worker thread")
    void propagatesContextAcrossThreadPool() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            // Warm up thread in pool while context is clean so InheritableThreadLocal is empty
            pool.submit(() -> {}).get(5, TimeUnit.SECONDS);

            UUID userId = UUID.randomUUID();
            UserContext.set(userId, "worker@exam.gov.in", "tenant-alpha", Set.of("ROLE_ADMIN"), "en");
            TenantContext.set("tenant-alpha");

            AtomicReference<UUID> observedUserId = new AtomicReference<>();
            AtomicReference<String> observedTenantId = new AtomicReference<>();
            AtomicReference<String> observedLang = new AtomicReference<>();

            Runnable task = decorator.decorate(() -> {
                observedUserId.set(UserContext.getUserId());
                observedTenantId.set(TenantContext.getTenantId());
                observedLang.set(UserContext.getPreferredLanguage());
            });

            Future<?> future = pool.submit(task);
            future.get(5, TimeUnit.SECONDS);

            assertThat(observedUserId.get()).isEqualTo(userId);
            assertThat(observedTenantId.get()).isEqualTo("tenant-alpha");
            assertThat(observedLang.get()).isEqualTo("en");

            // Verify thread in pool does not retain context after task execution finishes
            Future<Boolean> cleanupCheck = pool.submit(() ->
                    UserContext.get() == null && TenantContext.getTenantId() == null);
            assertThat(cleanupCheck.get(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("Propagates context through static Callable helper")
    void propagatesCallable() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            UUID userId = UUID.randomUUID();
            UserContext.set(userId, "call@exam.gov.in", "tenant-beta", Set.of("ROLE_AUTHOR"));
            TenantContext.set("tenant-beta");

            Callable<String> callable = ContextPropagatingTaskDecorator.propagateCallable(() ->
                    UserContext.getRequiredUserId().toString() + ":" + TenantContext.getRequiredTenantId());

            String result = pool.submit(callable).get(5, TimeUnit.SECONDS);
            assertThat(result).isEqualTo(userId.toString() + ":tenant-beta");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("Propagates context across CompletableFuture using Supplier helper")
    void propagatesSupplier() throws Exception {
        UUID userId = UUID.randomUUID();
        UserContext.set(userId, "supplier@exam.gov.in", "tenant-gamma", Set.of("ROLE_CANDIDATE"));
        TenantContext.set("tenant-gamma");

        CompletableFuture<String> future = CompletableFuture.supplyAsync(
                ContextPropagatingTaskDecorator.propagateSupplier(() ->
                        UserContext.getUserId().toString() + "@" + TenantContext.getTenantId())
        );

        String result = future.get(5, TimeUnit.SECONDS);
        assertThat(result).isEqualTo(userId.toString() + "@tenant-gamma");
    }
}
