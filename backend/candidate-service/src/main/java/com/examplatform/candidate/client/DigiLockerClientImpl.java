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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stub implementation of DigiLockerClient for local development.
 * In production, this would call the DigiLocker API with OAuth2 tokens.
 */
@Slf4j
@Component
public class DigiLockerClientImpl implements DigiLockerClient {

    @Override
    public DigiLockerResponse fetchDocument(String token, String docType) {
        log.info("[STUB] Fetching document from DigiLocker: docType={}", docType);
        return new DigiLockerResponse("SUCCESS", "STUB_DOC_DATA", docType);
    }
}
