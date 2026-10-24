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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;

/**
 * Thread-local holder for the current request's authenticated {@link CurrentUser}.
 *
 * <p>Uses {@link InheritableThreadLocal} to enable seamless context propagation across
 * child threads and Loom virtual threads, with defensive fallback to {@link SecurityContextHolder}.
 */
public final class UserContext {

    /** Default fallback UUID used when an anonymous or unauthenticated actor must be recorded. */
    public static final UUID DEFAULT_SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    /**
     * Immutable snapshot of the currently authenticated user.
     */
    public record CurrentUser(
            UUID userId,
            String email,
            String tenantId,
            Set<String> roles,
            String preferredLanguage
    ) {
        public CurrentUser(UUID userId, String email, String tenantId, Set<String> roles) {
            this(userId, email, tenantId, roles, null);
        }

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
        HOLDER.set(new CurrentUser(userId, email, tenantId, roles, null));
    }

    /**
     * Helper to construct and set a {@link CurrentUser} with preferred language in the thread-local context.
     */
    public static void set(UUID userId, String email, String tenantId, Set<String> roles, String preferredLanguage) {
        HOLDER.set(new CurrentUser(userId, email, tenantId, roles, preferredLanguage));
    }

    /**
     * Retrieves the current user from context, or {@code null} if unauthenticated.
     */
    public static CurrentUser get() {
        return HOLDER.get();
    }

    /**
     * Retrieves the current user's UUID, or {@code null} if unauthenticated.
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
     * Resolves the current user's UUID, falling back to the given Jwt parameter or {@code null}.
     */
    public static UUID resolveUserId(Jwt jwt) {
        UUID userId = getUserId();
        if (userId != null) {
            return userId;
        }
        if (jwt != null && jwt.getSubject() != null) {
            return parseUuidQuietly(jwt.getSubject());
        }
        return resolveUserIdFromSecurityContext();
    }

    /**
     * Resolves the current user's UUID, falling back to the given Jwt parameter or fallback UUID.
     */
    public static UUID resolveUserId(Jwt jwt, UUID fallback) {
        UUID resolved = resolveUserId(jwt);
        return resolved != null ? resolved : fallback;
    }

    /**
     * Resolves caller user ID falling back to {@link #DEFAULT_SYSTEM_USER_ID}.
     */
    public static UUID resolveUserIdOrDefault(Jwt jwt) {
        return resolveUserId(jwt, DEFAULT_SYSTEM_USER_ID);
    }

    /**
     * Resolves caller user ID as string (from UserContext, Jwt, SecurityContext, or "user-unknown").
     */
    public static String resolveUserIdStr(Jwt jwt) {
        UUID userId = resolveUserId(jwt);
        if (userId != null) {
            return userId.toString();
        }
        if (jwt != null && jwt.getSubject() != null && !jwt.getSubject().isBlank()) {
            return jwt.getSubject();
        }
        return "user-unknown";
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
     * Retrieves the preferred language code associated with the current user, or {@code null}.
     */
    public static String getPreferredLanguage() {
        CurrentUser user = HOLDER.get();
        return user != null ? user.preferredLanguage() : null;
    }

    /**
     * Retrieves all assigned roles/authorities for the current user.
     */
    public static Set<String> getRoles() {
        CurrentUser user = HOLDER.get();
        return user != null && user.roles() != null ? user.roles() : Collections.emptySet();
    }

    /**
     * Resolves caller roles from UserContext, falling back to Jwt claims.
     */
    @SuppressWarnings("unchecked")
    public static Set<String> resolveRoles(Jwt jwt) {
        Set<String> roles = getRoles();
        if (roles != null && !roles.isEmpty()) {
            return roles;
        }
        if (jwt != null) {
            try {
                Object realmAccess = jwt.getClaim("realm_access");
                if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> coll) {
                    Set<String> extracted = new HashSet<>();
                    for (Object o : coll) {
                        if (o != null && !o.toString().isBlank()) {
                            extracted.add(o.toString());
                        }
                    }
                    return Collections.unmodifiableSet(extracted);
                }
            } catch (Exception ignored) {
            }
        }
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
                Set<String> extracted = new HashSet<>();
                for (GrantedAuthority ga : auth.getAuthorities()) {
                    if (ga != null && ga.getAuthority() != null) {
                        String authStr = ga.getAuthority();
                        extracted.add(authStr);
                        extracted.add(authStr.replaceFirst("^ROLE_", ""));
                        extracted.add("ROLE_" + authStr.replaceFirst("^ROLE_", ""));
                    }
                }
                return Collections.unmodifiableSet(extracted);
            }
        } catch (NoClassDefFoundError | Exception ignored) {
        }
        return Collections.emptySet();
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
        if (roles == null) {
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

    private static UUID resolveUserIdFromSecurityContext() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return null;
            }
            if (auth.getPrincipal() instanceof Jwt jwt && jwt.getSubject() != null) {
                return parseUuidQuietly(jwt.getSubject());
            }
            if (auth.getName() != null) {
                return parseUuidQuietly(auth.getName());
            }
        } catch (NoClassDefFoundError | Exception ignored) {
        }
        return null;
    }

    private static UUID parseUuidQuietly(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
