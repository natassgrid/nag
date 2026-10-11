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

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal, dependency-free RFC 4180 CSV reader/writer.
 *
 * <p>Supports comma field separators, CRLF or LF record separators, and
 * double-quote quoting with {@code ""} escaping. Fields containing a comma,
 * quote, CR, or LF are quoted on write; quoted fields are unescaped on read.
 * This is intentionally small and self-contained to avoid pulling a third-party
 * CSV library into the service.
 */
public final class CsvUtil {

    private CsvUtil() {
    }

    /**
     * Writes a single CSV record (list of field values) followed by CRLF.
     * Null values are written as empty fields.
     */
    public static String writeRow(List<String> fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(fields.get(i)));
        }
        sb.append("\r\n");
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean mustQuote = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        if (!mustQuote) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /**
     * Parses an entire CSV document into a list of records, each a list of field
     * values. Handles quoted fields containing commas, quotes, and newlines.
     */
    public static List<List<String>> parse(String content) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        return new CsvParser(content).parse();
    }

    private static final class CsvParser {
        private final String content;
        private final int length;
        private final List<List<String>> rows = new ArrayList<>();
        private List<String> current = new ArrayList<>();
        private final StringBuilder field = new StringBuilder();
        private boolean inQuotes = false;
        private boolean fieldStarted = false;
        private int index = 0;

        private CsvParser(String content) {
            this.content = content;
            this.length = content.length();
        }

        private List<List<String>> parse() {
            while (index < length) {
                if (inQuotes) {
                    processQuotedChar();
                } else {
                    processUnquotedChar();
                }
            }
            flushTrailing();
            return rows;
        }

        private void processQuotedChar() {
            char c = content.charAt(index);
            if (c == '"') {
                if (index + 1 < length && content.charAt(index + 1) == '"') {
                    field.append('"');
                    index += 2;
                    return;
                }
                inQuotes = false;
                index++;
            } else {
                field.append(c);
                index++;
            }
        }

        private void processUnquotedChar() {
            char c = content.charAt(index);
            switch (c) {
                case '"' -> {
                    inQuotes = true;
                    fieldStarted = true;
                    index++;
                }
                case ',' -> {
                    commitField();
                    index++;
                }
                case '\r' -> {
                    if (index + 1 < length && content.charAt(index + 1) == '\n') {
                        index++;
                    }
                    commitRow();
                    index++;
                }
                case '\n' -> {
                    commitRow();
                    index++;
                }
                default -> {
                    field.append(c);
                    fieldStarted = true;
                    index++;
                }
            }
        }

        private void commitField() {
            current.add(field.toString());
            field.setLength(0);
            fieldStarted = false;
        }

        private void commitRow() {
            commitField();
            rows.add(current);
            current = new ArrayList<>();
        }

        private void flushTrailing() {
            if (!field.isEmpty() || fieldStarted || !current.isEmpty()) {
                current.add(field.toString());
                rows.add(current);
            }
        }
    }
}
