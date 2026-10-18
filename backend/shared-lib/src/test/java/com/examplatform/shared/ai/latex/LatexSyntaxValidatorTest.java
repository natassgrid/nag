/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 */

package com.examplatform.shared.ai.latex;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LatexSyntaxValidatorTest {

    @Test
    @DisplayName("Validates balanced single dollar inline math")
    void testBalancedDollarMath() {
        assertTrue(LatexSyntaxValidator.isValid("Let $x = 5$ and $y = 10$."));
        assertFalse(LatexSyntaxValidator.isValid("Let $x = 5 and $y = 10$.")); // 3 dollars -> unbalanced
    }

    @Test
    @DisplayName("Validates balanced double dollar display math")
    void testBalancedDisplayDollarMath() {
        assertTrue(LatexSyntaxValidator.isValid("Evaluate $$\\int_0^1 x dx$$ now."));
        List<String> errors = LatexSyntaxValidator.validate("Evaluate $$\\int_0^1 x dx$ now.");
        assertFalse(errors.isEmpty());
    }

    @Test
    @DisplayName("Validates balanced braces and brackets")
    void testBalancedBracesAndBrackets() {
        assertTrue(LatexSyntaxValidator.isValid("Formula $\\frac{a}{b} + [c]$ is correct."));
        assertFalse(LatexSyntaxValidator.isValid("Formula $\\frac{a}{b + [c]$ is missing brace."));
        assertFalse(LatexSyntaxValidator.isValid("Formula $\\frac{a}{b} + [c$ is missing bracket."));
    }

    @Test
    @DisplayName("Validates LaTeX environments \\begin and \\end")
    void testBalancedEnvironments() {
        String validMatrix = "\\begin{matrix} 1 & 0 \\\\ 0 & 1 \\end{matrix}";
        assertTrue(LatexSyntaxValidator.isValid(validMatrix));

        String mismatchedMatrix = "\\begin{matrix} 1 & 0 \\\\ 0 & 1 \\end{pmatrix}";
        List<String> errors = LatexSyntaxValidator.validate(mismatchedMatrix);
        assertFalse(errors.isEmpty());

        String unclosedEnv = "\\begin{cases} x & x > 0 \\\\ -x & x \\le 0";
        List<String> unclosedErrors = LatexSyntaxValidator.validate(unclosedEnv);
        assertFalse(unclosedErrors.isEmpty());
    }

    @Test
    @DisplayName("Handles null and empty strings gracefully")
    void testEmptyAndNull() {
        assertTrue(LatexSyntaxValidator.isValid(null));
        assertTrue(LatexSyntaxValidator.isValid(""));
        assertTrue(LatexSyntaxValidator.isValid("   "));
    }
}
