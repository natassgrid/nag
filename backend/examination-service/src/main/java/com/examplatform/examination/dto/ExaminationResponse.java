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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for examination configuration data.
 *
 * Validates: Requirements 7.1, 7.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExaminationResponse {

    private UUID id;
    private String name;
    private int durationMinutes;
    private int totalMarks;
    private boolean negativeMarkingEnabled;
    private double negativeMarkingValue;
    private String navigationPolicy;
    private String calculatorPolicy;
    private boolean reviewFlagEnabled;
    private List<Section> sections;
    private String status;
    private LocalDateTime createdAt;
}
