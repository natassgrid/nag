/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 Open Digital Public Infrastructure (DPI) Platform Contributors
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
package com.examplatform.questionbank.microlesson.ai.agent;

import com.examplatform.questionbank.microlesson.dto.MicroLessonContentDto;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic state-machine and regex engine that repairs malformed,
 * unescaped, or truncated LLM JSON responses for {@link MicroLessonContentDto}.
 */
@Component
public class DeterministicJsonRepairEngine {

    private static final Logger log = LoggerFactory.getLogger(DeterministicJsonRepairEngine.class);

    private static final char CH_FF = (char) 0x0C; // form-feed
    private static final char CH_TAB = (char) 0x09; // tab
    private static final char CH_BS = (char) 0x08; // backspace
    private static final char CH_CR = (char) 0x0D; // carriage return
    private static final char CH_LF = (char) 0x0A; // newline

    private static final Pattern CODE_FENCE_PATTERN =
            Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```");

    // StringTemplate leaked brace escapes: \{ and \}
    private static final Pattern STRING_TEMPLATE_OPEN_BRACE =
            Pattern.compile("(?<!\\\\)\\\\\\{");
    private static final Pattern STRING_TEMPLATE_CLOSE_BRACE =
            Pattern.compile("(?<!\\\\)\\\\\\}");

    // Comprehensive list of LaTeX commands (sorted by descending length to prevent prefix collisions)
    private static final String LATEX_COMMAND_NAMES =
            "rightarrow|Rightarrow|leftrightarrow|Leftrightarrow|triangle|vartheta|nexists|"
            + "overline|underline|partial|parallel|subseteq|approx|equiv|propto|middot|"
            + "forall|cdot|times|textbf|textit|textrm|text|mathbf|mathrm|notin|nabla|varrho|"
            + "lambda|Lambda|epsilon|sum|prod|sqrt|infty|subset|quad|qquad|cdots|ldots|"
            + "vdots|ddots|begin|right|left|frac|dfrac|tfrac|theta|Theta|omega|Omega|"
            + "sigma|Sigma|alpha|gamma|Gamma|delta|Delta|tau|beta|neq|neg|rho|phi|"
            + "Phi|psi|Psi|circ|angle|cong|sim|perp|cup|cap|vec|hat|bar|int|oint|lim|"
            + "div|leq|geq|log|ln|exp|det|ker|dim|min|max|sup|inf|gcd|deg|sin|cos|tan|"
            + "ce|pu|pm|mp|le|ge|ne|nu|to|in|bf|it|rm|sf|tt";

    // Pattern matching unescaped single backslash before known LaTeX commands or %
    private static final Pattern SINGLE_BACKSLASH_LATEX_PATTERN = Pattern.compile(
            "(?<!\\\\)\\\\(?=(?:" + LATEX_COMMAND_NAMES + ")(?![a-zA-Z])|%)"
    );

    // Pattern matching single backslash followed by control-collision letters (b, f, n, r, t) and lowercase letters
    private static final Pattern B_F_N_R_T_LATEX_COLLISION = Pattern.compile(
            "(?<!\\\\)\\\\(?=[ftbrn][a-z])"
    );

    // Pattern matching any remaining backslash not followed by a valid JSON escape
    private static final Pattern INVALID_JSON_ESCAPE_BACKSLASH = Pattern.compile(
            "(?<!\\\\)\\\\(?![\"\\\\/bfnrtu]|u[0-9a-fA-F]{4})"
    );

    // Regex fallback field extractors
    private static final Pattern CORE_CONCEPT_PATTERN = Pattern.compile(
            "\"(?:coreConcept|core_concept)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
    );
    private static final Pattern TRAP_PATTERN = Pattern.compile(
            "\"(?:trap|pitfall|common_trap)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
    );
    private static final Pattern CHEAT_SHEET_PATTERN = Pattern.compile(
            "\"(?:cheatSheet|cheat_sheet)\"\\s*:\\s*\\[([\\s\\S]*?)\\]"
    );
    private static final Pattern QUESTION_STEM_PATTERN = Pattern.compile(
            "\"(?:questionStem|question_stem|question)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
    );
    private static final Pattern STEPS_PATTERN = Pattern.compile(
            "\"(?:steps|breakdown_steps)\"\\s*:\\s*\\[([\\s\\S]*?)\\]"
    );
    private static final Pattern FINAL_ANSWER_PATTERN = Pattern.compile(
            "\"(?:finalAnswer|final_answer|answer)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
    );
    private static final Pattern QUOTED_STRING_ITEM = Pattern.compile(
            "\"((?:[^\"\\\\]|\\\\.)*)\""
    );

    /**
     * Strips markdown code fences, leading conversational preambles before the first '{',
     * and trailing commentary after balanced JSON.
     */
    public String stripMarkdownAndPreambles(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return "";
        }

        String text = rawResponse.trim();
        Matcher fenceMatcher = CODE_FENCE_PATTERN.matcher(text);
        if (fenceMatcher.find()) {
            text = fenceMatcher.group(1).trim();
        }

        int firstBrace = text.indexOf('{');
        if (firstBrace == -1) {
            return text;
        }

        // Substring from first '{'
        String candidate = text.substring(firstBrace).trim();

        // Check if root object is balanced
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        int matchingCloseIndex = -1;

        for (int i = 0; i < candidate.length(); i++) {
            char c = candidate.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        matchingCloseIndex = i;
                        break;
                    }
                }
            }
        }

        // If closed cleanly, trim any trailing conversational text
        if (matchingCloseIndex != -1) {
            return candidate.substring(0, matchingCloseIndex + 1).trim();
        }

        // If not balanced (truncated), return the entire candidate from first '{'
        return candidate;
    }

    /**
     * Removes StringTemplate leaked brace escapes (\{ -> { and \} -> }).
     */
    public String cleanStringTemplateBraces(String json) {
        if (json == null) {
            return null;
        }
        String cleaned = STRING_TEMPLATE_OPEN_BRACE.matcher(json).replaceAll("{");
        return STRING_TEMPLATE_CLOSE_BRACE.matcher(cleaned).replaceAll("}");
    }

    /**
     * Fixes the critical LaTeX-vs-JSON control-character collision and properly double-escapes
     * single backslashes in JSON strings.
     */
    public static String sanitizeLatexJson(String json) {
        if (json == null) {
            return null;
        }

        String result = json;

        // Step a: Replace already-corrupted control characters followed by LaTeX command suffixes
        // Form feed (\f)
        result = result.replace(CH_FF + "rac", "\\\\frac")
                       .replace(CH_FF + "orall", "\\\\forall")
                       .replace(String.valueOf(CH_FF), "\\\\f");

        // Tab (\t)
        result = result.replace(CH_TAB + "extbf", "\\\\textbf")
                       .replace(CH_TAB + "extit", "\\\\textit")
                       .replace(CH_TAB + "extrm", "\\\\textrm")
                       .replace(CH_TAB + "ext", "\\\\text")
                       .replace(CH_TAB + "imes", "\\\\times")
                       .replace(CH_TAB + "heta", "\\\\theta")
                       .replace(CH_TAB + "riangle", "\\\\triangle")
                       .replace(CH_TAB + "au", "\\\\tau")
                       .replaceAll(Pattern.quote(String.valueOf(CH_TAB)) + "o(?![a-zA-Z])", "\\\\to");

        // Backspace (\b)
        result = result.replace(CH_BS + "eta", "\\\\beta")
                       .replace(CH_BS + "egin", "\\\\begin")
                       .replace(CH_BS + "ar", "\\\\bar")
                       .replace(CH_BS + "f", "\\\\bf")
                       .replace(String.valueOf(CH_BS), "\\\\b");

        // Carriage return (\r)
        result = result.replace(CH_CR + "ightarrow", "\\\\rightarrow")
                       .replace(CH_CR + "Rightarrow", "\\\\Rightarrow")
                       .replace(CH_CR + "ight", "\\\\right")
                       .replace(CH_CR + "ho", "\\\\rho")
                       .replace("" + CH_CR + CH_LF, "" + CH_LF)
                       .replace(String.valueOf(CH_CR), "");

        // Newline (\n) followed by LaTeX command suffixes
        result = result.replaceAll(Pattern.quote(String.valueOf(CH_LF)) + "(?=eq(?![a-zA-Z]))", Matcher.quoteReplacement("\\\\n"))
                       .replaceAll(Pattern.quote(String.valueOf(CH_LF)) + "(?=eg(?![a-zA-Z]))", Matcher.quoteReplacement("\\\\n"))
                       .replaceAll(Pattern.quote(String.valueOf(CH_LF)) + "(?=nu(?![a-zA-Z]))", Matcher.quoteReplacement("\\\\n"))
                       .replaceAll(Pattern.quote(String.valueOf(CH_LF)) + "(?=ne(?![a-zA-Z]))", Matcher.quoteReplacement("\\\\n"))
                       .replaceAll(Pattern.quote(String.valueOf(CH_LF)) + "(?=abla)", Matcher.quoteReplacement("\\\\n"))
                       .replaceAll(Pattern.quote(String.valueOf(CH_LF)) + "(?=notin)", Matcher.quoteReplacement("\\\\n"))
                       .replaceAll(Pattern.quote(String.valueOf(CH_LF)) + "(?=exists)", Matcher.quoteReplacement("\\\\n"));

        // Step b: Properly double-escape single backslashes before known LaTeX commands & %
        result = SINGLE_BACKSLASH_LATEX_PATTERN.matcher(result).replaceAll("\\\\\\\\");

        // Step c: Double-escape backslashes followed by collision letters (f, t, b, r, n) and lowercase letters
        result = B_F_N_R_T_LATEX_COLLISION.matcher(result).replaceAll("\\\\\\\\");

        // Step d: Double-escape any remaining single backslash not followed by valid JSON escape
        result = INVALID_JSON_ESCAPE_BACKSLASH.matcher(result).replaceAll("\\\\\\\\");

        return result;
    }

    /**
     * Repairs truncated JSON structures by closing open strings, removing dangling commas/colons,
     * and appending missing closing brackets/braces in reverse order.
     */
    public String repairTruncatedJson(String json) {
        if (json == null || json.isBlank()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder(json.trim());
        Deque<Character> stack = new ArrayDeque<>();
        boolean inString = false;
        boolean escape = false;

        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == '{') {
                    stack.push('}');
                } else if (c == '[') {
                    stack.push(']');
                } else if (c == '}') {
                    if (!stack.isEmpty() && stack.peek() == '}') {
                        stack.pop();
                    }
                } else if (c == ']') {
                    if (!stack.isEmpty() && stack.peek() == ']') {
                        stack.pop();
                    }
                }
            }
        }

        // 1. Cleanly close open string
        if (inString) {
            if (escape && sb.length() > 0 && sb.charAt(sb.length() - 1) == '\\') {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append('"');
        }

        // 2. Strip trailing whitespace
        while (sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
            sb.deleteCharAt(sb.length() - 1);
        }

        // 3. Handle trailing colon or comma
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ':') {
            sb.append(" \"\"");
        } else if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
            while (sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
                sb.deleteCharAt(sb.length() - 1);
            }
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ':') {
                sb.append(" \"\"");
            }
        }

        // 4. Append missing closing brackets and braces in reverse stack order
        while (!stack.isEmpty()) {
            sb.append('\n').append(stack.pop());
        }

        return sb.toString();
    }

    /**
     * Fallback heuristic field extractor that extracts key micro-lesson fields
     * even if the JSON structure is severely mangled.
     */
    public MicroLessonContentDto extractFieldsDeterministically(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }

        String coreConcept = extractMatch(CORE_CONCEPT_PATTERN, rawText);
        String trap = extractMatch(TRAP_PATTERN, rawText);
        String questionStem = extractMatch(QUESTION_STEM_PATTERN, rawText);
        String finalAnswer = extractMatch(FINAL_ANSWER_PATTERN, rawText);

        List<String> cheatSheet = extractListMatches(CHEAT_SHEET_PATTERN, rawText);
        List<String> steps = extractListMatches(STEPS_PATTERN, rawText);

        // Check if key fields are present
        if (coreConcept == null && trap == null && questionStem == null) {
            return null;
        }

        if (coreConcept == null || coreConcept.isBlank()) {
            coreConcept = "Core concepts and governing relations of the subtopic.";
        }
        if (trap == null || trap.isBlank()) {
            trap = "Students commonly overlook sign conventions, unit dimensions, and limiting boundary conditions.";
        }
        if (cheatSheet.isEmpty()) {
            cheatSheet = List.of(
                    "$$\\text{Relation 1: Check boundary conditions}$$",
                    "$$\\text{Relation 2: Verify dimensional consistency}$$"
            );
        }

        if (questionStem == null || questionStem.isBlank()) {
            questionStem = "Evaluate the primary governing parameters under baseline boundary conditions.";
        }
        if (steps.isEmpty()) {
            steps = List.of(
                    "Step 1: Identify given boundary values and governing relationships.",
                    "Step 2: Substitute dimensionally consistent parameters and solve."
            );
        }
        if (finalAnswer == null || finalAnswer.isBlank()) {
            finalAnswer = "Standard numerical/algebraic solution";
        }

        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(questionStem, steps, finalAnswer);
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    /**
     * Full repair pipeline: strips markdown, cleans template braces, sanitizes LaTeX control escapes,
     * repairs truncation, parses, and guarantees non-null fields on the returned MicroLessonContentDto.
     */
    public MicroLessonContentDto parseAndRepair(String rawResponse, ObjectMapper aiObjectMapper)
            throws JsonProcessingException {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalArgumentException("Empty response provided to parseAndRepair");
        }

        String step1 = stripMarkdownAndPreambles(rawResponse);
        String step2 = cleanStringTemplateBraces(step1);
        String step3 = sanitizeLatexJson(step2);
        String repairedJson = repairTruncatedJson(step3);

        MicroLessonContentDto content = null;
        try {
            content = aiObjectMapper.readValue(repairedJson, MicroLessonContentDto.class);
        } catch (JsonProcessingException e) {
            log.warn("Direct parse failed on repaired JSON ({}). Attempting fallback field extraction.", e.getMessage());
            content = extractFieldsDeterministically(rawResponse);
            if (content == null) {
                // Try deterministic extraction on step3 as well
                content = extractFieldsDeterministically(step3);
            }
            if (content == null) {
                throw e;
            }
        }

        return enforceNonNullFields(content);
    }

    private MicroLessonContentDto enforceNonNullFields(MicroLessonContentDto content) {
        if (content == null) {
            return null;
        }

        String coreConcept = (content.coreConcept() != null && !content.coreConcept().isBlank())
                ? content.coreConcept()
                : "Core concepts and governing relations of the subtopic.";

        String trap = (content.trap() != null && !content.trap().isBlank())
                ? content.trap()
                : "Students commonly overlook sign conventions, unit dimensions, and limiting boundary conditions.";

        List<String> cheatSheet = content.cheatSheet();
        if (cheatSheet == null || cheatSheet.isEmpty()) {
            cheatSheet = List.of(
                    "$$\\text{Relation 1: Check boundary conditions}$$",
                    "$$\\text{Relation 2: Verify dimensional consistency}$$"
            );
        }

        StepByStepBreakdownDto breakdown = content.stepByStepBreakdown();
        if (breakdown == null) {
            breakdown = new StepByStepBreakdownDto(
                    "Evaluate the primary governing parameters under baseline boundary conditions.",
                    List.of(
                            "Step 1: Identify given boundary values and governing relationships.",
                            "Step 2: Substitute dimensionally consistent parameters and solve."
                    ),
                    "Standard numerical/algebraic solution"
            );
        } else {
            String qStem = (breakdown.questionStem() != null && !breakdown.questionStem().isBlank())
                    ? breakdown.questionStem()
                    : "Evaluate the primary governing parameters under baseline boundary conditions.";

            List<String> steps = breakdown.steps();
            if (steps == null || steps.isEmpty()) {
                steps = List.of(
                        "Step 1: Identify given boundary values and governing relationships.",
                        "Step 2: Substitute dimensionally consistent parameters and solve."
                );
            } else if (steps.size() == 1) {
                List<String> expanded = new ArrayList<>(steps);
                expanded.add("Step 2: Verify dimensional consistency and calculate the final result.");
                steps = expanded;
            }

            String finalAnswer = (breakdown.finalAnswer() != null && !breakdown.finalAnswer().isBlank())
                    ? breakdown.finalAnswer()
                    : "Standard numerical/algebraic solution";

            breakdown = new StepByStepBreakdownDto(qStem, steps, finalAnswer);
        }

        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private String extractMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return unescapeJsonString(matcher.group(1));
        }
        return null;
    }

    private List<String> extractListMatches(Pattern arrayPattern, String text) {
        List<String> results = new ArrayList<>();
        Matcher arrayMatcher = arrayPattern.matcher(text);
        if (arrayMatcher.find()) {
            String arrayContent = arrayMatcher.group(1);
            Matcher itemMatcher = QUOTED_STRING_ITEM.matcher(arrayContent);
            while (itemMatcher.find()) {
                results.add(unescapeJsonString(itemMatcher.group(1)));
            }
        }
        return results;
    }

    private String unescapeJsonString(String str) {
        if (str == null) return null;
        return str.replace("\\\"", "\"")
                  .replace("\\\\", "\\")
                  .replace("\\n", "\n")
                  .replace("\\t", "\t");
    }
}
