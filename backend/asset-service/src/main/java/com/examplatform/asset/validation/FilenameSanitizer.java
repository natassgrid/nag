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

package com.examplatform.asset.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Sanitizes uploaded filenames to prevent path traversal, injection attacks,
 * and filesystem compatibility issues.
 */
@Slf4j
@Component
public class FilenameSanitizer {

    /** Allowed characters: alphanumeric, hyphens, underscores, dots, spaces. */
    private static final Pattern UNSAFE_CHARS = Pattern.compile("[^a-zA-Z0-9._\\-\\s]");

    /** Consecutive dots (path traversal) */
    private static final Pattern DOUBLE_DOTS = Pattern.compile("\\.{2,}");

    /** Leading/trailing dots or spaces */
    private static final Pattern LEADING_TRAILING = Pattern.compile("^[.\\s]+|[.\\s]+$");

    /** Maximum filename length */
    private static final int MAX_LENGTH = 255;

    /**
     * Sanitize a filename for safe storage.
     *
     * @param originalFilename the original filename from the upload
     * @return sanitized filename
     * @throws AssetValidationException if the filename is empty after sanitization
     */
    public String sanitize(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new AssetValidationException("Filename is required");
        }

        // Extract just the filename (strip any path components)
        String filename = originalFilename;
        int lastSlash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            filename = filename.substring(lastSlash + 1);
        }

        // Remove path traversal
        filename = DOUBLE_DOTS.matcher(filename).replaceAll(".");

        // Remove unsafe characters
        filename = UNSAFE_CHARS.matcher(filename).replaceAll("_");

        // Remove leading/trailing dots and spaces
        filename = LEADING_TRAILING.matcher(filename).replaceAll("");

        // Truncate if too long
        if (filename.length() > MAX_LENGTH) {
            String ext = extractExtension(filename);
            int maxNameLen = MAX_LENGTH - ext.length() - 1;
            filename = filename.substring(0, Math.max(maxNameLen, 1)) + "." + ext;
        }

        if (filename.isBlank()) {
            throw new AssetValidationException("Filename is invalid after sanitization: " + originalFilename);
        }

        log.debug("Filename sanitized: '{}' → '{}'", originalFilename, filename);
        return filename;
    }

    /**
     * Extract the file extension from a filename.
     *
     * @param filename the filename
     * @return the extension (without dot), or empty string if none
     */
    public String extractExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) return "";
        return filename.substring(lastDot + 1).toLowerCase();
    }
}
