/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.shared.ai.latex;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance preservation and unmasking utility for LaTeX/KaTeX mathematical expressions,
 * Chemical Formulas (\ce{...}), Physical Units (\pu{...}), SMILES strings, Markdown images, and HTML images
 * during AI operations (e.g. translation, explanation generation).
 *
 * Prevents translation engines (such as IndicTrans2) and generative LLMs from mangling, translating,
 * or corrupting mathematical/scientific syntax.
 */
public final class LatexPreservationUtil {

    private static final String PLACEHOLDER_PREFIX = "__NAG_MATH_";
    private static final String PLACEHOLDER_SUFFIX = "__";

    // Matches standard mathematical and scientific notation in order of specificity
    private static final Pattern PRESERVED_PATTERN = Pattern.compile(
            // 1. Fenced SMILES code blocks: ```smiles ... ```
            "```smiles[\\s\\S]*?```" +
            // 2. Explicit XML-style SMILES tags: <smiles>...</smiles>
            "|<smiles>[\\s\\S]*?<\\/smiles>" +
            // 3. Markdown images: ![alt](url)
            "|!\\[[^\\]]*\\]\\([^)]+\\)" +
            // 4. HTML images: <img ... />
            "|<img\\b[^>]*\\/?>" +
            // 5. Display math: $$...$$
            "|\\$\\$[\\s\\S]*?\\$\\$" +
            // 6. Inline math: $...$ (ensuring not literal escaped \\$)
            "|(?:(?<!\\\\)\\$(?!\\$)(.*?)(?<!\\\\)\\$)" +
            // 7. LaTeX display math: \\[...\\]
            "|\\\\{1,4}\\[[\\s\\S]*?\\\\{1,4}\\]" +
            // 8. LaTeX inline math: \\(...\\)
            "|\\\\{1,4}\\([\\s\\S]*?\\\\{1,4}\\)" +
            // 9. LaTeX Chemistry (\\ce{...}) and Physical Quantities (\\pu{...})
            "|\\\\{1,4}(?:ce|pu)\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}" +
            // 10. Common multiline LaTeX math environments
            "|\\\\{1,4}begin\\{(?:equation|equation\\*|align|align\\*|gather|gather\\*|matrix|pmatrix|bmatrix|vmatrix|Vmatrix|cases)\\}[\\s\\S]*?\\\\{1,4}end\\{(?:equation|equation\\*|align|align\\*|gather|gather\\*|matrix|pmatrix|bmatrix|vmatrix|Vmatrix|cases)\\}" +
            // 11. Standalone LaTeX commands, fractions, Greek letters, and mathematical symbols
            "|\\\\{1,4}(?:frac|sqrt|mathbf|mathbfit|mathrm|text|vec|overline|underline|hat|dot|ddot)\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}(?:\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\})?" +
            "|\\\\{1,4}(?:alpha|beta|gamma|delta|epsilon|varepsilon|zeta|eta|theta|vartheta|iota|kappa|lambda|mu|nu|xi|pi|varpi|rho|varrho|sigma|varsigma|tau|upsilon|phi|varphi|chi|psi|omega|Gamma|Delta|Theta|Lambda|Xi|Pi|Sigma|Upsilon|Phi|Psi|Omega|cdot|times|div|pm|mp|circ|cap|cup|in|notin|subset|subseteq|supset|supseteq|leq|le|geq|ge|neq|ne|approx|sim|equiv|cong|propto|parallel|perp|infty|partial|nabla|int|iint|iiint|oint|sum|prod|coprod|lim|to|rightarrow|leftarrow|Rightarrow|Leftarrow|Leftrightarrow|iff|mapsto|forall|exists|nexists|emptyset|angle|triangle|deg|hbar|ell)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Highly tolerant pattern to match placeholders in translated/generated text across all model modifications:
    // Handles __NAG_MATH_0__, _NAG_MATH_0_, NAG_MATH_0, NAG MATH 0, NAG-MATH-0, NAG MATH0, NAGMATH0,
    // MATH_0, MATH 0, Devanagari transliterations (एन ए जी मैथ ०, मैथ ०), etc.
    private static final Pattern RESTORATION_PATTERN = Pattern.compile(
            "(?:_{1,4}\\s*|«|<|\\[|\\()?" +
            "(?:(?:NAG|एन\\s*ए\\s*जी)[\\s_\\-]*)*" +
            "(?:MATH|मैथ|गणित)[\\s_\\-]*" +
            "([0-9०-९]+)" +
            "(?:\\s*_{1,4}|»|>|\\]|\\))?",
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
     * Restores preserved LaTeX/KaTeX expressions from placeholders into the translated or generated output,
     * and automatically strips any stray underscore delimiters introduced around non-translatable tokens.
     */
    public static String unmask(String translatedText, List<String> preservedTokens) {
        if (translatedText == null || translatedText.isBlank()) {
            return translatedText;
        }
        if (preservedTokens == null || preservedTokens.isEmpty()) {
            return cleanStrayUnderscores(translatedText);
        }

        Matcher matcher = RESTORATION_PATTERN.matcher(translatedText);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            try {
                int tokenIdx = parseIndex(matcher.group(1));
                if (tokenIdx >= 0 && tokenIdx < preservedTokens.size()) {
                    String originalMath = preservedTokens.get(tokenIdx);
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(originalMath));
                } else {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                }
            } catch (Exception e) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);

        return cleanStrayUnderscores(sb.toString());
    }

