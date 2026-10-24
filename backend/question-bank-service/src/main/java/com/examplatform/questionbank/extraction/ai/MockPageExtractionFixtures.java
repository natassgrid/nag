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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Optional;

/**
 * Loads deterministic mock extraction fixtures for offline local development,
 * continuous integration pipelines, and token-conserving automated test runs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockPageExtractionFixtures {

    private final ObjectMapper objectMapper;

    /**
     * Attempts to load a JSON fixture for the requested page number.
     * Looks for classpath resources in this order:
     * 1. extraction-fixtures/page-{pageNumber}.json
     * 2. extraction-fixtures/default.json
     *
     * @param pageNumber 1-based page index
     * @return deserialized {@link LlmPageExtractionResult} if fixture exists, else empty
     */
    public Optional<LlmPageExtractionResult> loadFixture(int pageNumber) {
        String specificPath = "extraction-fixtures/page-" + pageNumber + ".json";
        Optional<LlmPageExtractionResult> specificResult = tryLoadFromClasspath(specificPath);
        if (specificResult.isPresent()) {
            log.info("Loaded mock extraction fixture from '{}' for page {}", specificPath, pageNumber);
            return specificResult;
        }

        String defaultPath = "extraction-fixtures/default.json";
        Optional<LlmPageExtractionResult> defaultResult = tryLoadFromClasspath(defaultPath);
        if (defaultResult.isPresent()) {
            log.info("Loaded default mock extraction fixture from '{}' for page {}", defaultPath, pageNumber);
            return defaultResult;
        }

        log.debug("No mock extraction fixture found for page {}", pageNumber);
        return Optional.empty();
    }

    private Optional<LlmPageExtractionResult> tryLoadFromClasspath(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                return Optional.empty();
            }
            try (InputStream is = resource.getInputStream()) {
                LlmPageExtractionResult result = objectMapper.readValue(is, LlmPageExtractionResult.class);
                return Optional.ofNullable(result);
            }
        } catch (Exception e) {
            log.warn("Failed to load or parse mock fixture from '{}': {}", resourcePath, e.getMessage());
            return Optional.empty();
        }
    }
}
