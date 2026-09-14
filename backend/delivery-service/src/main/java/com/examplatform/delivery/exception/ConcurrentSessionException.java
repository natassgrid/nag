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

package com.examplatform.delivery.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when a candidate attempts to start a new exam session while
 * already having an ACTIVE session within the same tenant.
 * Enforces the single concurrent session invariant.
 */
@Getter
@ResponseStatus(HttpStatus.CONFLICT)
public class ConcurrentSessionException extends RuntimeException {

    private final UUID activeExamId;
    private final UUID activeSessionId;

    public ConcurrentSessionException(String message) {
        super(message);
        this.activeExamId = null;
        this.activeSessionId = null;
    }

    public ConcurrentSessionException(String message, UUID activeExamId, UUID activeSessionId) {
        super(message);
        this.activeExamId = activeExamId;
        this.activeSessionId = activeSessionId;
    }
}