    /**
     * Cleans up stray underscore delimiters ('_') left by neural machine translation tokenizers
     * around preserved mathematical expressions, chemical equations, physical formulas, numbers,
     * or at the start of translated options.
     *
     * @param text input text possibly containing stray underscore artifacts
     * @return cleaned text with stray underscores around non-translatable elements removed
     */
    public static String cleanStrayUnderscores(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String cleaned = text;

        // 1. Remove stray underscores before non-translatable tokens ($$, $, \[, \(, <smiles>, ![, <img, <svg, <code>)
        // e.g. "_ $$75\text{ minutes}$$" -> "$$75\text{ minutes}$$"
        // e.g. "गति _ $$\frac{5}{6}$$" -> "गति $$\frac{5}{6}$$"
        cleaned = Pattern.compile("(^|\\s)(?:_{1,4}\\s*)+(?=\\$\\$|\\$|\\\\{1,4}\\[|\\\\{1,4}\\(|<smiles>|!\\[|<img|<svg|<code>)")
                .matcher(cleaned)
                .replaceAll("$1");

        // 2. Remove stray underscores before standalone numbers
        // e.g. "_ 15" -> "15" or option starting with "_ 75" -> "75"
        cleaned = Pattern.compile("(^|\\s)(?:_{1,4}\\s+)+(?=[0-9]+(?:\\.[0-9]+)?(?![0-9_a-zA-Z]))")
                .matcher(cleaned)
                .replaceAll("$1");

        // 3. Remove stray leading underscores at the beginning of strings or option text
        // e.g. "^_+\s*" before non-whitespace
        cleaned = Pattern.compile("^\\s*(?:_{1,4}\\s*)+(?=[^\\s_])")
                .matcher(cleaned)
                .replaceAll("");

        // 4. Remove stray underscores after non-translatable tokens
        // e.g. "$$\frac{5}{6}$$ _ पर" -> "$$\frac{5}{6}$$ पर"
        // e.g. "$$15\text{ minutes}$$ _ _ देर" -> "$$15\text{ minutes}$$ देर"
        // e.g. "$$T$$ _ का" -> "$$T$$ का"
        Matcher postTokenMatcher = Pattern.compile("(\\$\\$|\\$|\\\\{1,4}\\]|\\\\{1,4}\\)|<\\/smiles>|<\\/code>)\\s+(?:_{1,4}\\s*)+(?=[\\s.,;:!?\\u0964\\p{L}]|$)")
                .matcher(cleaned);
        StringBuilder sb = new StringBuilder();
        while (postTokenMatcher.find()) {
            postTokenMatcher.appendReplacement(sb, Matcher.quoteReplacement(postTokenMatcher.group(1)) + " ");
        }
        postTokenMatcher.appendTail(sb);
        cleaned = sb.toString();

        // 5. Remove trailing stray underscores at the end of strings or before terminal punctuation
        // e.g. " देर से पहुँचता है _ ." or " ... $$ _"
        cleaned = Pattern.compile("\\s+(?:_{1,4}\\s*)+(?=[.,;:!?\\u0964]|$)")
                .matcher(cleaned)
                .replaceAll("");

        // 6. Normalize multiple consecutive spaces
        cleaned = cleaned.replaceAll("[ \\t]{2,}", " ").trim();

        return cleaned;
    }

    private static int parseIndex(String indexStr) {
        if (indexStr == null || indexStr.isBlank()) {
            return -1;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : indexStr.toCharArray()) {
            if (c >= '0' && c <= '9') {
                sb.append(c);
            } else if (c >= '\u0966' && c <= '\u096f') {
                sb.append((char) ('0' + (c - '\u0966')));
            }
        }
        return Integer.parseInt(sb.toString());
    }
}
