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

package com.examplatform.admin.controller;

import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.security.UserContext;
import com.examplatform.shared.tenant.TenantContext;
import org.springframework.security.oauth2.jwt.Jwt;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Controller-layer utility methods for extracting authenticated actor UUIDs and resolving active tenant context.
 */
public final class AdminControllerHelper {

    private static final UUID DEFAULT_ANONYMOUS_ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private AdminControllerHelper() {
    }

    public static String resolveTenant(String paramTenant) {
        return TenantContext.resolveTenant(paramTenant);
    }

    public static UUID extractActorId() {
        return UserContext.resolveUserId(null, DEFAULT_ANONYMOUS_ACTOR);
    }
}
