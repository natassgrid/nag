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
        List<List<String>> rows = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean fieldStarted = false;
        int i = 0;
        int n = content.length();

        while (i < n) {
            char c = content.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < n && content.charAt(i + 1) == '"') {
                        field.append('"');
                        i += 2;
                        continue;
                    }
                    inQuotes = false;
                    i++;
                } else {
                    field.append(c);
                    i++;
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                    fieldStarted = true;
                    i++;
                } else if (c == ',') {
                    current.add(field.toString());
                    field.setLength(0);
                    fieldStarted = false;
                    i++;
                } else if (c == '\r') {
                    // consume optional following \n as part of the record separator
                    if (i + 1 < n && content.charAt(i + 1) == '\n') {
                        i++;
                    }
                    current.add(field.toString());
                    field.setLength(0);
                    rows.add(current);
                    current = new ArrayList<>();
                    fieldStarted = false;
                    i++;
                } else if (c == '\n') {
                    current.add(field.toString());
                    field.setLength(0);
                    rows.add(current);
                    current = new ArrayList<>();
                    fieldStarted = false;
                    i++;
                } else {
                    field.append(c);
                    fieldStarted = true;
                    i++;
                }
            }
        }

        // flush trailing field/record (file not ending in newline)
        if (field.length() > 0 || fieldStarted || !current.isEmpty()) {
            current.add(field.toString());
            rows.add(current);
        }
        return rows;
    }
}
