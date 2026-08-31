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

package com.examplatform.candidate.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for creating or updating a candidate educational detail record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateEducationRequest {

    @NotBlank(message = "Qualification is required")
    private String qualification;

    private String courseName;

    @NotBlank(message = "Board or University is required")
    private String boardOrUniversity;

    private String institutionName;

    @NotNull(message = "Passing year is required")
    @Min(value = 1950, message = "Passing year must be valid (>= 1950)")
    @Max(value = 2100, message = "Passing year must be valid (<= 2100)")
    private Integer passingYear;

    private BigDecimal percentageOrCgpa;

    private String gradeOrDivision;

    private String specialization;

    private String rollNumber;

    private UUID certificateAssetId;
}
