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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OcrServiceClient Unit Tests")
class OcrServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    private OcrServiceClient client;

    @BeforeEach
    void setUp() {
        client = new OcrServiceClient(restTemplate, "http://localhost:5005");
    }

    @Test
    @DisplayName("isAvailable: returns true when /health responds with 200 OK")
    void testIsAvailableSuccess() {
        when(restTemplate.getForEntity("http://localhost:5005/health", Map.class))
                .thenReturn(new ResponseEntity<>(Map.of("status", "healthy"), HttpStatus.OK));

        assertThat(client.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("isAvailable: returns false when service throws RestClientException")
    void testIsAvailableFailure() {
        when(restTemplate.getForEntity("http://localhost:5005/health", Map.class))
                .thenThrow(new RestClientException("Connection refused"));

        assertThat(client.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("extractTextFromImage: returns extracted text on 200 OK")
    void testExtractTextSuccess() {
        OcrServiceClient.OcrResponseDto responseDto = new OcrServiceClient.OcrResponseDto();
        responseDto.setStatus("success");
        responseDto.setText("1. Q1 Sample question content in Hindi and English");
        responseDto.setLength(50);
        responseDto.setLanguage("eng+hin");

        when(restTemplate.postForEntity(eq("http://localhost:5005/ocr"), any(), eq(OcrServiceClient.OcrResponseDto.class)))
                .thenReturn(new ResponseEntity<>(responseDto, HttpStatus.OK));

        String text = client.extractTextFromImage("data:image/jpeg;base64,abc123", "eng+hin");

        assertThat(text).isEqualTo("1. Q1 Sample question content in Hindi and English");
    }

    @Test
    @DisplayName("extractTextFromImage: returns null gracefully when service throws exception")
    void testExtractTextFailureGraceful() {
        when(restTemplate.postForEntity(eq("http://localhost:5005/ocr"), any(), eq(OcrServiceClient.OcrResponseDto.class)))
                .thenThrow(new RestClientException("OCR Timeout"));

        String text = client.extractTextFromImage("data:image/jpeg;base64,abc123", "eng+hin");

        assertThat(text).isNull();
    }

    @Test
    @DisplayName("extractTextFromImage: returns null when input data URI is null or blank")
    void testExtractTextNullInput() {
        assertThat(client.extractTextFromImage(null, "eng+hin")).isNull();
        assertThat(client.extractTextFromImage("   ", "eng+hin")).isNull();
    }
}
