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

package com.examplatform.identity.exception;

import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
public class SmsRateLimitExceededException extends RuntimeException {

    private final LocalDateTime nextAvailableAt;
    private final long retryAfterSeconds;

    public SmsRateLimitExceededException(String message, LocalDateTime nextAvailableAt, long retryAfterSeconds) {
        super(message);
        this.nextAvailableAt = nextAvailableAt;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public SmsRateLimitExceededException(String message, LocalDateTime nextAvailableAt) {
        super(message);
        this.nextAvailableAt = nextAvailableAt;
        if (nextAvailableAt != null) {
            long diff = Duration.between(LocalDateTime.now(), nextAvailableAt).getSeconds();
            this.retryAfterSeconds = Math.max(1, diff);
        } else {
            this.retryAfterSeconds = 86400;
        }
    }

    public SmsRateLimitExceededException(String message) {
        this(message, null, 86400);
    }
}
