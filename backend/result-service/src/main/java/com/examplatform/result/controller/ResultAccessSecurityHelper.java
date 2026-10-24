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

package com.examplatform.result.controller;

import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.security.UserContext;
import com.examplatform.shared.tenant.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ResultAccessSecurityHelper {

    public static final String DEFAULT_TENANT_ID = SharedConstants.DEFAULT_TENANT;

    private static final Set<String> ELEVATED_ROLES = Set.of(
            "ROLE_SUPER_ADMIN", "ROLE_EXAM_CONTROLLER"
    );

    private ResultAccessSecurityHelper() {}

    public static void validateCandidateAccess(UUID candidateId, Authentication auth, String denialMessage) {
        if (candidateId == null) {
            return;
        }
        if (UserContext.hasAnyRole("SUPER_ADMIN", "EXAM_CONTROLLER")) {
            return;
        }
        if (auth != null) {
            boolean isElevated = auth.getAuthorities().stream()
                    .anyMatch(a -> ELEVATED_ROLES.contains(a.getAuthority()));
            if (isElevated) {
                return;
            }
        }

        UUID currentUserId = UserContext.getUserId();
        if (currentUserId != null) {
            if (!currentUserId.equals(candidateId)) {
                throw new AccessDeniedException(denialMessage);
            }
            return;
        }

        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String userId = jwt.getSubject();
            if (userId != null && !userId.equals(candidateId.toString())) {
                throw new AccessDeniedException(denialMessage);
            }
        }
    }

    public static String extractTenantId(Authentication auth) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId;
        }
        return Optional.ofNullable(auth)
                .map(Authentication::getDetails)
                .filter(Map.class::isInstance)
                .map(d -> ((Map<?, ?>) d).get("tenant_id"))
                .map(Object::toString)
                .orElse(DEFAULT_TENANT_ID);
    }
}
