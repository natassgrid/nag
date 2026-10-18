// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MicroLessonFormulaParsingTest {

    @Test
    @DisplayName("Should NOT split on colon when colon is inside LaTeX math delimiters like \\text{Relation 2: ...}")
    void shouldNotSplitOnColonInsideLatexMathDelimiters() {
        String formulaWithColonInsideMath = "$$\\text{Relation 2: Verify dimensional homogeneity and boundary limits}$$";

        MicroLessonResponse response = new MicroLessonResponse(
                UUID.randomUUID(),
                1L, 1L, 1L,
                "Physics", "Mechanics", "Dimensional Analysis",
                "JEE_MAIN",
                "Core Concept",
                "Common Trap",
                List.of(formulaWithColonInsideMath),
                null,
                "APPROVED",
                null, null, null,
                1, null, null
        );

        List<FormulaEntryDto> formulas = response.getContent().formulaCheatSheet();
        assertThat(formulas).hasSize(1);
        FormulaEntryDto entry = formulas.get(0);

        assertThat(entry.name()).isEqualTo("Formula 1");
        assertThat(entry.latex()).isEqualTo(formulaWithColonInsideMath);
        assertThat(entry.name()).doesNotContain("$$\\text{Relation 2");
    }

    @Test
    @DisplayName("Should split on colon when colon separates formula name and latex expression")
    void shouldSplitOnColonWhenSeparatingNameAndFormula() {
        String namedFormula = "Ohm's Law: $$V = IR$$";

        MicroLessonResponse response = new MicroLessonResponse(
                UUID.randomUUID(),
                1L, 1L, 1L,
                "Physics", "Current Electricity", "Ohm's Law",
                "JEE_MAIN",
                "Core Concept",
                "Common Trap",
                List.of(namedFormula),
                null,
                "APPROVED",
                null, null, null,
                1, null, null
        );

        List<FormulaEntryDto> formulas = response.getContent().formulaCheatSheet();
        assertThat(formulas).hasSize(1);
        FormulaEntryDto entry = formulas.get(0);

        assertThat(entry.name()).isEqualTo("Ohm's Law");
        assertThat(entry.latex()).isEqualTo("$$V = IR$$");
    }

    @Test
    @DisplayName("StepByStepBreakdownDto should parse string formulas with and without LaTeX colons correctly")
    void stepByStepBreakdownDtoParsing() {
        String formulaWithColon = "$$\\text{Rule 1: Boundary condition}$$";
        String namedFormula = "Coulomb's Law: $$F = \\frac{1}{4\\pi \\epsilon_0}\\frac{q_1 q_2}{r^2}$$";

        StepByStepBreakdownDto dto = StepByStepBreakdownDto.fromJson(Map.of(
                "problemStatement", "Sample problem",
                "structuredFormulas", List.of(formulaWithColon, namedFormula)
        ));

        assertThat(dto).isNotNull();
        assertThat(dto.structuredFormulas()).hasSize(2);

        FormulaEntryDto entry1 = dto.structuredFormulas().get(0);
        assertThat(entry1.name()).isEqualTo("Formula 1");
        assertThat(entry1.latex()).isEqualTo(formulaWithColon);

        FormulaEntryDto entry2 = dto.structuredFormulas().get(1);
        assertThat(entry2.name()).isEqualTo("Coulomb's Law");
        assertThat(entry2.latex()).isEqualTo("$$F = \\frac{1}{4\\pi \\epsilon_0}\\frac{q_1 q_2}{r^2}$$");
    }
}
