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

package com.examplatform.examination.dto;

import com.examplatform.examination.domain.Section;
import com.examplatform.examination.domain.enums.CalculatorPolicy;
import com.examplatform.examination.domain.enums.NavigationPolicy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating or updating an examination configuration.
 *
 * Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExaminationRequest {

    @NotBlank
    private String name;

    private String code;

    private String conductingAuthority;

    private String category;

    private String examinationType;

    private String academicYear;

    private String examinationMode;

    @NotNull
    @Min(1)
    private Integer durationMinutes;

    @NotNull
    @Min(1)
    private Integer totalMarks;

    private Boolean negativeMarkingEnabled;

    private Double negativeMarkingValue;

    @NotNull
    private NavigationPolicy navigationPolicy;

    @NotNull
    private CalculatorPolicy calculatorPolicy;

    private Boolean reviewFlagEnabled;

    @NotEmpty
    private List<Section> sections;
}
