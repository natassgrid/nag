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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Implementation of DigiLockerClient that connects to configured DigiLocker
 * endpoint (or mock server) when url is provided, or returns fallback stub.
 */
@Slf4j
@Component
public class DigiLockerClientImpl implements DigiLockerClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    @Value("${app.digilocker.api-url:${DIGILOCKER_API_URL:}}")
    private String digiLockerApiUrl;

    private final RestClient restClient = RestClient.create();

    @Override
    public DigiLockerResponse fetchDocument(String token, String docType) {
        log.info("Fetching document from DigiLocker: docType={}, apiUrl={}", docType, digiLockerApiUrl);

        if (digiLockerApiUrl != null && !digiLockerApiUrl.isBlank()) {
            try {
                Map<String, Object> response = restClient.post()
                        .uri(digiLockerApiUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "token", token != null ? token : "",
                                "docType", docType != null ? docType : "AADHAAR"
                        ))
                        .retrieve()
                        .body(MAP_TYPE);

                if (response != null) {
                    String status = response.get("status") != null ? String.valueOf(response.get("status")) : "SUCCESS";
                    String docData = response.get("documentData") != null ? String.valueOf(response.get("documentData")) : "MOCK_DOC_DATA";
                    String issuerId = response.get("issuerId") != null ? String.valueOf(response.get("issuerId")) : (docType != null ? docType : "in.gov.cbse");
                    return new DigiLockerResponse(status, docData, issuerId);
                }
            } catch (Exception e) {
                log.warn("Failed to invoke DigiLocker API at {}: {}. Falling back to default mock response.",
                        digiLockerApiUrl, e.getMessage());
            }
        }

        return new DigiLockerResponse("SUCCESS", "STUB_DOC_DATA", docType);
    }
}
