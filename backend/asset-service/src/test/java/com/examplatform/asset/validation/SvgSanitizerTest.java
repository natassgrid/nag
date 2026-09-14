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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SvgSanitizer}.
 */
@DisplayName("SvgSanitizer")
class SvgSanitizerTest {

    private SvgSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new SvgSanitizer();
    }

    @Test
    @DisplayName("accepts valid and clean SVG diagrams")
    void acceptsValidSvg() {
        String cleanSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                    <circle cx="50" cy="50" r="40" stroke="green" stroke-width="4" fill="yellow" />
                </svg>
                """;

        assertThatCode(() -> sanitizer.validate(new ByteArrayInputStream(cleanSvg.getBytes(StandardCharsets.UTF_8))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejects SVG containing <script> tags")
    void rejectsScriptTags() {
        String maliciousSvg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                    <script type="text/javascript">alert('XSS');</script>
                    <rect width="100" height="100" />
                </svg>
                """;

        assertThatThrownBy(() -> sanitizer.validate(new ByteArrayInputStream(maliciousSvg.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(AssetValidationException.class)
                .hasMessageContaining("<script>");
    }

    @Test
    @DisplayName("rejects SVG containing inline event handlers (onload, onclick)")
    void rejectsEventHandlers() {
        String maliciousSvg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                    <image href="test.png" onload="alert(document.cookie)" />
                </svg>
                """;

        assertThatThrownBy(() -> sanitizer.validate(new ByteArrayInputStream(maliciousSvg.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(AssetValidationException.class)
                .hasMessageContaining("inline event handler");
    }

    @Test
    @DisplayName("rejects SVG containing javascript: pseudo-protocol in links")
    void rejectsJavascriptProtocol() {
        String maliciousSvg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                    <a href="javascript:alert(1)"><text y="20">Click me</text></a>
                </svg>
                """;

        assertThatThrownBy(() -> sanitizer.validate(new ByteArrayInputStream(maliciousSvg.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(AssetValidationException.class)
                .hasMessageContaining("javascript:");
    }

    @Test
    @DisplayName("rejects SVG containing XML entity declarations (XXE)")
    void rejectsXmlEntities() {
        String maliciousSvg = """
                <?xml version="1.0" standalone="no"?>
                <!DOCTYPE svg [
                  <!ELEMENT svg ANY >
                  <!ENTITY xxe SYSTEM "file:///etc/passwd" >]>
                <svg>&xxe;</svg>
                """;

        assertThatThrownBy(() -> sanitizer.validate(new ByteArrayInputStream(maliciousSvg.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(AssetValidationException.class)
                .hasMessageContaining("XML entity");
    }
}
