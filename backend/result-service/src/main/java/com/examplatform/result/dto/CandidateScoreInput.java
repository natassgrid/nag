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

package com.examplatform.result.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Input DTO carrying a single candidate's raw scores for result computation.
 * Contains the raw total score, section-wise breakdown, and shift statistics
 * needed for normalization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateScoreInput {

    private UUID candidateId;

    private UUID examId;

    private String shiftId;

    /**
     * Sum of all evaluation scores for this candidate.
     */
    private double totalRawScore;

    /**
     * Section name → score mapping.
     */
    private Map<String, Double> sectionScores;

    /**
     * Section breakdowns containing sectionId, sectionName, subjectId, score, accuracy, etc.
     */
    private List<SectionBreakdownDto> sectionBreakdowns;

    /**
     * Topic breakdowns containing subjectId, topicId, score, accuracy, etc.
     */
    private List<TaxonomyBreakdownDto> topicBreakdowns;

    /**
     * Subtopic breakdowns containing subjectId, topicId, subtopicId, score, accuracy, etc.
     */
    private List<TaxonomyBreakdownDto> subtopicBreakdowns;

    /**
     * Question-level score details.
     */
    private List<Map<String, Object>> questionLevelScores;

    /**
     * Overall accuracy rate (correct / attempted * 100).
     */
    private Double accuracyRate;

    /**
     * Mean score of all candidates in this candidate's shift (for normalization).
     * 0 if not applicable.
     */
    private double shiftMean;

    /**
     * Standard deviation of scores in this candidate's shift (for normalization).
     * 0 if not applicable.
     */
    private double shiftStdDev;
}
