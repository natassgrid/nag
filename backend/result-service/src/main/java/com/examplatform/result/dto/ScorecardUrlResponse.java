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

package com.examplatform.result.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO containing a secure time-limited presigned URL for downloading a candidate's scorecard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScorecardUrlResponse {

    /** Unique result identifier. */
    private UUID resultId;

    /** Unique candidate identifier. */
    private UUID candidateId;

    /** Exam identifier. */
    private UUID examId;

    /** Time-limited secure download URL. */
    private String downloadUrl;

    /** URL lifetime in seconds (e.g. 900 for 15 minutes). */
    private long expiresInSeconds;

    /** Timestamp at which the URL expires. */
    private Instant expiresAt;

    /** Storage provider type (e.g. "s3", "local"). */
    private String storageProvider;

    /** S3 object key or relative storage path. */
    private String storageKey;
}
