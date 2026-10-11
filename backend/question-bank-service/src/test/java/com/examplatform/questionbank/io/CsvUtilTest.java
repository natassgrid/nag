/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

package com.examplatform.questionbank.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CsvUtil Unit Tests")
class CsvUtilTest {

    @Test
    @DisplayName("writeRow should quote fields with comma, quote, CRLF, and escape quotes")
    void shouldWriteRowWithEscaping() {
        List<String> fields = List.of("simple", "with,comma", "with\"quote", "with\nnewline", "");
        String row = CsvUtil.writeRow(fields);

        assertThat(row).isEqualTo("simple,\"with,comma\",\"with\"\"quote\",\"with\nnewline\",\r\n");
    }

    @Test
    @DisplayName("parse should parse simple unquoted CSV")
    void shouldParseSimpleCsv() {
        String input = "a,b,c\r\n1,2,3";
        List<List<String>> result = CsvUtil.parse(input);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsExactly("a", "b", "c");
        assertThat(result.get(1)).containsExactly("1", "2", "3");
    }

    @Test
    @DisplayName("parse should handle quoted commas, escaped quotes, and newlines")
    void shouldParseComplexQuotedCsv() {
        String input = "\"hello, world\",\"say \"\"hi\"\"\",\"line1\nline2\"\r\nsecond,row";
        List<List<String>> result = CsvUtil.parse(input);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsExactly("hello, world", "say \"hi\"", "line1\nline2");
        assertThat(result.get(1)).containsExactly("second", "row");
    }

    @Test
    @DisplayName("parse should return empty list for null or empty input")
    void shouldHandleEmptyOrNullInput() {
        assertThat(CsvUtil.parse(null)).isEmpty();
        assertThat(CsvUtil.parse("")).isEmpty();
    }

    @Test
    @DisplayName("parse should handle empty fields and trailing fields without newline")
    void shouldHandleEmptyFields() {
        String input = "a,,c\n,d,";
        List<List<String>> result = CsvUtil.parse(input);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsExactly("a", "", "c");
        assertThat(result.get(1)).containsExactly("", "d", "");
    }
}
