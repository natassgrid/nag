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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Summary of a question import run: how many files/rows were processed, how many
 * questions were created, and a per-failure breakdown for diagnosis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {

    /** Number of batch files found and processed inside the archive. */
    private int filesProcessed;

    /** Total question records read across all files. */
    private int totalRecords;

    /** Number of questions successfully created. */
    private int imported;

    /** Number of records that failed validation or creation. */
    private int failed;

    /** Per-record failures (bounded to avoid unbounded responses). */
    @Builder.Default
    private List<FailedRecord> failures = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedRecord {
        /** Archive entry (file) the record came from. */
        private String file;
        /** 0-based index of the record within its file. */
        private int recordIndex;
        /** Human-readable reason for the failure. */
        private String error;
    }
}
