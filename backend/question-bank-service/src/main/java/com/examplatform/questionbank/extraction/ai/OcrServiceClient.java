/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 Open Digital Public Infrastructure (DPI) Platform Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */
package com.examplatform.questionbank.extraction.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * REST client for the local Tesseract OCR microservice (Option B - Local Fast OCR).
 * Performs high-speed local character recognition (English + Hindi) on scanned PDF page images.
 */
@Service
@Slf4j
public class OcrServiceClient {

    @Value("${app.ai.extraction.ocr-service-url:${OCR_SERVICE_URL:http://localhost:5005}}")
    private String ocrServiceUrl = "http://localhost:5005";

    @Value("${app.ai.extraction.ocr-enabled:true}")
    private boolean ocrEnabled = true;

    private final RestTemplate restTemplate;

    public OcrServiceClient() {
        this.restTemplate = createOcrRestTemplate();
    }

    @Autowired
    public OcrServiceClient(
            @Value("${app.ai.extraction.ocr-service-url:${OCR_SERVICE_URL:http://localhost:5005}}") String ocrServiceUrl) {
        this.restTemplate = createOcrRestTemplate();
        this.ocrServiceUrl = ocrServiceUrl;
    }

    public OcrServiceClient(RestTemplate restTemplate, String ocrServiceUrl) {
        this.restTemplate = restTemplate != null ? restTemplate : createOcrRestTemplate();
        this.ocrServiceUrl = ocrServiceUrl;
    }

    private static RestTemplate createOcrRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));
        return new RestTemplate(factory);
    }

    public boolean isAvailable() {
        if (!ocrEnabled) {
            return false;
        }
        try {
            ResponseEntity<Map> res = restTemplate.getForEntity(ocrServiceUrl + "/health", Map.class);
            return res.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.debug("Local OCR service unavailable at {}: {}", ocrServiceUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Extracts text from an image data URI or base64 string using local Tesseract OCR.
     *
     * @param dataUriOrBase64 image base64 data URI (data:image/jpeg;base64,...)
     * @param language        language codes e.g. "eng+hin"
     * @return extracted text or null if extraction fails
     */
    public String extractTextFromImage(String dataUriOrBase64, String language) {
        if (!ocrEnabled || dataUriOrBase64 == null || dataUriOrBase64.isBlank()) {
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> payload = Map.of(
                    "image_data_uri", dataUriOrBase64,
                    "lang", language != null ? language : "eng+hin"
            );

            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<OcrResponseDto> response = restTemplate.postForEntity(
                    ocrServiceUrl + "/ocr", request, OcrResponseDto.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getText();
            }
        } catch (Exception e) {
            log.warn("Local OCR extraction call failed against {}: {}", ocrServiceUrl, e.getMessage());
        }
        return null;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OcrResponseDto {
        private String status;
        private String text;
        private int length;
        private String language;
    }
}
