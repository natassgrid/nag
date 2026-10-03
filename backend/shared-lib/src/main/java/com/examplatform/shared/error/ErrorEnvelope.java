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

package com.examplatform.shared.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Standard nested error response envelope (used in delivery and evaluation services).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorEnvelope(
        String status,
        ErrorDetails error,
        int httpStatus
) {
    public static ErrorEnvelope of(String code, String message, int httpStatus) {
        return new ErrorEnvelope("error", new ErrorDetails(code, message, Instant.now().toString(), null, null), httpStatus);
    }

    public static ErrorEnvelope of(String code, String message, int httpStatus, String activeExamId, String activeSessionId) {
        return new ErrorEnvelope("error", new ErrorDetails(code, message, Instant.now().toString(), activeExamId, activeSessionId), httpStatus);
    }
}
