/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */

package com.examplatform.questionbank.translation.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to protect and preserve LaTeX / KaTeX mathematical expressions,
 * chemical equations (\ce{...}), physical units (\pu{...}), SVG graphics,
 * and scientific symbols during machine translation (e.g. IndicTrans2).
 *
 * It extracts all math/symbol structures into numbered placeholders before
 * passing the natural text to the neural translation model, and then restores
 * the exact verbatim mathematical syntax on the translated output.
 */
public final class LatexPreservationUtil {

    private static final String PLACEHOLDER_PREFIX = "__NAG_MATH_";
    private static final String PLACEHOLDER_SUFFIX = "__";

    // Combined pattern matching all LaTeX, KaTeX, mhchem, and code blocks in order of precedence:
    // 1. Markdown code blocks ```...```
    // 2. SVG tags <svg>...</svg>
    // 3. HTML code tags <code>...</code>
    // 4. Display Math $$...$$
    // 5. LaTeX Display Brackets \[...\] (accepting 1-4 backslashes)
    // 6. LaTeX Environments \begin{matrix|pmatrix|...}...\end{...}
    // 7. LaTeX Inline Brackets \(...\)
    // 8. Inline Dollar Math $...$ (not preceded or followed by $)
    // 9. LaTeX Chemistry / Physics macros \ce{...}, \pu{...}
    // 10. LaTeX Structural Commands \frac{...}{...}, \sqrt{...}, etc.
    // 11. Standalone LaTeX Greek / Math Symbols \alpha, \beta, \theta, \int, \infty, etc.
    private static final Pattern PRESERVED_PATTERN = Pattern.compile(
            "```[\\s\\S]*?```" +
            "|<svg[\\s\\S]*?</svg>" +
            "|<code>[\\s\\S]*?</code>" +
            "|\\$\\$[\\s\\S]*?\\$\\$" +
            "|\\\\{1,4}\\[[\\s\\S]*?\\\\{1,4}\\]" +
            "|\\\\{1,4}begin\\{(matrix|pmatrix|bmatrix|vmatrix|Vmatrix|cases|align|align\\*|aligned|equation|equation\\*|gather|gather\\*|split|array|subequations)\\}[\\s\\S]*?\\\\{1,4}end\\{\\1\\}" +
            "|\\\\{1,4}\\([\\s\\S]*?\\\\{1,4}\\)" +
            "|(?<!\\\\)\\$(?!\\$)(?:[^$\\n\\r]+?)(?<!\\\\)\\$" +
            "|\\\\{1,4}(?:ce|pu)\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}" +
            "|\\\\{1,4}(?:frac|sqrt|mathbf|mathbfit|mathrm|text|vec|overline|underline|hat|dot|ddot)\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}(?:\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\})?" +
            "|\\\\{1,4}(?:alpha|beta|gamma|delta|epsilon|varepsilon|zeta|eta|theta|vartheta|iota|kappa|lambda|mu|nu|xi|pi|varpi|rho|varrho|sigma|varsigma|tau|upsilon|phi|varphi|chi|psi|omega|Gamma|Delta|Theta|Lambda|Xi|Pi|Sigma|Upsilon|Phi|Psi|Omega|cdot|times|div|pm|mp|circ|cap|cup|in|notin|subset|subseteq|supset|supseteq|leq|le|geq|ge|neq|ne|approx|sim|equiv|cong|propto|parallel|perp|infty|partial|nabla|int|iint|iiint|oint|sum|prod|coprod|lim|to|rightarrow|leftarrow|Rightarrow|Leftarrow|Leftrightarrow|iff|mapsto|forall|exists|nexists|emptyset|angle|triangle|deg|hbar|ell)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to match placeholders in translated text with tolerance for spacing or case variations
    private static final Pattern RESTORATION_PATTERN = Pattern.compile(
            "__\\s*NAG_MATH_(\\d+)\\s*__",
            Pattern.CASE_INSENSITIVE
    );

    private LatexPreservationUtil() {
        // Utility class
    }

    public record MaskResult(String maskedText, List<String> preservedTokens) {}

    /**
     * Extracts LaTeX/KaTeX expressions and replaces them with opaque placeholders.
     */
    public static MaskResult mask(String text) {
        if (text == null || text.isBlank()) {
            return new MaskResult(text, List.of());
        }

        List<String> tokens = new ArrayList<>();
        Matcher matcher = PRESERVED_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String token = matcher.group();
            int index = tokens.size();
            tokens.add(token);
            String placeholder = PLACEHOLDER_PREFIX + index + PLACEHOLDER_SUFFIX;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(placeholder));
        }
        matcher.appendTail(sb);

        return new MaskResult(sb.toString(), tokens);
    }

    /**
     * Restores preserved LaTeX/KaTeX expressions from placeholders into the translated output.
     */
    public static String unmask(String translatedText, List<String> preservedTokens) {
        if (translatedText == null || translatedText.isBlank() || preservedTokens == null || preservedTokens.isEmpty()) {
            return translatedText;
        }

        Matcher matcher = RESTORATION_PATTERN.matcher(translatedText);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            try {
                int tokenIdx = Integer.parseInt(matcher.group(1));
                if (tokenIdx >= 0 && tokenIdx < preservedTokens.size()) {
                    String originalMath = preservedTokens.get(tokenIdx);
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(originalMath));
                } else {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                }
            } catch (NumberFormatException e) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}
