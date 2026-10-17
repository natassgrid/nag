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

    /**
     * Validates that formulas in core concept, trap, cheat sheet, step-by-step breakdown,
     * takeaways, and quick-check items have syntactically sound KaTeX / LaTeX expressions
     * (balanced delimiters and braces).
     *
     * @return list of validation errors, empty if valid
     */
    public List<String> validateContent(MicroLessonContentDto content) {
        List<String> errors = new ArrayList<>();
        if (content == null) {
            errors.add("Content payload cannot be null");
            return errors;
        }

        if (content.coreConcept() == null || content.coreConcept().isBlank()) {
            errors.add("Core concept cannot be empty");
        } else {
            for (String err : validateLatexExpression(content.coreConcept())) {
                errors.add("CoreConcept: " + err);
            }
        }

        if (content.trap() == null || content.trap().isBlank()) {
            errors.add("Trap description cannot be empty");
        } else {
            for (String err : validateLatexExpression(content.trap())) {
                errors.add("Trap: " + err);
            }
        }

        if (content.cheatSheet() == null || content.cheatSheet().isEmpty()) {
            errors.add("Cheat sheet must contain at least 1 formula or framework");
        } else {
            for (int i = 0; i < content.cheatSheet().size(); i++) {
                String formula = content.cheatSheet().get(i);
                List<String> exprErrors = validateLatexExpression(formula);
                for (String err : exprErrors) {
                    errors.add("CheatSheet item " + (i + 1) + ": " + err);
                }
            }
        }

        StepByStepBreakdownDto breakdown = content.stepByStepBreakdown();
        if (breakdown == null) {
            errors.add("Step-by-step breakdown cannot be null");
        } else {
            if (breakdown.questionStem() == null || breakdown.questionStem().isBlank()) {
                errors.add("Breakdown question stem cannot be empty");
            } else {
                for (String err : validateLatexExpression(breakdown.questionStem())) {
                    errors.add("Breakdown questionStem: " + err);
                }
            }
            if (breakdown.steps() == null || breakdown.steps().isEmpty()) {
                errors.add("Breakdown must contain at least one step");
            } else {
                for (int i = 0; i < breakdown.steps().size(); i++) {
                    String step = breakdown.steps().get(i);
                    List<String> stepErrors = validateLatexExpression(step);
                    for (String err : stepErrors) {
                        errors.add("Step " + (i + 1) + ": " + err);
                    }
                }
            }
            if (breakdown.finalAnswer() == null || breakdown.finalAnswer().isBlank()) {
                errors.add("Breakdown final answer cannot be empty");
            } else {
                for (String err : validateLatexExpression(breakdown.finalAnswer())) {
                    errors.add("Breakdown finalAnswer: " + err);
                }
            }

            if (breakdown.structuredSteps() != null) {
                for (int i = 0; i < breakdown.structuredSteps().size(); i++) {
                    StepItemDto item = breakdown.structuredSteps().get(i);
                    if (item == null) continue;
                    validateOptionalField(item.explanation(), "StructuredStep " + (i + 1) + " explanation", errors);
                    validateOptionalField(item.formula(), "StructuredStep " + (i + 1) + " formula", errors);
                    validateOptionalField(item.tip(), "StructuredStep " + (i + 1) + " tip", errors);
                }
            }

            TrapAlertDto trapDto = breakdown.structuredTrapAlert();
            if (trapDto != null) {
                validateOptionalField(trapDto.description(), "TrapAlert description", errors);
                validateOptionalField(trapDto.commonMistake(), "TrapAlert commonMistake", errors);
                validateOptionalField(trapDto.correctApproach(), "TrapAlert correctApproach", errors);
            }

            if (breakdown.structuredFormulas() != null) {
                for (int i = 0; i < breakdown.structuredFormulas().size(); i++) {
                    FormulaEntryDto f = breakdown.structuredFormulas().get(i);
                    if (f == null) continue;
                    validateOptionalField(f.latex(), "Formula " + (i + 1) + " latex", errors);
                    validateOptionalField(f.description(), "Formula " + (i + 1) + " description", errors);
                }
            }

            if (breakdown.keyTakeaways() != null) {
                for (int i = 0; i < breakdown.keyTakeaways().size(); i++) {
                    validateOptionalField(breakdown.keyTakeaways().get(i), "KeyTakeaway " + (i + 1), errors);
                }
            }

            if (breakdown.quickCheckItems() != null) {
                for (int i = 0; i < breakdown.quickCheckItems().size(); i++) {
                    QuickCheckItemDto q = breakdown.quickCheckItems().get(i);
                    if (q == null) continue;
                    validateOptionalField(q.question(), "QuickCheck " + (i + 1) + " question", errors);
                    validateOptionalField(q.correctAnswer(), "QuickCheck " + (i + 1) + " answer", errors);
                    validateOptionalField(q.explanation(), "QuickCheck " + (i + 1) + " explanation", errors);
                    if (q.options() != null) {
                        for (int j = 0; j < q.options().size(); j++) {
                            validateOptionalField(q.options().get(j), "QuickCheck " + (i + 1) + " option " + (j + 1), errors);
                        }
                    }
                }
            }
        }

        return errors;
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

