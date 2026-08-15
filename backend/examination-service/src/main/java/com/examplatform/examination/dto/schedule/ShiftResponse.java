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

package com.examplatform.examination.dto.schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Response DTO for a single shift within an examination schedule.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftResponse {

    private UUID id;
    private UUID scheduleId;
    private int shiftNumber;
    private String shiftName;
    private LocalTime reportingTime;
    private LocalTime gateClosingTime;
    private LocalTime loginStartTime;
    private LocalTime examStartTime;
    private LocalTime examEndTime;
    private LocalTime exitTime;
    private int durationMinutes;
    private int bufferMinutes;
    private Instant createdAt;
    private Instant updatedAt;
}
