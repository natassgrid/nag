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

package com.examplatform.result.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client implementation for DigiLocker scorecard publishing.
 * Pushes digital scorecard references and certificates to DigiLocker vault.
 *
 * Validates: Requirements 13.5
 */
@Slf4j
@Component
public class DigiLockerClientImpl implements DigiLockerClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    @Value("${app.digilocker.push-url:${DIGILOCKER_PUSH_URL:http://localhost:8099/digilocker/v1/credential/push}}")
    private String digiLockerPushUrl;

    private final RestClient restClient;

    public DigiLockerClientImpl() {
        this.restClient = RestClient.create();
    }

    public DigiLockerClientImpl(RestClient restClient) {
        this.restClient = restClient != null ? restClient : RestClient.create();
    }

    @Override
    public void pushScorecard(UUID candidateId, String pdfRef) {
        log.info("Pushing scorecard to DigiLocker for candidate={}, pdfRef={}, endpoint={}",
                candidateId, pdfRef, digiLockerPushUrl);

        if (digiLockerPushUrl != null && !digiLockerPushUrl.isBlank()) {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("candidateId", candidateId != null ? candidateId.toString() : "");
                payload.put("pdfRef", pdfRef != null ? pdfRef : "");
                payload.put("docType", "SCORECARD");
                payload.put("timestamp", Instant.now().toString());

                Map<String, Object> response = restClient.post()
                        .uri(digiLockerPushUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .body(MAP_TYPE);

                log.info("DigiLocker scorecard push succeeded for candidate {}: docId={}, status={}",
                        candidateId,
                        response != null ? response.get("docId") : "N/A",
                        response != null ? response.get("status") : "SUCCESS");
            } catch (Exception e) {
                log.warn("Failed to push scorecard to DigiLocker at {}: {}", digiLockerPushUrl, e.getMessage());
            }
        }
    }
}
