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

class LatexPreservationUtilTest {

    @Test
    @DisplayName("Preserves display math ($$...$$) and inline math ($...$)")
    void testPreserveDollarMath() {
        String input = "Calculate the value of $$x^2 + 2x + 1 = 0$$ when $x > 0$.";
        LatexPreservationUtil.MaskResult result = LatexPreservationUtil.mask(input);

        assertEquals("Calculate the value of __NAG_MATH_0__ when __NAG_MATH_1__.", result.maskedText());
        assertEquals(2, result.preservedTokens().size());
        assertEquals("$$x^2 + 2x + 1 = 0$$", result.preservedTokens().get(0));
        assertEquals("$x > 0$", result.preservedTokens().get(1));

        String translatedMock = "जब __NAG_MATH_1__ हो तो __NAG_MATH_0__ का मान ज्ञात कीजिए।";
        String restored = LatexPreservationUtil.unmask(translatedMock, result.preservedTokens());
        assertEquals("जब $x > 0$ हो तो $$x^2 + 2x + 1 = 0$$ का मान ज्ञात कीजिए।", restored);
    }

    @Test
    @DisplayName("Preserves SMILES chemical notation tags (<smiles>...</smiles>)")
    void testPreserveSmilesTags() {
        String input = "Identify the product of the reaction with <smiles>c1ccccc1</smiles> (benzene) and <smiles>CC(=O)O</smiles>.";
        LatexPreservationUtil.MaskResult result = LatexPreservationUtil.mask(input);

        assertEquals("Identify the product of the reaction with __NAG_MATH_0__ (benzene) and __NAG_MATH_1__.", result.maskedText());
        assertEquals(2, result.preservedTokens().size());
        assertEquals("<smiles>c1ccccc1</smiles>", result.preservedTokens().get(0));
        assertEquals("<smiles>CC(=O)O</smiles>", result.preservedTokens().get(1));

        String translatedMock = "__NAG_MATH_0__ (बेंजीन) और __NAG_MATH_1__ के साथ प्रतिक्रिया के उत्पाद की पहचान करें।";
        String restored = LatexPreservationUtil.unmask(translatedMock, result.preservedTokens());
        assertEquals("<smiles>c1ccccc1</smiles> (बेंजीन) और <smiles>CC(=O)O</smiles> के साथ प्रतिक्रिया के उत्पाद की पहचान करें।", restored);
    }

    @Test
    @DisplayName("Handles null and empty string safely")
    void testNullAndEmptyHandling() {
        LatexPreservationUtil.MaskResult nullRes = LatexPreservationUtil.mask(null);
        assertNull(nullRes.maskedText());
        assertTrue(nullRes.preservedTokens().isEmpty());

        LatexPreservationUtil.MaskResult emptyRes = LatexPreservationUtil.mask("   ");
        assertEquals("   ", emptyRes.maskedText());
        assertTrue(emptyRes.preservedTokens().isEmpty());

        assertNull(LatexPreservationUtil.unmask(null, List.of("token")));
        assertEquals("test", LatexPreservationUtil.unmask("test", List.of()));
    }

    @Test
    @DisplayName("Removes stray underscores before and after non-translatable formulas in translated question content (Issue #422)")
    void testCleanStrayUnderscoresInQuestionContent() {
        // Model emitted raw translated text with stray underscores around placeholders:
        String rawTranslated = "अपनी सामान्य गति से _ __NAG_MATH_0__ _ पर चलते हुए, एक आदमी अपने कार्यालय _ __NAG_MATH_1__ _ _ देर से पहुँचता है। कार्यालय पहुँचने का उसका सामान्य समय क्या है?";
        List<String> tokens = List.of("$$\\frac{5}{6}$$", "$$15\\text{ minutes}$$");

        String restored = LatexPreservationUtil.unmask(rawTranslated, tokens);
        assertEquals(
                "अपनी सामान्य गति से $$\\frac{5}{6}$$ पर चलते हुए, एक आदमी अपने कार्यालय $$15\\text{ minutes}$$ देर से पहुँचता है। कार्यालय पहुँचने का उसका सामान्य समय क्या है?",
                restored
        );
    }

    @Test
    @DisplayName("Removes stray underscores in option texts (Issue #422)")
    void testCleanStrayUnderscoresInOptions() {
        assertEquals("$$75\\text{ minutes}$$", LatexPreservationUtil.cleanStrayUnderscores("_ $$75\\text{ minutes}$$"));
        assertEquals("$$90\\text{ minutes}$$", LatexPreservationUtil.cleanStrayUnderscores("_ $$90\\text{ minutes}$$"));
        assertEquals("$$60\\text{ minutes}$$", LatexPreservationUtil.cleanStrayUnderscores("_ $$60\\text{ minutes}$$"));
        assertEquals("$$80\\text{ minutes}$$", LatexPreservationUtil.cleanStrayUnderscores("_ $$80\\text{ minutes}$$"));

        // When unmasking placeholder directly in option
        String optionWithPlaceholder = "_ __NAG_MATH_0__";
        List<String> tokens = List.of("$$75\\text{ minutes}$$");
        assertEquals("$$75\\text{ minutes}$$", LatexPreservationUtil.unmask(optionWithPlaceholder, tokens));
    }

    @Test
    @DisplayName("Removes stray underscores in explanation content (Issue #422)")
    void testCleanStrayUnderscoresInExplanation() {
        String rawExplanation = "यदि गति _ $$\\frac{5}{6}$$ है, तो समय सामान्य समय _ $$T$$ _ का _ $$= \\frac{6}{5}T - T = \\frac{1}{5}T = 15\\text{ min} \\implies T = 75\\text{ minutes}$$ है।";
        String cleaned = LatexPreservationUtil.cleanStrayUnderscores(rawExplanation);

        assertEquals(
                "यदि गति $$\\frac{5}{6}$$ है, तो समय सामान्य समय $$T$$ का $$= \\frac{6}{5}T - T = \\frac{1}{5}T = 15\\text{ min} \\implies T = 75\\text{ minutes}$$ है।",
                cleaned
        );
    }

    @Test
    @DisplayName("Preserves valid snake_case identifiers and LaTeX subscripts inside math delimiters")
    void testPreserveLegitimateUnderscores() {
        String input = "The variable score_card_count is defined as $$x_1 + x_2 + \\alpha_i$$.";
        String cleaned = LatexPreservationUtil.cleanStrayUnderscores(input);
        assertEquals(input, cleaned);
    }
}
