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

package com.examplatform.questionbank.microlesson.translation.dto;

import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MicroLessonTranslationResponse implements Serializable {

    private UUID id;
    private UUID microlessonId;
    private String languageCode;
    private String coreConcept;
    private String trap;
    private List<String> cheatSheet;
    private StepByStepBreakdownDto stepByStepBreakdown;
    private int sourceVersion;
    private String status;
    private UUID translatorId;
    private UUID reviewerId;
    private String reviewComments;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    @JsonProperty("translationId")
    public UUID getTranslationId() {
        return id;
    }

    @JsonProperty("lessonId")
    public UUID getLessonId() {
        return microlessonId;
    }
}
