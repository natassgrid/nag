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

package com.examplatform.admin.client;

import java.util.UUID;

/**
 * Client interface for Keycloak admin operations.
 * Implementations will use the Keycloak Admin REST API to manage user accounts.
 */
public interface KeycloakAdminClient {

    /**
     * Disables a user account in Keycloak, preventing further authentication.
     *
     * @param userId   the user's unique identifier (matches Keycloak sub claim)
     * @param tenantId the tenant (examination authority) identifier (maps to Keycloak realm or attribute)
     */
    void disableUser(UUID userId, String tenantId);
}
