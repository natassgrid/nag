// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReviewMicroLessonRequest(
        @JsonAlias({"action"})
        String status, // APPROVED, PUBLISHED, REJECTED, IN_REVIEW
        @JsonAlias({"reviewerNotes"})
        String comments,
        String coreConcept,
        @JsonAlias({"trapAlert"})
        String trap,
        @JsonAlias({"formulaCheatSheet"})
        List<String> cheatSheet,
        StepByStepBreakdownDto stepByStepBreakdown
) implements Serializable {}
