/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

import java.util.List;
import java.util.UUID;

/**
 * Catalogue item representing a published examination available for learner enrollment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamCatalogueItemResponse {

    private UUID id;
    private String name;
    private String code;
    private String conductingAuthority;
    private String category;
    private String examinationType;
    private String academicYear;
    private int durationMinutes;
    private int totalMarks;
    private boolean negativeMarkingEnabled;
    private double negativeMarkingValue;
    private UUID blueprintId;
    private UUID parentExaminationId;
    private List<Section> sections;
    private int syllabusNodeCount;
    private String status;
}
