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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating or updating seat allocation for a shift at a centre.
 * Validates: Requirements 7b.6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatAllocationRequest {

    @NotNull
    private UUID centreId;

    @Min(0)
    private int totalSeats;

    @Min(0)
    private int availableSeats;

    @Min(0)
    private int reservedSeats;

    @Min(0)
    private int pwdSeats;

    @Min(0)
    private int emergencyBufferSeats;

    @Min(0)
    private int femaleReservedSeats;

    @Min(0)
    private int specialCategorySeats;
}
