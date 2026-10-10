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

package com.examplatform.admin.controller;

import com.examplatform.shared.tenant.TenantContext;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Controller-layer utility methods for extracting authenticated actor UUIDs and resolving active tenant context.
 */
public final class AdminControllerHelper {

    private static final UUID DEFAULT_ANONYMOUS_ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private AdminControllerHelper() {
    }

    public static String resolveTenant(String paramTenant) {
        if (paramTenant != null && !paramTenant.isBlank()) {
            return paramTenant;
        }
        String contextTenant = TenantContext.get();
        if (contextTenant != null && !contextTenant.isBlank()) {
            return contextTenant;
        }
        return "default";
    }

    public static UUID extractActorId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            return DEFAULT_ANONYMOUS_ACTOR;
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(jwt.getSubject().getBytes());
        }
    }
}
