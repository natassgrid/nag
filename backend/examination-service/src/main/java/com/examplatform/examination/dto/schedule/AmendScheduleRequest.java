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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for amending a Published examination schedule.
 * Change reason is mandatory per Req 7b.8.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmendScheduleRequest {

    @NotBlank(message = "changeReason is mandatory when amending a published schedule")
    @Size(max = 1000)
    private String changeReason;

    @NotBlank
    @Size(max = 255)
    private String scheduleName;

    @Size(max = 100)
    private String notificationNumber;

    @NotNull
    private LocalDate examDate;

    private LocalDate reserveDate;

    private LocalDate effectiveFrom;

    @Builder.Default
    private String timeZone = "Asia/Kolkata";
}
