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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.\n */

package com.examplatform.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response returned after a session is successfully started.
 * Contains the first question content (decrypted in memory) to meet the 500ms SLA,
 * as well as dynamically configured delivery parameters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStartResponse {

    private UUID sessionId;
    private UUID examId;
    private UUID shiftId;
    private Instant startedAt;
    private Instant scheduledEndAt;
    private String firstQuestionContent;
    private int totalQuestions;

    // Dynamically configured delivery parameters
    @Builder.Default
    private boolean kioskModeEnforced = true;
    @Builder.Default
    private int heartbeatIntervalSeconds = 10;
    @Builder.Default
    private int autosaveIntervalSeconds = 15;
    @Builder.Default
    private int maxDisconnectGraceSeconds = 180;
    @Builder.Default
    private boolean tamperDetectionEnabled = true;
}
