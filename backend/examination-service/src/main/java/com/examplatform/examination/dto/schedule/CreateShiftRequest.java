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

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Request DTO for adding a shift to an examination schedule.
 * All timing invariants are enforced in the service layer.
 * Validates: Requirements 7b.2, 7b.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShiftRequest {

    @Min(1)
    private int shiftNumber;

    @Size(max = 100)
    private String shiftName;

    @NotNull
    private LocalTime reportingTime;

    @NotNull
    private LocalTime gateClosingTime;

    @NotNull
    private LocalTime loginStartTime;

    @NotNull
    private LocalTime examStartTime;

    @NotNull
    private LocalTime examEndTime;

    private LocalTime exitTime;

    @Min(1)
    private int durationMinutes;

    @Min(0)
    private int bufferMinutes;
}
