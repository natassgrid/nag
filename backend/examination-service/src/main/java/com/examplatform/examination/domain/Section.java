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

package com.examplatform.examination.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Section POJO used in requests and responses.
 * Represents an examination section bound to a canonical taxonomy subject.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Section {
    private UUID id;
    private Long subjectId;
    private String name;
    private String subject;
    private Integer orderIndex;
    private Integer questionCount;
    private Double marksPerQuestion;
    private Double negativeMarks;
    private Double negativeMarksPerQuestion;
    private Integer durationMinutes;
    private List<SubjectTopicRule> topicRules;

    public Double getEffectiveNegativeMarks() {
        if (negativeMarks != null) return negativeMarks;
        if (negativeMarksPerQuestion != null) return negativeMarksPerQuestion;
        return 0.0;
    }
}
