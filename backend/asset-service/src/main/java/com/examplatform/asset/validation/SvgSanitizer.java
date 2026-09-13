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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.\n */

package com.examplatform.asset.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Validates SVG content to prevent stored XSS, script injection, and XXE vulnerabilities.
 */
@Slf4j
@Component
public class SvgSanitizer {

    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile("(?i)<\\s*script[\\s>]");
    private static final Pattern EVENT_HANDLER_PATTERN = Pattern.compile("(?i)\\bon[a-z]+\\s*=");
    private static final Pattern JAVASCRIPT_URL_PATTERN = Pattern.compile("(?i)(href|src)\\s*=\\s*['\"]?\\s*javascript:");
    private static final Pattern XXE_PATTERN = Pattern.compile("(?i)<!ENTITY\\b");

    /**
     * Inspects the SVG stream for malicious executable content or script tags.
     * Throws {@link AssetValidationException} if unsafe elements are found.
     *
     * @param content the SVG content input stream
     * @throws AssetValidationException if prohibited constructs are detected
     */
    public void validate(InputStream content) {
        try {
            if (content.markSupported()) {
                content.mark(10 * 1024 * 1024); // up to 10MB
            }
            byte[] bytes = content.readAllBytes();
            if (content.markSupported()) {
                content.reset();
            }

            String svgText = new String(bytes, StandardCharsets.UTF_8);

            if (SCRIPT_TAG_PATTERN.matcher(svgText).find()) {
                log.warn("SVG sanitization rejected upload: <script> tag detected");
                throw new AssetValidationException("SVG contains prohibited <script> tag");
            }

            if (EVENT_HANDLER_PATTERN.matcher(svgText).find()) {
                log.warn("SVG sanitization rejected upload: inline event handler detected");
                throw new AssetValidationException("SVG contains prohibited inline event handler attributes");
            }

            if (JAVASCRIPT_URL_PATTERN.matcher(svgText).find()) {
                log.warn("SVG sanitization rejected upload: javascript: pseudo-protocol detected");
                throw new AssetValidationException("SVG contains prohibited javascript: pseudo-protocol");
            }

            if (XXE_PATTERN.matcher(svgText).find()) {
                log.warn("SVG sanitization rejected upload: XML entity declaration detected");
                throw new AssetValidationException("SVG contains prohibited XML entity declarations");
            }

            log.debug("SVG security validation passed");
        } catch (IOException e) {
            throw new AssetValidationException("Failed to read SVG content for security validation", e);
        }
    }
}
