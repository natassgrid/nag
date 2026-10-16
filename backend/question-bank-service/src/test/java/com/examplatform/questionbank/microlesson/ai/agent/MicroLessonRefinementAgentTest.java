// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.ai.agent;

import com.examplatform.questionbank.microlesson.ai.KaTexValidator;
import com.examplatform.questionbank.microlesson.dto.MicroLessonContentDto;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MicroLessonRefinementAgent Unit Tests")
class MicroLessonRefinementAgentTest {

    private MicroLessonRefinementAgent refinementAgent;
    private MicroLessonCriticAgent criticAgent;

    @BeforeEach
    void setUp() {
        refinementAgent = new MicroLessonRefinementAgent();
        KaTexValidator validator = new KaTexValidator();
        criticAgent = new MicroLessonCriticAgent(validator);
    }

    @Test
    @DisplayName("Converts \\(...\\) and \\[...\\] to $$...$$")
    void testNormalizeParenthesisAndBracketDelimiters() {
        String input = "The kinetic energy is \\(K = \\frac{1}{2}mv^2\\) and potential is \\[U = mgh\\]";
        String refined = refinementAgent.refineText(input);

        assertThat(refined).contains("$$K = \\frac{1}{2}mv^2$$");
        assertThat(refined).contains("$$U = mgh$$");
        assertThat(refined).doesNotContain("\\(");
        assertThat(refined).doesNotContain("\\[");
    }

    @Test
    @DisplayName("Converts isolated single-dollar math delimiters to $$...$$")
    void testNormalizeSingleDollarDelimiters() {
        String input = "Consider velocity $v = 10\\text{ m/s}$ and acceleration $a = 2\\text{ m/s}^2$ with display $$F = ma$$.";
        String refined = refinementAgent.refineText(input);

        assertThat(refined).contains("$$v = 10\\text{ m/s}$$");
        assertThat(refined).contains("$$a = 2\\text{ m/s}^2$$");
        assertThat(refined).contains("$$F = ma$$");
        // Verify no stray isolated single dollars remain
        assertThat(refined).doesNotMatch("(?<![\\\\$])\\$(?!\\$)");
    }

    @Test
    @DisplayName("Escapes unescaped % inside $$...$$ blocks to \\%")
    void testEscapePercentInMathBlocks() {
        String input = "$$P = \\frac{part}{total} \\times 100%$$ and $$99.9%$$";
        String refined = refinementAgent.refineText(input);

        assertThat(refined).contains("100\\%$$");
        assertThat(refined).contains("99.9\\%$$");
    }

    @Test
    @DisplayName("Balances unclosed $$, braces, and brackets")
    void testBalanceDelimiters() {
        String unclosedDisplay = "$$E = mc^2";
        assertThat(refinementAgent.refineText(unclosedDisplay)).isEqualTo("$$E = mc^2$$");

        String unclosedBrace = "$$I = \\frac{1}{2} M R^{2$$";
        assertThat(refinementAgent.refineText(unclosedBrace)).isEqualTo("$$I = \\frac{1}{2} M R^{2}$$");
    }

    @Test
    @DisplayName("Wraps raw cheat sheet formulas with math notation in $$...$$")
    void testWrapCheatSheetFormulas() {
        String formula1 = "E = mc^2";
        String formula2 = "\\Delta U = Q - W";
        String formula3 = "2H2 + O2 -> 2H2O";
        String plainText = "Always verify boundary constraints";

        assertThat(refinementAgent.refineCheatSheetFormula(formula1)).isEqualTo("$$E = mc^2$$");
        assertThat(refinementAgent.refineCheatSheetFormula(formula2)).isEqualTo("$$\\Delta U = Q - W$$");
        assertThat(refinementAgent.refineCheatSheetFormula(formula3)).isEqualTo("$$2H2 + O2 -> 2H2O$$");
        assertThat(refinementAgent.refineCheatSheetFormula(plainText)).isEqualTo("Always verify boundary constraints");
    }

    @Test
    @DisplayName("Cleanly prefixes steps with Step 1:, Step 2:, etc.")
    void testNormalizeStepPrefixes() {
        assertThat(refinementAgent.normalizeStepPrefix("State governing equation", 1))
                .isEqualTo("Step 1: State governing equation");
        assertThat(refinementAgent.normalizeStepPrefix("1. State governing equation", 1))
                .isEqualTo("Step 1: State governing equation");
        assertThat(refinementAgent.normalizeStepPrefix("Step 1 - Substitute values", 2))
                .isEqualTo("Step 2: Substitute values");
    }

    @Test
    @DisplayName("End-to-end refinement turns non-compliant DTO into 100% compliant and approved DTO")
    void testEndToEndRefinementAndCriticApproval() {
        // Raw non-compliant DTO with prohibited delimiters, unescaped %, unprefixed steps, raw cheatSheet
        MicroLessonContentDto unrefined = new MicroLessonContentDto(
                "Efficiency \\(\\eta = \\frac{W}{Q_H}\\) indicates heat engine performance.",
                "Neglecting that efficiency cannot exceed Carnot limit $1 - \\frac{T_C}{T_H}$.",
                List.of(
                        "\\eta = 1 - \\frac{T_C}{T_H}",
                        "\\eta_{max} = 100%"
                ),
                new StepByStepBreakdownDto(
                        "Find Carnot efficiency for reservoirs at 300K and 600K.",
                        List.of(
                                "Convert temperatures to Kelvin if needed.",
                                "Compute \\(\\eta = 1 - \\frac{300}{600} = 50%\\)"
                        ),
                        "50%"
                )
        );

        // Verify critic initially rejects it
        MicroLessonCriticResult initialReview = criticAgent.review(unrefined);
        assertThat(initialReview.isApproved()).isFalse();
        assertThat(initialReview.issues()).isNotEmpty();

        // Refine with refinement agent
        MicroLessonContentDto refined = refinementAgent.refine(unrefined, initialReview);
        assertThat(refined).isNotNull();

        // Verify refined DTO now completely satisfies critic agent
        MicroLessonCriticResult postReview = criticAgent.review(refined);
        assertThat(postReview.isApproved()).isTrue();
        assertThat(postReview.score()).isEqualTo(1.0);
        assertThat(postReview.issues()).isEmpty();
        assertThat(postReview.verifiedRubrics()).hasSize(4);
    }
}
