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

package com.examplatform.audit.controller;

import com.examplatform.shared.constants.SharedConstants;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.Optional;

final class AuditSecurityHelper {

    static final String DEFAULT_TENANT_ID = SharedConstants.DEFAULT_TENANT;

    private AuditSecurityHelper() {}

    static String extractTenantId(Authentication auth) {
        return Optional.ofNullable(auth)
                .map(Authentication::getDetails)
                .filter(Map.class::isInstance)
                .map(d -> ((Map<?, ?>) d).get("tenant_id"))
                .map(Object::toString)
                .orElse(DEFAULT_TENANT_ID);
    }
}
