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
 * Domain events for proctoring snapshots, alerts, and analysis.
 */
public final class ProctoringEvents {

    private ProctoringEvents() {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SnapshotCaptured(
            String eventType,
            String sessionId,
            String candidateId,
            String snapshotRef,
            String tenantId,
            String capturedAt,
            int imageSize
    ) implements Serializable {
        public static SnapshotCaptured of(String sessionId, String candidateId, String snapshotRef, String tenantId, int imageSize) {
            return new SnapshotCaptured(
                    "SNAPSHOT_CAPTURED",
                    sessionId,
                    candidateId,
                    snapshotRef,
                    tenantId,
                    Instant.now().toString(),
                    imageSize
            );
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AuditAlert(
            String eventType,
            String sessionId,
            String candidateId,
            String snapshotRef,
            String source,
            double confidence,
            String occurredAt
    ) implements Serializable {
        public static AuditAlert of(String eventType, String sessionId, String candidateId, String snapshotRef, String source, double confidence) {
            return new AuditAlert(
                    eventType,
                    sessionId,
                    candidateId,
                    snapshotRef,
                    source,
                    confidence,
                    Instant.now().toString()
            );
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionFlaggedAlert(
            String eventType,
            String sessionId,
            String candidateId,
            int fullScreenExitCount,
            int threshold,
            String occurredAt,
            String tenantId
    ) implements Serializable {
        public static SessionFlaggedAlert of(String sessionId, String candidateId, int count, int threshold, String tenantId) {
            return new SessionFlaggedAlert(
                    "SESSION_FLAGGED_FULLSCREEN_EXITS",
                    sessionId,
                    candidateId,
                    count,
                    threshold,
                    Instant.now().toString(),
                    tenantId
            );
        }
    }
}
