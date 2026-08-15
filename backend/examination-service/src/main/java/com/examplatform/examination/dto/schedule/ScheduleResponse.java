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
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for an examination schedule.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {

    private UUID id;
    private UUID examinationId;
    private String scheduleName;
    private int scheduleVersion;
    private String notificationNumber;
    private LocalDate examDate;
    private LocalDate reserveDate;
    private String timeZone;
    private String status;
    private String changeReason;
    private LocalDate effectiveFrom;
    private UUID previousVersionId;
    private UUID createdBy;
    private UUID modifiedBy;
    private UUID approvedBy;
    private Instant approvedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
