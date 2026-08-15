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

package com.examplatform.shared.auth;

/**
 * Client interface for requesting service-account tokens from the identity provider (Keycloak).
 * Implementations use the OAuth2 client_credentials grant to obtain short-lived JWTs
 * for inter-service authentication.
 */
public interface ServiceAccountTokenClient {

    /**
     * Requests a new access token for the specified service using the client_credentials grant.
     *
     * @param serviceId the service identifier (maps to a Keycloak client ID)
     * @return the token response containing the access token and expiry information
     */
    TokenResponse requestToken(String serviceId);
}
