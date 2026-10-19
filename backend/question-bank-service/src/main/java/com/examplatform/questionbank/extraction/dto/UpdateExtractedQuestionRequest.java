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

package com.examplatform.questionbank.extraction.dto;

import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.extraction.domain.ExtractedQuestionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request payload for updating or reviewing an extracted question staging record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateExtractedQuestionRequest {

    private Integer questionNumber;
    private String sectionName;
    private String questionType;
    private String content;
    private List<QuestionOption> options;
    private String answerKey;
    private String explanation;
    private String passageText;
    private Map<String, Map<String, Object>> translations;
    private Long suggestedSubjectId;
    private Long suggestedTopicId;
    private Long suggestedSubtopicId;
    private String suggestedDifficulty;
    private String suggestedCognitiveLevel;
    private ExtractedQuestionStatus status;
    private String reviewComments;
}
