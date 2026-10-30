// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.ai;

import com.examplatform.questionbank.microlesson.dto.FormulaEntryDto;
import com.examplatform.questionbank.microlesson.dto.MicroLessonContentDto;
import com.examplatform.questionbank.microlesson.dto.QuickCheckItemDto;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import com.examplatform.questionbank.microlesson.dto.StepItemDto;
import com.examplatform.questionbank.microlesson.dto.TrapAlertDto;
import com.examplatform.shared.ai.latex.LatexSyntaxValidator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class KaTexValidator {

    private static final String STRUCTURED_STEP_PREFIX = "StructuredStep ";
    private static final String QUICK_CHECK_PREFIX = "QuickCheck ";

    /**
     * Validates that formulas in core concept, trap, cheat sheet, step-by-step breakdown,
     * takeaways, and quick-check items have syntactically sound KaTeX / LaTeX expressions
     * (balanced delimiters and braces).
     *
     * @return list of validation errors, empty if valid
     */
    public List<String> validateContent(MicroLessonContentDto content) {
        return validateContent(content, true);
    }

    /**
     * Validates micro-lesson content with configurable requirement for Part 3 (Cheat Sheet).
     *
     * @param content            the micro-lesson content DTO
     * @param requireCheatSheet  true if at least 1 formula is required (Math, Science, Numerical);
     *                           false if Part 3 is optional for the subject/topic
     * @return list of validation errors, empty if valid
     */
    public List<String> validateContent(MicroLessonContentDto content, boolean requireCheatSheet) {
        List<String> errors = new ArrayList<>();
        if (content == null) {
            errors.add("Content payload cannot be null");
            return errors;
        }

        validateCoreConcept(content.coreConcept(), errors);
        validateTrap(content.trap(), errors);
        validateCheatSheet(content.cheatSheet(), requireCheatSheet, errors);
        validateBreakdown(content.stepByStepBreakdown(), errors);

        return errors;
    }

    private void validateCoreConcept(String coreConcept, List<String> errors) {
        if (coreConcept == null || coreConcept.isBlank()) {
            errors.add("Core concept cannot be empty");
            return;
        }
        for (String err : validateLatexExpression(coreConcept)) {
            errors.add("CoreConcept: " + err);
        }
    }

    private void validateTrap(String trap, List<String> errors) {
        if (trap == null || trap.isBlank()) {
            errors.add("Trap description cannot be empty");
            return;
        }
        for (String err : validateLatexExpression(trap)) {
            errors.add("Trap: " + err);
        }
    }

    private void validateCheatSheet(List<String> cheatSheet, boolean requireCheatSheet, List<String> errors) {
        if (cheatSheet == null || cheatSheet.isEmpty()) {
            if (requireCheatSheet) {
                errors.add("Cheat sheet must contain at least 1 formula or framework");
            }
            return;
        }
        for (int i = 0; i < cheatSheet.size(); i++) {
            List<String> exprErrors = validateLatexExpression(cheatSheet.get(i));
            for (String err : exprErrors) {
                errors.add("CheatSheet item " + (i + 1) + ": " + err);
            }
        }
    }

    private void validateBreakdown(StepByStepBreakdownDto breakdown, List<String> errors) {
        if (breakdown == null) {
            errors.add("Step-by-step breakdown cannot be null");
            return;
        }

        validateBreakdownCore(breakdown, errors);
        validateStructuredSteps(breakdown.structuredSteps(), errors);
        validateTrapAlert(breakdown.structuredTrapAlert(), errors);
        validateStructuredFormulas(breakdown.structuredFormulas(), errors);
        validateKeyTakeaways(breakdown.keyTakeaways(), errors);
        validateQuickCheckItems(breakdown.quickCheckItems(), errors);
    }

    private void validateBreakdownCore(StepByStepBreakdownDto breakdown, List<String> errors) {
        validateQuestionStem(breakdown.questionStem(), errors);
        validateSteps(breakdown.steps(), errors);
        validateFinalAnswer(breakdown.finalAnswer(), errors);
    }

    private void validateQuestionStem(String questionStem, List<String> errors) {
        if (questionStem == null || questionStem.isBlank()) {
            errors.add("Breakdown question stem cannot be empty");
            return;
        }
        for (String err : validateLatexExpression(questionStem)) {
            errors.add("Breakdown questionStem: " + err);
        }
    }

    private void validateSteps(List<String> steps, List<String> errors) {
        if (steps == null || steps.isEmpty()) {
            errors.add("Breakdown must contain at least one step");
            return;
        }
        for (int i = 0; i < steps.size(); i++) {
            for (String err : validateLatexExpression(steps.get(i))) {
                errors.add("Step " + (i + 1) + ": " + err);
            }
        }
    }

    private void validateFinalAnswer(String finalAnswer, List<String> errors) {
        if (finalAnswer == null || finalAnswer.isBlank()) {
            errors.add("Breakdown final answer cannot be empty");
            return;
        }
        for (String err : validateLatexExpression(finalAnswer)) {
            errors.add("Breakdown finalAnswer: " + err);
        }
    }

    private void validateStructuredSteps(List<StepItemDto> structuredSteps, List<String> errors) {
        if (structuredSteps == null) {
            return;
        }
        for (int i = 0; i < structuredSteps.size(); i++) {
            StepItemDto item = structuredSteps.get(i);
            if (item != null) {
                int stepNum = i + 1;
                validateOptionalField(item.explanation(), STRUCTURED_STEP_PREFIX + stepNum + " explanation", errors);
                validateOptionalField(item.formula(), STRUCTURED_STEP_PREFIX + stepNum + " formula", errors);
                validateOptionalField(item.tip(), STRUCTURED_STEP_PREFIX + stepNum + " tip", errors);
            }
        }
    }

    private void validateTrapAlert(TrapAlertDto trapDto, List<String> errors) {
        if (trapDto != null) {
            validateOptionalField(trapDto.description(), "TrapAlert description", errors);
            validateOptionalField(trapDto.commonMistake(), "TrapAlert commonMistake", errors);
            validateOptionalField(trapDto.correctApproach(), "TrapAlert correctApproach", errors);
        }
    }

    private void validateStructuredFormulas(List<FormulaEntryDto> formulas, List<String> errors) {
        if (formulas == null) {
            return;
        }
        for (int i = 0; i < formulas.size(); i++) {
            FormulaEntryDto f = formulas.get(i);
            if (f != null) {
                validateOptionalField(f.latex(), "Formula " + (i + 1) + " latex", errors);
                validateOptionalField(f.description(), "Formula " + (i + 1) + " description", errors);
            }
        }
    }

    private void validateKeyTakeaways(List<String> keyTakeaways, List<String> errors) {
        if (keyTakeaways == null) {
            return;
        }
        for (int i = 0; i < keyTakeaways.size(); i++) {
            validateOptionalField(keyTakeaways.get(i), "KeyTakeaway " + (i + 1), errors);
        }
    }

    private void validateQuickCheckItems(List<QuickCheckItemDto> quickCheckItems, List<String> errors) {
        if (quickCheckItems == null) {
            return;
        }
        for (int i = 0; i < quickCheckItems.size(); i++) {
            QuickCheckItemDto q = quickCheckItems.get(i);
            if (q != null) {
                validateSingleQuickCheckItem(q, i + 1, errors);
            }
        }
    }

    private void validateSingleQuickCheckItem(QuickCheckItemDto q, int index, List<String> errors) {
        validateOptionalField(q.question(), QUICK_CHECK_PREFIX + index + " question", errors);
        validateOptionalField(q.correctAnswer(), QUICK_CHECK_PREFIX + index + " answer", errors);
        validateOptionalField(q.explanation(), QUICK_CHECK_PREFIX + index + " explanation", errors);
        if (q.options() != null) {
            for (int j = 0; j < q.options().size(); j++) {
                validateOptionalField(q.options().get(j), QUICK_CHECK_PREFIX + index + " option " + (j + 1), errors);
            }
        }
    }

    private void validateOptionalField(String text, String label, List<String> errors) {
        if (text != null && !text.isBlank()) {
            for (String err : validateLatexExpression(text)) {
                errors.add(label + ": " + err);
            }
        }
    }

    /**
     * Validates an individual LaTeX or KaTeX expression using the shared AI latex syntax validator.
     */
    public List<String> validateLatexExpression(String text) {
        return LatexSyntaxValidator.validate(text);
    }
}
