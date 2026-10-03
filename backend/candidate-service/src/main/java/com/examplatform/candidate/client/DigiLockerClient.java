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

package com.examplatform.candidate.client;

import com.examplatform.candidate.dto.DigiLockerResponse;

import java.util.Map;

/**
 * Client interface for calling DigiLocker OAuth2 and Document APIs.
 * Supports OAuth2 authorization code flow, userinfo fetching,
 * and identity document verification.
 *
 * Validates: Requirements 1.3
 */
public interface DigiLockerClient {

    /**
     * Constructs the DigiLocker OAuth2 authorization URL.
     *
     * @param state       state token for CSRF protection and context preservation
     * @param redirectUri optional custom redirect URI
     * @return full authorization redirect URL
     */
    String getAuthorizationUrl(String state, String redirectUri);

    /**
     * Exchanges an authorization code for DigiLocker OAuth2 access and ID tokens.
     *
     * @param code        the authorization code received in callback
     * @param redirectUri the redirect URI used during authorization
     * @return token response map containing access_token, id_token, etc.
     */
    Map<String, Object> exchangeCodeForToken(String code, String redirectUri);

    /**
     * Fetches user profile / demographic identity data using OAuth2 access token.
     *
     * @param token OAuth2 bearer access token
     * @return map of user demographic fields (name, dob, gender, digilocker_id, etc.)
     */
    Map<String, Object> getUserInfo(String token);

    /**
     * Fetches a document from DigiLocker for verification.
     *
     * @param token   the OAuth2 access token
     * @param docType the document type to fetch (e.g., "AADHAAR", "PAN", "10TH_MARKSHEET")
     * @return the document response from DigiLocker
     */
    DigiLockerResponse fetchDocument(String token, String docType);
}
