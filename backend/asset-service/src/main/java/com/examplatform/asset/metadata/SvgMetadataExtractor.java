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

package com.examplatform.asset.metadata;

import com.examplatform.asset.domain.enums.AssetType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts width and height metadata from SVG diagrams by inspecting width,
 * height, or viewBox attributes.
 */
@Slf4j
@Component
public class SvgMetadataExtractor implements MetadataExtractor {

    private static final Pattern WIDTH_PATTERN = Pattern.compile("(?i)\\bwidth\\s*=\\s*['\"]([0-9.]+)(?:px)?['\"]");
    private static final Pattern HEIGHT_PATTERN = Pattern.compile("(?i)\\bheight\\s*=\\s*['\"]([0-9.]+)(?:px)?['\"]");
    private static final Pattern VIEWBOX_PATTERN = Pattern.compile("(?i)\\bviewBox\\s*=\\s*['\"][0-9.-]+\\s+[0-9.-]+\\s+([0-9.]+)\\s+([0-9.]+)['\"]");

    @Override
    public AssetType supportedType() {
        return AssetType.SVG;
    }

    @Override
    public MediaMetadata extract(InputStream content, String contentType) {
        MediaMetadata.MediaMetadataBuilder builder = MediaMetadata.builder();
        try {
            if (content.markSupported()) {
                content.mark(1024 * 1024);
            }
            byte[] bytes = content.readAllBytes();
            if (content.markSupported()) {
                content.reset();
            }

            String text = new String(bytes, StandardCharsets.UTF_8);

            Integer width = null;
            Integer height = null;

            Matcher wMatcher = WIDTH_PATTERN.matcher(text);
            if (wMatcher.find()) {
                width = parseInteger(wMatcher.group(1));
            }

            Matcher hMatcher = HEIGHT_PATTERN.matcher(text);
            if (hMatcher.find()) {
                height = parseInteger(hMatcher.group(1));
            }

            if (width == null || height == null) {
                Matcher vbMatcher = VIEWBOX_PATTERN.matcher(text);
                if (vbMatcher.find()) {
                    if (width == null) width = parseInteger(vbMatcher.group(1));
                    if (height == null) height = parseInteger(vbMatcher.group(2));
                }
            }

            builder.width(width);
            builder.height(height);
        } catch (Exception e) {
            log.warn("Failed to extract SVG metadata: {}", e.getMessage());
        }

        return builder.build();
    }

    private Integer parseInteger(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return (int) Math.round(Double.parseDouble(val));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
