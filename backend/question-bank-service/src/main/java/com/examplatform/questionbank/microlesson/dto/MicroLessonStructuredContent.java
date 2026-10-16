// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MicroLessonStructuredContent(
        String coreConcept,
        TrapAlertDto trapAlert,
        List<FormulaEntryDto> formulaCheatSheet,
        List<StepItemDto> stepByStepBreakdown,
        String problemStatement,
        String finalAnswer,
        List<String> keyTakeaways,
        List<QuickCheckItemDto> quickCheckItems
) implements Serializable {}
