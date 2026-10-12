// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.service;

import com.examplatform.questionbank.microlesson.dto.MicroLessonContentDto;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KaTexValidator KaTeX & LaTeX Syntax Verification Tests")
class KaTexValidatorTest {

    private KaTexValidator validator;

    @BeforeEach
    void setUp() {
        validator = new KaTexValidator();
    }

    @Test
    @DisplayName("Valid mathematical expression with balanced delimiters passes validation")
    void testValidMathExpressions() {
        List<String> errors1 = validator.validateLatexExpression("$E = mc^2$");
        assertThat(errors1).isEmpty();

        List<String> errors2 = validator.validateLatexExpression("$$I = \\int_{0}^{R} r^2 \\, dm$$");
        assertThat(errors2).isEmpty();

        List<String> errors3 = validator.validateLatexExpression("The rotational inertia is given by $I = \\frac{1}{2} M R^2$.");
        assertThat(errors3).isEmpty();
    }

    @Test
    @DisplayName("Unbalanced inline dollar delimiter triggers validation error")
    void testUnbalancedSingleDollar() {
        List<String> errors = validator.validateLatexExpression("Formula is $E = mc^2 without closing dollar");
        assertThat(errors).anyMatch(e -> e.contains("inline math delimiter '$'"));
    }

    @Test
    @DisplayName("Unbalanced display double-dollar delimiter triggers validation error")
    void testUnbalancedDoubleDollar() {
        List<String> errors = validator.validateLatexExpression("$$I = \\int r^2 dm");
        assertThat(errors).anyMatch(e -> e.contains("display math delimiter '$$'"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\\frac{a}{b",
            "\\sqrt{x + y",
            "A = [1, 2, 3"
    })
    @DisplayName("Unclosed braces and brackets trigger validation error")
    void testUnclosedDelimiters(String expr) {
        List<String> errors = validator.validateLatexExpression(expr);
        assertThat(errors).isNotEmpty();
    }

    @Test
    @DisplayName("Complete valid MicroLessonContentDto passes validation")
    void testCompleteValidContent() {
        MicroLessonContentDto content = new MicroLessonContentDto(
                "Centripetal acceleration pulls inward toward the center.",
                "Students often confuse centrifugal fictitious force with centripetal force.",
                List.of("$a_c = \\frac{v^2}{r}$", "$$F_c = m \\omega^2 r$$"),
                new StepByStepBreakdownDto(
                        "Find $a_c$ for $v = 10\\text{ m/s}$ and $r = 2\\text{ m}$.",
                        List.of("Step 1: Write equation $a_c = \\frac{v^2}{r}$", "Step 2: Substitute values"),
                        "50 \\text{ m/s}^2"
                )
        );

        List<String> errors = validator.validateContent(content);
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Missing core concept or trap triggers validation error")
    void testMissingFields() {
        MicroLessonContentDto content = new MicroLessonContentDto(
                "",
                null,
                List.of("$a=b$"),
                new StepByStepBreakdownDto("Stem", List.of("Step 1"), "Ans")
        );

        List<String> errors = validator.validateContent(content);
        assertThat(errors).anyMatch(e -> e.contains("Core concept"));
        assertThat(errors).anyMatch(e -> e.contains("Trap"));
    }
}
