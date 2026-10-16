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

@DisplayName("MicroLessonCriticAgent Unit Tests")
class MicroLessonCriticAgentTest {

    private MicroLessonCriticAgent criticAgent;

    @BeforeEach
    void setUp() {
        KaTexValidator kaTexValidator = new KaTexValidator();
        criticAgent = new MicroLessonCriticAgent(kaTexValidator);
    }

    private MicroLessonContentDto createValidPhysicsContent() {
        return new MicroLessonContentDto(
                "Moment of Inertia reflects rotational sluggishness depending on mass distribution.",
                "Students often neglect the parallel axis theorem when axis is not through the center of mass.",
                List.of(
                        "$$I_{cm} = \\frac{1}{2} M R^2$$",
                        "$$I = I_{cm} + M d^2$$"
                ),
                new StepByStepBreakdownDto(
                        "Calculate the moment of inertia about the tangent to a solid sphere.",
                        List.of(
                                "Step 1: Note that $I_{cm} = \\frac{2}{5} M R^2$ for a solid sphere.",
                                "Step 2: Apply parallel axis theorem $I = I_{cm} + M R^2 = \\frac{7}{5} M R^2$."
                        ),
                        "$$\\frac{7}{5} M R^2$$"
                )
        );
    }

    @Test
    @DisplayName("Approved when all rubrics are strictly satisfied")
    void testValidContentApproved() {
        MicroLessonContentDto content = new MicroLessonContentDto(
                "Moment of Inertia reflects rotational sluggishness depending on mass distribution.",
                "Students often neglect the parallel axis theorem when axis is not through the center of mass.",
                List.of(
                        "$$I_{cm} = \\frac{1}{2} M R^2$$",
                        "$$I = I_{cm} + M d^2$$"
                ),
                new StepByStepBreakdownDto(
                        "Calculate the moment of inertia about the tangent to a solid sphere.",
                        List.of(
                                "Step 1: Note that $$I_{cm} = \\frac{2}{5} M R^2$$ for a solid sphere.",
                                "Step 2: Apply parallel axis theorem $$I = I_{cm} + M R^2 = \\frac{7}{5} M R^2$$."
                        ),
                        "$$\\frac{7}{5} M R^2$$"
                )
        );

        MicroLessonCriticResult result = criticAgent.review(content, "Physics", "Mechanics", "Moment of Inertia");
        assertThat(result.isApproved()).isTrue();
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.issues()).isEmpty();
        assertThat(result.verifiedRubrics()).hasSize(4);
    }

    @Test
    @DisplayName("Rubric 1: Fails on missing or blank structural fields")
    void testFailsOnStructuralIncompleteness() {
        MicroLessonContentDto incomplete = new MicroLessonContentDto(
                "",
                "Trap exists",
                List.of("$$a=b$$", "$$c=d$$"),
                new StepByStepBreakdownDto("Stem", List.of("Step 1", "Step 2"), null)
        );

        MicroLessonCriticResult result = criticAgent.review(incomplete);
        assertThat(result.isApproved()).isFalse();
        assertThat(result.issues()).anyMatch(i -> i.contains("Core concept is missing"));
        assertThat(result.issues()).anyMatch(i -> i.contains("final answer is missing"));
    }

    @Test
    @DisplayName("Rubric 2: Fails on prohibited \\(...\\), \\[...\\], single $, or unescaped %")
    void testFailsOnProhibitedLatexDelimiters() {
        MicroLessonContentDto badLatex = new MicroLessonContentDto(
                "Calculate interest rate \\(r\\) precisely.",
                "Forgetting single $x$ math delimiter.",
                List.of("$$P = 100%$$", "$$I = P r t$$"),
                new StepByStepBreakdownDto("Stem", List.of("Step 1", "Step 2"), "Ans")
        );

        MicroLessonCriticResult result = criticAgent.review(badLatex);
        assertThat(result.isApproved()).isFalse();
        assertThat(result.issues()).anyMatch(i -> i.contains("Prohibited LaTeX delimiter '\\(...\\)'"));
        assertThat(result.issues()).anyMatch(i -> i.contains("Prohibited single '$' delimiter"));
        assertThat(result.issues()).anyMatch(i -> i.contains("Unescaped '%' inside KaTeX block"));
    }

    @Test
    @DisplayName("Rubric 3: Fails on domain mismatch when non-physics topic contains rotational mechanics formulas")
    void testFailsOnDomainMismatch() {
        MicroLessonContentDto mismatched = new MicroLessonContentDto(
                "Chemical Equilibrium relates reaction quotient to equilibrium constant. Indian bicycle chain tension analogy applies.",
                "Students confuse Q with K.",
                List.of("$$\\Delta K + \\Delta U = W_{ext}$$", "$$I = \\int r^2 \\, dm$$"),
                new StepByStepBreakdownDto("Stem", List.of("Step 1", "Step 2"), "4.5 \\times 10^3 \\text{ J}")
        );

        MicroLessonCriticResult result = criticAgent.review(mismatched, "Chemistry", "Equilibrium", "Chemical Equilibrium");
        assertThat(result.isApproved()).isFalse();
        assertThat(result.issues()).anyMatch(i -> i.contains("Domain mismatch: non-physics topic"));
    }

    @Test
    @DisplayName("Rubric 4: Fails on insufficient counts (< 2 cheat sheet items or < 2 steps)")
    void testFailsOnInsufficientCounts() {
        MicroLessonContentDto sparse = new MicroLessonContentDto(
                "Core concept text",
                "Trap note text",
                List.of("$$a = b$$"), // Only 1 cheatSheet item
                new StepByStepBreakdownDto("Stem", List.of("Step 1: Sole step"), "Answer") // Only 1 step
        );

        MicroLessonCriticResult result = criticAgent.review(sparse);
        assertThat(result.isApproved()).isFalse();
        assertThat(result.issues()).anyMatch(i -> i.contains("Cheat sheet must contain at least 2 items"));
        assertThat(result.issues()).anyMatch(i -> i.toLowerCase().contains("breakdown must contain at least 2 steps"));
    }
}
