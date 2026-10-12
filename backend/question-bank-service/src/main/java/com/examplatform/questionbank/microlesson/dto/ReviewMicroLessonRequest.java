// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReviewMicroLessonRequest(
        String status, // APPROVED, PUBLISHED, REJECTED, IN_REVIEW
        String comments,
        String coreConcept,
        String trap,
        List<String> cheatSheet,
        StepByStepBreakdownDto stepByStepBreakdown
) implements Serializable {}
