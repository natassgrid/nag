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

package com.examplatform.delivery.dto;

import com.examplatform.shared.constants.QuestionTypeConstants;
import com.examplatform.shared.constants.SharedConstants;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * POJO representing a single question item in an examination paper definition.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaperQuestionPayload {

    @JsonAlias({SharedConstants.QUESTION_ID, "question_id"})
    private String id;

    @JsonAlias("text")
    private String content;

    @JsonAlias({"section_id", "sectionId"})
    private UUID sectionId;

    @JsonAlias({"section_name", "sectionName"})
    private String sectionName;

    @JsonAlias({"subject_id", "subjectId"})
    private Long subjectId;

    @JsonAlias(SharedConstants.SUBJECT)
    private String subject;

    @JsonAlias({"topic_id", "topicId"})
    private Long topicId;

    @Builder.Default
    private String topic = "General";

    @JsonAlias({"subtopic_id", "subtopicId"})
    private Long subtopicId;

    @JsonAlias({"subtopic", "subtopic_name"})
    private String subtopic;

    private String explanation;

    @JsonAlias("answer_key")
    private String answerKey;

    private String imageUrl;
    private String imageAltText;
    private Boolean hasImages;

    @Builder.Default
    @JsonAlias("question_type")
    private String questionType = QuestionTypeConstants.SINGLE_MCQ;

    @JsonAlias("passage_id")
    private String passageId;

    @JsonAlias("passage_content")
    private String passageContent;

    @JsonAlias("passage_order_index")
    private Integer passageOrderIndex;

    private Integer correctOptionIndex;

    private List<PaperOptionPayload> options;

    @Builder.Default
    private Double marks = 2.0;

    @Builder.Default
    private Double negativeMarks = 0.5;
}
