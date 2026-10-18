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

package com.examplatform.questionbank.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Request DTO for blueprint question matching from Paper Generator using numeric IDs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlueprintMatchRequest implements Serializable {

    @JsonProperty("subjectId")
    @JsonAlias({"subject_id"})
    private Long subjectId;

    @JsonProperty("topicId")
    @JsonAlias({"topic_id"})
    private Long topicId;

    @JsonProperty("subtopicId")
    @JsonAlias({"subtopic_id"})
    private Long subtopicId;

    private String subject;
    private String topic;
    private String subtopic;

    private String difficulty;
    private String cognitiveLevel;
}
