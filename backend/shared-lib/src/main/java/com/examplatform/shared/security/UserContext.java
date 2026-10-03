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

package com.examplatform.shared.security;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Thread-local holder for the current request's authenticated {@link CurrentUser}.
 *
 * <p>Uses {@link InheritableThreadLocal} to enable seamless context propagation across
 * child threads and Loom virtual threads.
 */
public final class UserContext {

    /**
     * Immutable snapshot of the currently authenticated user.
     */
    public record CurrentUser(
            UUID userId,
            String email,
            String tenantId,
            Set<String> roles
    ) {
        public CurrentUser {
            roles = roles != null ? Collections.unmodifiableSet(roles) : Collections.emptySet();
        }
    }

    private static final InheritableThreadLocal<CurrentUser> HOLDER = new InheritableThreadLocal<>();

    private UserContext() {
        throw new AssertionError("UserContext must not be instantiated");
    }

    /**
     * Stores the given {@link CurrentUser} in the thread-local context.
     */
    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    /**
     * Helper to construct and set a {@link CurrentUser} in the thread-local context.
     */
    public static void set(UUID userId, String email, String tenantId, Set<String> roles) {
        HOLDER.set(new CurrentUser(userId, email, tenantId, roles));
    }

    /**
     * Retrieves the current user from context, or {@code null} if unauthenticated.
     */
    public static CurrentUser get() {
        return HOLDER.get();
    }

    /**
     * Retrieves the current user's UUID, or {@code null} if not authenticated.
     */
    public static UUID getUserId() {
        CurrentUser user = HOLDER.get();
        return user != null ? user.userId() : null;
    }

    /**
     * Retrieves the current user's UUID, throwing {@link IllegalStateException} if unauthenticated.
     */
    public static UUID getRequiredUserId() {
        UUID userId = getUserId();
        if (userId == null) {
            throw new IllegalStateException("No authenticated user present in UserContext");
        }
        return userId;
    }

    /**
     * Retrieves the current user's email address, or {@code null} if unauthenticated.
     */
    public static String getEmail() {
        CurrentUser user = HOLDER.get();
        return user != null ? user.email() : null;
    }

    /**
     * Retrieves the tenant ID associated with the current user, or {@code null} if unauthenticated.
     */
    public static String getTenantId() {
        CurrentUser user = HOLDER.get();
        return user != null ? user.tenantId() : null;
    }

    /**
     * Retrieves all assigned roles/authorities for the current user.
     */
    public static Set<String> getRoles() {
        CurrentUser user = HOLDER.get();
        return user != null && user.roles() != null ? user.roles() : Collections.emptySet();
    }

    /**
     * Checks if the current user holds the specified role (matching with or without "ROLE_" prefix).
     */
    public static boolean hasRole(String role) {
        CurrentUser user = HOLDER.get();
        if (user == null || user.roles() == null || role == null || role.isBlank()) {
            return false;
        }
        String normalizedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        String rawRole = role.startsWith("ROLE_") ? role.substring(5) : role;
        return user.roles().contains(role) || user.roles().contains(normalizedRole) || user.roles().contains(rawRole);
    }

    /**
     * Checks if the current user holds any of the specified roles.
     */
    public static boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a user is currently authenticated in context.
     */
    public static boolean isAuthenticated() {
        return HOLDER.get() != null;
    }

    /**
     * Clears the current user from context.
     */
    public static void clear() {
        HOLDER.remove();
    }
}
