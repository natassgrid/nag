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

import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.security.UserContext;

/**
 * Thread-local holder for the current request's {@code tenantId}.
 *
 * <p>The tenant identifier (examination authority ID) is extracted from the
 * {@code X-Tenant-Id} HTTP header by the API Gateway and propagated via a
 * Spring {@code HandlerInterceptor} or Servlet filter at the start of every
 * request. All downstream service code reads the tenant from this context
 * rather than passing it through every method signature.
 *
 * <h3>Virtual-thread compatibility (Java 21)</h3>
 * <p>{@link InheritableThreadLocal} is used instead of plain
 * {@link ThreadLocal} so that the tenant value propagates automatically to
 * child threads and to virtual threads spawned with
 * {@link Thread.Builder#inheritInheritableThreadLocals(boolean)}.
 * Spring Boot 3.x's virtual-thread executor (Tomcat with Loom) creates each
 * virtual thread as a child of the platform thread that dispatched the
 * request, so inheritable thread-locals are accessible without any extra
 * wiring.
 *
 * <h3>Lifecycle</h3>
 * <pre>
 *   // In a filter / interceptor — start of request:
 *   TenantContext.set(request.getHeader("X-Tenant-Id"));
 *   try {
 *       filterChain.doFilter(request, response);
 *   } finally {
 *       TenantContext.clear();   // MUST be called to prevent leaks
 *   }
 * </pre>
 *
 * <h3>Security note</h3>
 * <p>The tenant ID is treated as a trusted value only after JWT validation by
 * the API Gateway. Services must never use a raw header value; they read it
 * from this context after the gateway has verified the claim.
 */
public final class TenantContext {

    /**
     * Backing store — {@link InheritableThreadLocal} so virtual-thread child
     * threads inherit the tenant without explicit passing.
     */
    private static final InheritableThreadLocal<String> HOLDER =
            new InheritableThreadLocal<>();

    /** Prevent instantiation — this is a purely static utility class. */
    private TenantContext() {
        throw new AssertionError("TenantContext must not be instantiated");
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Store {@code tenantId} in the current thread's context.
     *
     * @param tenantId the examination authority identifier; must not be
     *                 {@code null} or blank
     * @throws IllegalArgumentException if {@code tenantId} is null or blank
     */
    public static void setTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(
                    "tenantId must not be null or blank");
        }
        HOLDER.set(tenantId);
    }

    /**
     * Alias for {@link #setTenantId(String)} — retained for compatibility.
     *
     * @param tenantId the examination authority identifier
     */
    public static void set(String tenantId) {
        setTenantId(tenantId);
    }

    /**
     * Retrieve the tenant ID bound to the current thread, or fall back to {@link UserContext#getTenantId()}.
     *
     * @return the tenant ID, or {@code null} if none has been set
     *         (e.g. during background jobs that are not tenant-scoped)
     */
    public static String getTenantId() {
        String tenantId = HOLDER.get();
        if (tenantId != null) {
            return tenantId;
        }
        try {
            return UserContext.getTenantId();
        } catch (NoClassDefFoundError | Exception ignored) {
            return null;
        }
    }

    /**
     * Alias for {@link #getTenantId()} — retained for compatibility.
     *
     * @return the tenant ID, or {@code null} if none has been set
     */
    public static String get() {
        return getTenantId();
    }

    /**
     * Retrieve the tenant ID, throwing if none is bound.
     *
     * <p>Use this variant in service-layer code that always expects a tenant
     * to be present (all API-request paths). Use {@link #getTenantId()} for
     * background/scheduled tasks that may run without a tenant.
     *
     * @return the tenant ID bound to the current thread
     * @throws IllegalStateException if no tenant ID is set
     */
    public static String getRequired() {
        String tenantId = getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException(
                    "No tenantId bound to the current thread. "
                    + "Ensure TenantContext.setTenantId() is called in the request filter.");
        }
        return tenantId;
    }

    /**
     * Alias for {@link #getRequired()} for standardized naming convention.
     *
     * @return the tenant ID bound to the current thread
     * @throws IllegalStateException if no tenant ID is set
     */
    public static String getRequiredTenantId() {
        return getRequired();
    }

    /**
     * Resolves the current tenant ID, falling back to {@link SharedConstants#DEFAULT_TENANT}.
     */
    public static String getTenantIdOrDefault() {
        return getTenantIdOrDefault(SharedConstants.DEFAULT_TENANT);
    }

    /**
     * Resolves the current tenant ID, falling back to {@code defaultTenant}.
     */
    public static String getTenantIdOrDefault(String defaultTenant) {
        String tenantId = getTenantId();
        return (tenantId != null && !tenantId.isBlank()) ? tenantId : defaultTenant;
    }

    /**
     * Resolves tenant ID from request header parameter, falling back to ambient thread context
     * and finally {@link SharedConstants#DEFAULT_TENANT}.
     */
    public static String resolveTenant(String headerTenant) {
        if (headerTenant != null && !headerTenant.isBlank()) {
            return headerTenant;
        }
        return getTenantIdOrDefault();
    }

    /**
     * Resolves mandatory tenant ID from header parameter or ambient context.
     * Throws {@link IllegalArgumentException} if no tenant identifier is present.
     */
    public static String resolveRequiredTenant(String headerTenant) {
        if (headerTenant != null && !headerTenant.isBlank()) {
            return headerTenant;
        }
        String tenantId = getTenantId();
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId;
        }
        throw new IllegalArgumentException("Missing required request header: " + SharedConstants.HEADER_TENANT_ID);
    }

    /**
     * Remove the tenant ID from the current thread's context.
     *
     * <p><strong>Must</strong> be called in the {@code finally} block of the
     * filter / interceptor that set the value to avoid memory leaks when
     * threads are pooled (virtual or platform).
     */
    public static void clear() {
        HOLDER.remove();
    }
}
