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

package com.examplatform.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Generic response envelope returned by every API endpoint in the platform.
 *
 * <pre>
 * {
 *   "status":    "SUCCESS" | "ERROR",
 *   "message":   "Human-readable summary",
 *   "data":      { ... },        // present on success; absent on error
 *   "timestamp": "2024-01-01T00:00:00"
 * }
 * </pre>
 *
 * @param <T> the type of the response payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiResponse<T> {

    /** "SUCCESS" or "ERROR". */
    private final String status;

    /** Short human-readable summary of the outcome. */
    private final String message;

    /** Response payload — null on error responses. */
    private final T data;

    /** UTC timestamp of when the response was created. */
    private final LocalDateTime timestamp;

    // -----------------------------------------------------------------------
    // Private constructor — use static factory methods
    // -----------------------------------------------------------------------

    private ApiResponse(String status, String message, T data) {
        this.status    = status;
        this.message   = message;
        this.data      = data;
        this.timestamp = LocalDateTime.now();
    }

    // -----------------------------------------------------------------------
    // Static factories
    // -----------------------------------------------------------------------

    /**
     * Build a successful response with a payload.
     *
     * @param data response payload
     * @param <T>  payload type
     * @return success envelope with default "OK" message
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", null, data);
    }

    /**
     * Build a successful response with a payload and custom message.
     *
     * @param data    response payload
     * @param message human-readable success message
     * @param <T>     payload type
     * @return success envelope
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>("SUCCESS", message, data);
    }

    /**
     * Build an error response.
     *
     * @param message human-readable error summary
     * @param <T>     payload type
     * @return error envelope with {@code data = null}
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("ERROR", message, null);
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "ApiResponse{status='" + status + "', message='" + message + "', timestamp=" + timestamp + '}';
    }
}
