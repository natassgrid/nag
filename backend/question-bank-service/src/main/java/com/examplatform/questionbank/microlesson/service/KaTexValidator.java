// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.service;

import com.examplatform.questionbank.microlesson.dto.MicroLessonContentDto;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

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

    public List<String> validateLatexExpression(String text) {
        List<String> errors = new ArrayList<>();
        if (text == null || text.isBlank()) return errors;

        // 1. Check balanced dollar delimiters (ignoring escaped ones: \$)
        int singleDollarCount = 0;
        int doubleDollarCount = 0;
        int len = text.length();

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                i++; // skip escaped character
                continue;
            }
            if (c == '$') {
                if (i + 1 < len && text.charAt(i + 1) == '$') {
                    doubleDollarCount++;
                    i++;
                } else {
                    singleDollarCount++;
                }
            }
        }

        if (singleDollarCount % 2 != 0) {
            errors.add("Unbalanced inline math delimiter '$' in expression");
        }
        if (doubleDollarCount % 2 != 0) {
            errors.add("Unbalanced display math delimiter '$$' in expression");
        }

        // 2. Check balanced curly braces '{' and '}' and brackets '[' and ']'
        Stack<Character> stack = new Stack<>();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                i++;
                continue;
            }
            if (c == '{' || c == '[') {
                stack.push(c);
            } else if (c == '}') {
                if (stack.isEmpty() || stack.pop() != '{') {
                    errors.add("Unbalanced or mismatched closing brace '}' in expression");
                    break;
                }
            } else if (c == ']') {
                if (stack.isEmpty() || stack.pop() != '[') {
                    errors.add("Unbalanced or mismatched closing bracket ']' in expression");
                    break;
                }
            }
        }
        if (!stack.isEmpty()) {
            char unclosed = stack.pop();
            errors.add("Unclosed delimiter '" + unclosed + "' in expression");
        }

        return errors;
    }
}
