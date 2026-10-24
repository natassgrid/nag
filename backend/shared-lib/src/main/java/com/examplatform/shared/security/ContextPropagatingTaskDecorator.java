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
import org.jspecify.annotations.NonNull;
import org.springframework.core.task.TaskDecorator;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Task decorator that snapshots {@link UserContext}, {@link TenantContext}, and
 * {@link SecurityContextHolder} from the caller thread and restores them onto
 * the asynchronous execution thread, restoring previous thread state in a finally block.
 *
 * <p>Enables context safety across {@code @Async}, virtual threads, thread pools,
 * and event listener boundaries without thread-local leakage.
 */
public class ContextPropagatingTaskDecorator implements TaskDecorator {

    @Override
    public @NonNull Runnable decorate(@NonNull Runnable runnable) {
        return propagate(runnable);
    }

    /**
     * Wraps a {@link Runnable} to capture and propagate current context across thread boundaries.
     */
    public static @NonNull Runnable propagate(@NonNull Runnable runnable) {
        UserContext.CurrentUser userContext = UserContext.get();
        String tenantId = TenantContext.getTenantId();
        SecurityContext securityContext = SecurityContextHolder.getContext();

        return () -> {
            UserContext.CurrentUser previousUser = UserContext.get();
            String previousTenant = TenantContext.getTenantId();
            SecurityContext previousSecurityContext = SecurityContextHolder.getContext();

            try {
                if (userContext != null) {
                    UserContext.set(userContext);
                } else {
                    UserContext.clear();
                }

                if (tenantId != null && !tenantId.isBlank()) {
                    TenantContext.set(tenantId);
                } else {
                    TenantContext.clear();
                }

                if (securityContext != null && securityContext.getAuthentication() != null) {
                    SecurityContextHolder.setContext(securityContext);
                }

                runnable.run();
            } finally {
                if (previousUser != null) {
                    UserContext.set(previousUser);
                } else {
                    UserContext.clear();
                }

                if (previousTenant != null && !previousTenant.isBlank()) {
                    TenantContext.set(previousTenant);
                } else {
                    TenantContext.clear();
                }

                if (previousSecurityContext != null && previousSecurityContext.getAuthentication() != null) {
                    SecurityContextHolder.setContext(previousSecurityContext);
                } else {
                    SecurityContextHolder.clearContext();
                }
            }
        };
    }

    /**
     * Wraps a {@link Callable} to capture and propagate current context across thread boundaries.
     */
    public static <T> @NonNull Callable<T> propagateCallable(@NonNull Callable<T> callable) {
        UserContext.CurrentUser userContext = UserContext.get();
        String tenantId = TenantContext.getTenantId();
        SecurityContext securityContext = SecurityContextHolder.getContext();

        return () -> {
            UserContext.CurrentUser previousUser = UserContext.get();
            String previousTenant = TenantContext.getTenantId();
            SecurityContext previousSecurityContext = SecurityContextHolder.getContext();

            try {
                if (userContext != null) {
                    UserContext.set(userContext);
                } else {
                    UserContext.clear();
                }

                if (tenantId != null && !tenantId.isBlank()) {
                    TenantContext.set(tenantId);
                } else {
                    TenantContext.clear();
                }

                if (securityContext != null && securityContext.getAuthentication() != null) {
                    SecurityContextHolder.setContext(securityContext);
                }

                return callable.call();
            } finally {
                if (previousUser != null) {
                    UserContext.set(previousUser);
                } else {
                    UserContext.clear();
                }

                if (previousTenant != null && !previousTenant.isBlank()) {
                    TenantContext.set(previousTenant);
                } else {
                    TenantContext.clear();
                }

                if (previousSecurityContext != null && previousSecurityContext.getAuthentication() != null) {
                    SecurityContextHolder.setContext(previousSecurityContext);
                } else {
                    SecurityContextHolder.clearContext();
                }
            }
        };
    }

    /**
     * Wraps a {@link Supplier} to capture and propagate current context across thread boundaries.
     */
    public static <T> @NonNull Supplier<T> propagateSupplier(@NonNull Supplier<T> supplier) {
        Callable<T> callable = supplier::get;
        Callable<T> wrapped = propagateCallable(callable);
        return () -> {
            try {
                return wrapped.call();
            } catch (IllegalStateException ise) {
                throw ise;
            } catch (Exception e) {
                throw new IllegalStateException("Context-propagated task execution failed", e);
            }
        };
    }
}
