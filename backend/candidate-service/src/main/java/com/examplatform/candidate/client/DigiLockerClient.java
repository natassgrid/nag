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

/**
 * Stub interface for calling DigiLocker API with OAuth2 token.
 * Implementations will integrate with the actual DigiLocker
 * document verification service.
 *
 * Validates: Requirements 1.3
 */
public interface DigiLockerClient {

    /**
     * Fetches a document from DigiLocker for verification.
     *
     * @param token   the OAuth2 access token
     * @param docType the document type to fetch (e.g., "AADHAAR", "PAN")
     * @return the document response from DigiLocker
     */
    DigiLockerResponse fetchDocument(String token, String docType);
}
