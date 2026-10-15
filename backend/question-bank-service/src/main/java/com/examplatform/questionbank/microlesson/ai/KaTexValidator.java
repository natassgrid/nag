// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.ai;

import com.examplatform.questionbank.microlesson.dto.MicroLessonContentDto;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import com.examplatform.shared.ai.latex.LatexSyntaxValidator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class KaTexValidator {

    /**
     * Validates that formulas in cheat sheet and step-by-step breakdown
     * have syntactically sound KaTeX / LaTeX expressions (balanced delimiters and braces).
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
        }

        if (content.trap() == null || content.trap().isBlank()) {
            errors.add("Trap description cannot be empty");
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
            }
        }

        return errors;
    }

    /**
     * Validates an individual LaTeX or KaTeX expression using the shared AI latex syntax validator.
     */
    public List<String> validateLatexExpression(String text) {
        return LatexSyntaxValidator.validate(text);
    }
}
