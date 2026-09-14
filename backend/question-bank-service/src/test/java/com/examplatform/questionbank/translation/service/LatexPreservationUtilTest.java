/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.questionbank.translation.service;

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
    @DisplayName("Preserves Markdown image syntax (![alt](url))")
    void testPreserveMarkdownImage() {
        String input = "Refer to the diagram ![Circuit Diagram](https://cdn.examplatform.org/diagrams/q123.svg) to find the equivalent resistance.";
        LatexPreservationUtil.MaskResult result = LatexPreservationUtil.mask(input);

        assertEquals("Refer to the diagram __NAG_MATH_0__ to find the equivalent resistance.", result.maskedText());
        assertEquals(1, result.preservedTokens().size());
        assertEquals("![Circuit Diagram](https://cdn.examplatform.org/diagrams/q123.svg)", result.preservedTokens().get(0));

        String translatedMock = "समतुल्य प्रतिरोध ज्ञात करने के लिए आरेख __NAG_MATH_0__ देखें।";
        String restored = LatexPreservationUtil.unmask(translatedMock, result.preservedTokens());
        assertEquals("समतुल्य प्रतिरोध ज्ञात करने के लिए आरेख ![Circuit Diagram](https://cdn.examplatform.org/diagrams/q123.svg) देखें।", restored);
    }

    @Test
    @DisplayName("Preserves HTML img tag syntax (<img ... />)")
    void testPreserveHtmlImgTag() {
        String input = "Observe the apparatus <img src=\"https://cdn.examplatform.org/img.png\" alt=\"titration\" /> carefully.";
        LatexPreservationUtil.MaskResult result = LatexPreservationUtil.mask(input);

        assertEquals("Observe the apparatus __NAG_MATH_0__ carefully.", result.maskedText());
        assertEquals(1, result.preservedTokens().size());
        assertEquals("<img src=\"https://cdn.examplatform.org/img.png\" alt=\"titration\" />", result.preservedTokens().get(0));

        String translatedMock = "उपकरण __NAG_MATH_0__ को ध्यान से देखें।";
        String restored = LatexPreservationUtil.unmask(translatedMock, result.preservedTokens());
        assertEquals("उपकरण <img src=\"https://cdn.examplatform.org/img.png\" alt=\"titration\" /> को ध्यान से देखें।", restored);
    }

    @Test
    @DisplayName("Preserves mixed LaTeX formulas and Markdown images")
    void testPreserveMixedLatexAndMarkdownImages() {
        String input = "Given ![Circuit](https://cdn.examplatform.org/c1.png), calculate $I = \\frac{V}{R}$ where $V = 10\\text{V}$.";
        LatexPreservationUtil.MaskResult result = LatexPreservationUtil.mask(input);

        assertTrue(result.preservedTokens().contains("![Circuit](https://cdn.examplatform.org/c1.png)"));
        assertTrue(result.preservedTokens().contains("$I = \\frac{V}{R}$"));
        assertTrue(result.preservedTokens().contains("$V = 10\\text{V}$"));

        String restored = LatexPreservationUtil.unmask(result.maskedText(), result.preservedTokens());
        assertEquals(input, restored);
    }

    @Test
    @DisplayName("Preserves LaTeX display \\[...\\] and inline \\(...\\) brackets")
    void testPreserveLatexBrackets() {
        String input = "Given \\[ E = mc^2 \\] where \\( c \\) is the speed of light.";
        LatexPreservationUtil.MaskResult result = LatexPreservationUtil.mask(input);

        assertEquals("Given __NAG_MATH_0__ where __NAG_MATH_1__ is the speed of light.", result.maskedText());
        assertEquals("\\[ E = mc^2 \\]", result.preservedTokens().get(0));
        assertEquals("\\( c \\)", result.preservedTokens().get(1));

        String translatedMock = "दिया गया है __NAG_MATH_0__ जहाँ __NAG_MATH_1__ प्रकाश की गति है।";
        String restored = LatexPreservationUtil.unmask(translatedMock, result.preservedTokens());
        assertEquals("दिया गया है \\[ E = mc^2 \\] जहाँ \\( c \\) प्रकाश की गति है।", restored);
    }

    @Test
    @DisplayName("Preserves LaTeX matrix and multiline environments")
    void testPreserveLatexEnvironments() {
        String input = "Find the inverse of the matrix \\begin{pmatrix} 1 & 2 \\\\ 3 & 4 \\end{pmatrix}.";
        LatexPreservationUtil.MaskResult result = LatexPreservationUtil.mask(input);

        assertEquals("Find the inverse of the matrix __NAG_MATH_0__.", result.maskedText());
        assertEquals("\\begin{pmatrix} 1 & 2 \\\\ 3 & 4 \\end{pmatrix}", result.preservedTokens().get(0));

        String translatedMock = "मैट्रिक्स __NAG_MATH_0__ का व्युत्क्रम ज्ञात कीजिए।";
        String restored = LatexPreservationUtil.unmask(translatedMock, result.preservedTokens());
        assertEquals("मैट्रिक्स \\begin{pmatrix} 1 & 2 \\\\ 3 & 4 \\end{pmatrix} का व्युत्क्रम ज्ञात कीजिए।", restored);
    }

    @Test
    @DisplayName("Preserves Chemistry mhchem (\\ce{...}) and Physical Units (\\pu{...})")
    void testPreserveChemicalAndPhysicalFormulas() {
        String input = "Consider the reaction \\ce{2H2 + O2 -> 2H2O} at pressure \\pu{1 atm}.";
        LatexPreservationUtil.MaskResult result = LatexPreservationUtil.mask(input);

        assertEquals("Consider the reaction __NAG_MATH_0__ at pressure __NAG_MATH_1__.", result.maskedText());
        assertEquals("\\ce{2H2 + O2 -> 2H2O}", result.preservedTokens().get(0));
        assertEquals("\\pu{1 atm}", result.preservedTokens().get(1));

        String translatedMock = "दबाव __NAG_MATH_1__ पर प्रतिक्रिया __NAG_MATH_0__ पर विचार करें।";
        String restored = LatexPreservationUtil.unmask(translatedMock, result.preservedTokens());
        assertEquals("दबाव \\pu{1 atm} पर प्रतिक्रिया \\ce{2H2 + O2 -> 2H2O} पर विचार करें।", restored);
    }

    @Test
    @DisplayName("Preserves Greek symbols and standalone mathematical operators")
    void testPreserveGreekAndMathSymbols() {
        String input = "Let \\theta be an angle such that \\sin(\\theta) = \\frac{1}{2} and \\Delta \\geq 0.";
        LatexPreservationUtil.MaskResult result = LatexPreservationUtil.mask(input);

        assertTrue(result.maskedText().contains("__NAG_MATH_"));
        assertTrue(result.preservedTokens().contains("\\theta"));
        assertTrue(result.preservedTokens().contains("\\frac{1}{2}"));
        assertTrue(result.preservedTokens().contains("\\Delta"));
        assertTrue(result.preservedTokens().contains("\\geq"));

        String restored = LatexPreservationUtil.unmask(result.maskedText(), result.preservedTokens());
        assertEquals(input, restored);
    }

    @Test
    @DisplayName("Handles IndicTrans2 spaces removal (NAG MATH 0) in translated explanation")
    void testIndicTrans2SpaceStrippedPlaceholderRestoration() {
        List<String> tokens = List.of(
                "\\(A \\subseteq B\\)",
                "\\(B \\subseteq C\\)",
                "\\(A \\subseteq C\\)",
                "\\(x \\in A\\)",
                "\\(x \\in C\\)",
                "\\(x \\notin B\\)"
        );

        String translatedFromIndicTrans2 = "(1), NAG MATH 0 (विच्छेद विभाजन) से। (2) और (3) से, NAG MATH 1 से। " +
                "NAG MATH 2 से, NAG MATH 3 (निष्कर्ष मैं अनुसरण करता हूँ)। (4) के लिए, NAG MATH 4 । (2), NAG MATH 5 (निष्कर्ष II निम्नलिखित है) के विपरीत।";

        String restored = LatexPreservationUtil.unmask(translatedFromIndicTrans2, tokens);

        assertFalse(restored.contains("NAG MATH"));
        assertTrue(restored.contains("\\(A \\subseteq B\\)"));
        assertTrue(restored.contains("\\(B \\subseteq C\\)"));
        assertTrue(restored.contains("\\(A \\subseteq C\\)"));
        assertTrue(restored.contains("\\(x \\in A\\)"));
        assertTrue(restored.contains("\\(x \\in C\\)"));
        assertTrue(restored.contains("\\(x \\notin B\\)"));
    }

    @Test
    @DisplayName("Handles Devanagari numerals in translated placeholders")
    void testDevanagariNumeralsInPlaceholder() {
        List<String> tokens = List.of("$$x = 5$$", "$$y = 10$$");
        String translatedWithDevanagari = "यहाँ NAG MATH ० और NAG MATH १ दिया गया है।";

        String restored = LatexPreservationUtil.unmask(translatedWithDevanagari, tokens);
        assertEquals("यहाँ $$x = 5$$ और $$y = 10$$ दिया गया है।", restored);
    }

    @Test
    @DisplayName("Handles whitespace tolerance in translated placeholders")
    void testPlaceholderToleranceWithWhitespace() {
        List<String> tokens = List.of("$$E = h\\nu$$", "$c = 3 \\times 10^8$");
        String translatedWithSpaces = "यहाँ __ NAG_MATH_0 __ और __nag_math_1__ दिया गया है।";

        String restored = LatexPreservationUtil.unmask(translatedWithSpaces, tokens);
        assertEquals("यहाँ $$E = h\\nu$$ और $c = 3 \\times 10^8$ दिया गया है।", restored);
    }

    @Test
    @DisplayName("Handles null and empty string inputs gracefully")
    void testNullAndEmptyHandling() {
        LatexPreservationUtil.MaskResult nullRes = LatexPreservationUtil.mask(null);
        assertNull(nullRes.maskedText());
        assertTrue(nullRes.preservedTokens().isEmpty());

        LatexPreservationUtil.MaskResult emptyRes = LatexPreservationUtil.mask("   ");
        assertEquals("   ", emptyRes.maskedText());

        assertNull(LatexPreservationUtil.unmask(null, List.of("token")));
        assertEquals("test", LatexPreservationUtil.unmask("test", List.of()));
    }
}
