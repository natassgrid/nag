/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.candidate.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmitDiagnosticRequest(
        List<DiagnosticAnswerDto> responses,
        List<DiagnosticAnswerDto> answers,
        @JsonProperty("isSkip")
        @JsonAlias({"skip"})
        boolean isSkip
) {
    public SubmitDiagnosticRequest(List<DiagnosticAnswerDto> responses, boolean isSkip) {
        this(responses, null, isSkip);
    }

    public List<DiagnosticAnswerDto> effectiveResponses() {
        if (responses != null && !responses.isEmpty()) {
            return responses;
        }
        return answers != null ? answers : Collections.emptyList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DiagnosticAnswerDto(
            UUID questionId,
            Long subtopicId,
            List<String> selectedOptionIds,
            String selectedOptionId,
            long timeSpentMs,
            Integer timeSpentSeconds
    ) {
        public DiagnosticAnswerDto(
                UUID questionId,
                Long subtopicId,
                List<String> selectedOptionIds,
                long timeSpentMs
        ) {
            this(questionId, subtopicId, selectedOptionIds, null, timeSpentMs, null);
        }

        public List<String> effectiveSelectedOptionIds() {
            if (selectedOptionIds != null && !selectedOptionIds.isEmpty()) {
                return selectedOptionIds;
            }
            if (selectedOptionId != null && !selectedOptionId.isBlank()) {
                return List.of(selectedOptionId);
            }
            return Collections.emptyList();
        }

        public long effectiveTimeSpentMs() {
            if (timeSpentMs > 0) {
                return timeSpentMs;
            }
            return timeSpentSeconds != null && timeSpentSeconds > 0 ? timeSpentSeconds * 1000L : 0L;
        }
    }
}
