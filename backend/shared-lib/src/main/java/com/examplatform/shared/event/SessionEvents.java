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

package com.examplatform.shared.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.Instant;

/**
 * Domain events for exam session lifecycle (SESSION_STARTED, SESSION_RESUMED, SESSION_EXPIRED, SESSION_FINALIZED).
 */
public final class SessionEvents {

    private SessionEvents() {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionStarted(
            String eventType,
            String sessionId,
            String candidateId,
            String examId,
            String shiftId,
            String startedAt,
            String tenantId
    ) implements Serializable {
        public static SessionStarted of(String sessionId, String candidateId, String examId, String shiftId, String tenantId) {
            return new SessionStarted("SESSION_STARTED", sessionId, candidateId, examId, shiftId, Instant.now().toString(), tenantId);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionResumed(
            String eventType,
            String sessionId,
            String candidateId,
            String examId,
            String shiftId,
            String resumedAt,
            String tenantId
    ) implements Serializable {
        public static SessionResumed of(String sessionId, String candidateId, String examId, String shiftId, String tenantId) {
            return new SessionResumed("SESSION_RESUMED", sessionId, candidateId, examId, shiftId, Instant.now().toString(), tenantId);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionExpired(
            String eventType,
            String sessionId,
            String candidateId,
            String examId,
            String expiredAt,
            String tenantId
    ) implements Serializable {
        public static SessionExpired of(String sessionId, String candidateId, String examId, String tenantId) {
            return new SessionExpired("SESSION_EXPIRED", sessionId, candidateId, examId, Instant.now().toString(), tenantId);
        }
    }
}
