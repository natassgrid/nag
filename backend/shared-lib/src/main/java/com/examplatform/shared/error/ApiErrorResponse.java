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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Standard typed error response structure for REST endpoints across the platform.
 * Supports standard HTTP error fields, field validation maps, and optional extension attributes.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        String code,
        Map<String, String> fieldErrors,
        @JsonAnyGetter
        Map<String, Object> extraProperties
) {

    public ApiErrorResponse {
        if (timestamp == null) {
            timestamp = Instant.now().toString();
        }
    }

    public static ApiErrorResponse of(int status, String error, String message) {
        return new ApiErrorResponse(Instant.now().toString(), status, error, message, null, null, null, null);
    }

    public static ApiErrorResponse of(int status, String error, String message, String code) {
        return new ApiErrorResponse(Instant.now().toString(), status, error, message, null, code, null, null);
    }

    public static ApiErrorResponse validation(int status, String message, Map<String, String> fieldErrors) {
        return new ApiErrorResponse(Instant.now().toString(), status, "Validation Failed", message, null, "VALIDATION_ERROR", fieldErrors, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String timestamp = Instant.now().toString();
        private int status;
        private String error;
        private String message;
        private String path;
        private String code;
        private Map<String, String> fieldErrors;
        private final Map<String, Object> extraProperties = new HashMap<>();

        public Builder timestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder fieldErrors(Map<String, String> fieldErrors) {
            this.fieldErrors = fieldErrors != null ? new HashMap<>(fieldErrors) : null;
            return this;
        }

        public Builder property(String key, Object value) {
            if (key != null && value != null) {
                this.extraProperties.put(key, value);
            }
            return this;
        }

        public ApiErrorResponse build() {
            return new ApiErrorResponse(
                    timestamp,
                    status,
                    error,
                    message,
                    path,
                    code,
                    fieldErrors,
                    extraProperties.isEmpty() ? null : Collections.unmodifiableMap(extraProperties)
            );
        }
    }
}
