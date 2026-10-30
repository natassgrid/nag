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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic state-machine and regex engine that repairs malformed,
 * unescaped, or truncated LLM JSON responses for {@link MicroLessonContentDto}.
 */
@Component
public class DeterministicJsonRepairEngine {

    private static final Logger log = LoggerFactory.getLogger(DeterministicJsonRepairEngine.class);

    private static final String DEFAULT_CORE_CONCEPT =
            "Core concepts and governing relations of the subtopic.";
    private static final String DEFAULT_TRAP =
            "Students commonly overlook sign conventions, unit dimensions, and limiting boundary conditions.";
    private static final String DEFAULT_QUESTION_STEM =
            "Evaluate the primary governing parameters under baseline boundary conditions.";
    private static final String DEFAULT_STEP_1 =
            "Step 1: Identify given boundary values and governing relationships.";
    private static final String DEFAULT_STEP_2 =
            "Step 2: Substitute dimensionally consistent parameters and solve.";
    private static final String DEFAULT_FINAL_ANSWER =
            "Standard numerical/algebraic solution";
    private static final String DOUBLE_BACKSLASH_REPLACEMENT =
            "\\\\\\\\";

    private static final List<String> DEFAULT_STEPS = List.of(
            DEFAULT_STEP_1,
            DEFAULT_STEP_2
    );

    private static final List<String> DEFAULT_CHEAT_SHEET = List.of(
            "$$\\text{Relation 1: Check boundary conditions}$$",
            "$$\\text{Relation 2: Verify dimensional consistency}$$"
    );

    private static final char CH_FF = (char) 0x0C; // form-feed
    private static final char CH_TAB = (char) 0x09; // tab
    private static final char CH_BS = (char) 0x08; // backspace
    private static final char CH_CR = (char) 0x0D; // carriage return
    private static final char CH_LF = (char) 0x0A; // newline

    private static final Pattern CODE_FENCE_PATTERN =
            Pattern.compile("```(?:json)?\\s*+([\\s\\S]*?)```");

    // StringTemplate leaked brace escapes
    private static final Pattern STRING_TEMPLATE_OPEN_BRACE =
            Pattern.compile("(?<!\\\\)\\\\\\{");
    private static final Pattern STRING_TEMPLATE_CLOSE_BRACE =
            Pattern.compile("(?<!\\\\)\\\\}");

    // Known LaTeX commands for single-backslash double escaping
    private static final Set<String> LATEX_COMMANDS = Set.of(
            "rightarrow", "Rightarrow", "leftrightarrow", "Leftrightarrow", "triangle", "vartheta", "nexists",
            "overline", "underline", "partial", "parallel", "subseteq", "approx", "equiv", "propto", "middot",
            "forall", "cdot", "times", "textbf", "textit", "textrm", "text", "mathbf", "mathrm", "notin", "nabla", "varrho",
            "lambda", "Lambda", "epsilon", "sum", "prod", "sqrt", "infty", "subset", "quad", "qquad", "cdots", "ldots",
            "vdots", "ddots", "begin", "right", "left", "frac", "dfrac", "tfrac", "theta", "Theta", "omega", "Omega",
            "sigma", "Sigma", "alpha", "gamma", "Gamma", "delta", "Delta", "tau", "beta", "neq", "neg", "rho", "phi",
            "Phi", "psi", "Psi", "circ", "angle", "cong", "sim", "perp", "cup", "cap", "vec", "hat", "bar", "int", "oint", "lim",
            "div", "leq", "geq", "log", "ln", "exp", "det", "ker", "dim", "min", "max", "sup", "inf", "gcd", "deg", "sin", "cos", "tan",
            "ce", "pu", "pm", "mp", "le", "ge", "ne", "nu", "to", "in", "bf", "it", "rm", "sf", "tt"
    );

    // Simplified pattern matching single backslash before candidate command name or percent sign
    private static final Pattern SINGLE_BACKSLASH_CANDIDATE = Pattern.compile(
            "(?<!\\\\)\\\\([a-zA-Z]+|%)"
    );

    // Pattern matching single backslash followed by control-collision letters (b, f, n, r, t) and lowercase letters
    private static final Pattern B_F_N_R_T_LATEX_COLLISION = Pattern.compile(
            "(?<!\\\\)\\\\(?=[ftbrn][a-z])"
    );

    // Pattern matching any remaining backslash not followed by a valid JSON escape
    private static final Pattern INVALID_JSON_ESCAPE_BACKSLASH = Pattern.compile(
            "(?<!\\\\)\\\\(?![\"\\\\/bfnrtu]|u[0-9a-fA-F]{4})"
    );

    // Regex fallback field extractors with possessive unrolled loops to prevent stack overflow
    private static final Pattern CORE_CONCEPT_PATTERN = Pattern.compile(
            "\"(?:coreConcept|core_concept)\"\\s*:\\s*\"([^\"\\\\]*+(?:\\\\.[^\"\\\\]*+)*+)\""
    );
    private static final Pattern TRAP_PATTERN = Pattern.compile(
            "\"(?:trap|pitfall|common_trap)\"\\s*:\\s*\"([^\"\\\\]*+(?:\\\\.[^\"\\\\]*+)*+)\""
    );
    private static final Pattern CHEAT_SHEET_PATTERN = Pattern.compile(
            "\"(?:cheatSheet|cheat_sheet)\"\\s*:\\s*\\[([\\s\\S]*?)]"
    );
    private static final Pattern QUESTION_STEM_PATTERN = Pattern.compile(
            "\"(?:questionStem|question_stem|question)\"\\s*:\\s*\"([^\"\\\\]*+(?:\\\\.[^\"\\\\]*+)*+)\""
    );
    private static final Pattern STEPS_PATTERN = Pattern.compile(
            "\"(?:steps|breakdown_steps)\"\\s*:\\s*\\[([\\s\\S]*?)]"
    );
    private static final Pattern FINAL_ANSWER_PATTERN = Pattern.compile(
            "\"(?:finalAnswer|final_answer|answer)\"\\s*:\\s*\"([^\"\\\\]*+(?:\\\\.[^\"\\\\]*+)*+)\""
    );
    private static final Pattern QUOTED_STRING_ITEM = Pattern.compile(
            "\"([^\"\\\\]*+(?:\\\\.[^\"\\\\]*+)*+)\""
    );

    /**
     * Strips markdown code fences, leading conversational preambles before the first opening brace,
     * and trailing commentary after balanced JSON.
     */
    public String stripMarkdownAndPreambles(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return "";
        }

        String text = extractFencedContent(rawResponse.trim());
        int firstBrace = text.indexOf('{');
        if (firstBrace == -1) {
            return text;
        }

        String candidate = text.substring(firstBrace).trim();
        int matchingCloseIndex = findMatchingCloseBraceIndex(candidate);
        if (matchingCloseIndex != -1) {
            return candidate.substring(0, matchingCloseIndex + 1).trim();
        }

        return candidate;
    }

    private String extractFencedContent(String text) {
        Matcher fenceMatcher = CODE_FENCE_PATTERN.matcher(text);
        if (fenceMatcher.find()) {
            return fenceMatcher.group(1).trim();
        }
        return text;
    }

    private int findMatchingCloseBraceIndex(String candidate) {
        int depth = 0;
        boolean inString = false;
        boolean escape = false;

        for (int i = 0; i < candidate.length(); i++) {
            char c = candidate.charAt(i);
            if (escape) {
                escape = false;
            } else if (c == '\\' && inString) {
                escape = true;
            } else if (c == '"') {
                inString = !inString;
            } else if (!inString && c == '{') {
                depth++;
            } else if (!inString && c == '}' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Removes StringTemplate leaked brace escapes.
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
        result = result.replace(CH_FF + "rac", "\\\\frac")
                       .replace(CH_FF + "orall", "\\\\forall")
                       .replace(String.valueOf(CH_FF), "\\\\f");

        result = result.replace(CH_TAB + "extbf", "\\\\textbf")
                       .replace(CH_TAB + "extit", "\\\\textit")
                       .replace(CH_TAB + "extrm", "\\\\textrm")
                       .replace(CH_TAB + "ext", "\\\\text")
                       .replace(CH_TAB + "imes", "\\\\times")
                       .replace(CH_TAB + "heta", "\\\\theta")
                       .replace(CH_TAB + "riangle", "\\\\triangle")
                       .replace(CH_TAB + "au", "\\\\tau")
                       .replaceAll(Pattern.quote(String.valueOf(CH_TAB)) + "o(?![a-zA-Z])", "\\\\to");

        result = result.replace(CH_BS + "eta", "\\\\beta")
                       .replace(CH_BS + "egin", "\\\\begin")
                       .replace(CH_BS + "ar", "\\\\bar")
                       .replace(CH_BS + "f", "\\\\bf")
                       .replace(String.valueOf(CH_BS), "\\\\b");

        result = result.replace(CH_CR + "ightarrow", "\\\\rightarrow")
                       .replace(CH_CR + "Rightarrow", "\\\\Rightarrow")
                       .replace(CH_CR + "ight", "\\\\right")
                       .replace(CH_CR + "ho", "\\\\rho")
                       .replace("" + CH_CR + CH_LF, "" + CH_LF)
                       .replace(String.valueOf(CH_CR), "");

        result = result.replaceAll(Pattern.quote(String.valueOf(CH_LF)) + "(?=eq(?![a-zA-Z]))", Matcher.quoteReplacement("\\\\n"))
                       .replaceAll(Pattern.quote(String.valueOf(CH_LF)) + "(?=eg(?![a-zA-Z]))", Matcher.quoteReplacement("\\\\n"))
                       .replaceAll(Pattern.quote(String.valueOf(CH_LF)) + "(?=nu(?![a-zA-Z]))", Matcher.quoteReplacement("\\\\n"))
                       .replaceAll(Pattern.quote(String.valueOf(CH_LF)) + "(?=ne(?![a-zA-Z]))", Matcher.quoteReplacement("\\\\n"))
                       .replaceAll(Pattern.quote(String.valueOf(CH_LF)) + "(?=abla)", Matcher.quoteReplacement("\\\\n"))
                       .replaceAll(Pattern.quote(String.valueOf(CH_LF)) + "(?=notin)", Matcher.quoteReplacement("\\\\n"))
                       .replaceAll(Pattern.quote(String.valueOf(CH_LF)) + "(?=exists)", Matcher.quoteReplacement("\\\\n"));

        // Step b: Properly double-escape single backslashes before known LaTeX commands & %
        result = SINGLE_BACKSLASH_CANDIDATE.matcher(result).replaceAll(matchResult -> {
            String token = matchResult.group(1);
            if ("%".equals(token) || LATEX_COMMANDS.contains(token)) {
                return Matcher.quoteReplacement("\\\\" + token);
            }
            return Matcher.quoteReplacement(matchResult.group());
        });

        // Step c: Double-escape backslashes followed by collision letters (f, t, b, r, n) and lowercase letters
        result = B_F_N_R_T_LATEX_COLLISION.matcher(result).replaceAll(DOUBLE_BACKSLASH_REPLACEMENT);

        // Step d: Double-escape any remaining single backslash not followed by valid JSON escape
        result = INVALID_JSON_ESCAPE_BACKSLASH.matcher(result).replaceAll(DOUBLE_BACKSLASH_REPLACEMENT);

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
        Deque<Character> stack = scanClosingBrackets(sb);

        closeOpenStringAndTrim(sb, stack);
        handleTrailingDelimiters(sb);
        appendMissingBrackets(sb, stack);

        return sb.toString();
    }

    private Deque<Character> scanClosingBrackets(StringBuilder sb) {
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
                inString = processJsonChar(c, stack);
            }
        }
        if (inString) {
            stack.push('"');
        }
        return stack;
    }

    private boolean processJsonChar(char c, Deque<Character> stack) {
        if (c == '"') {
            return true;
        }
        if (c == '{') {
            stack.push('}');
        } else if (c == '[') {
            stack.push(']');
        } else if (c == '}' && !stack.isEmpty() && stack.peek() == '}') {
            stack.pop();
        } else if (c == ']' && !stack.isEmpty() && stack.peek() == ']') {
            stack.pop();
        }
        return false;
    }

    private void closeOpenStringAndTrim(StringBuilder sb, Deque<Character> stack) {
        if (!stack.isEmpty() && stack.peek() == '"') {
            stack.pop();
            if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '\\') {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append('"');
        }
        trimTrailingWhitespace(sb);
    }

    private void trimTrailingWhitespace(StringBuilder sb) {
        while (!sb.isEmpty() && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
            sb.deleteCharAt(sb.length() - 1);
        }
    }

    private void handleTrailingDelimiters(StringBuilder sb) {
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == ':') {
            sb.append(" \"\"");
        } else if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
            trimTrailingWhitespace(sb);
            if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == ':') {
                sb.append(" \"\"");
            }
        }
    }

    private void appendMissingBrackets(StringBuilder sb, Deque<Character> stack) {
        while (!stack.isEmpty()) {
            sb.append('\n').append(stack.pop());
        }
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

        if (coreConcept == null && trap == null && questionStem == null) {
            return null;
        }

        if (coreConcept == null || coreConcept.isBlank()) {
            coreConcept = DEFAULT_CORE_CONCEPT;
        }
        if (trap == null || trap.isBlank()) {
            trap = DEFAULT_TRAP;
        }
        if (cheatSheet.isEmpty()) {
            cheatSheet = DEFAULT_CHEAT_SHEET;
        }

        if (questionStem == null || questionStem.isBlank()) {
            questionStem = DEFAULT_QUESTION_STEM;
        }
        if (steps.isEmpty()) {
            steps = DEFAULT_STEPS;
        }
        if (finalAnswer == null || finalAnswer.isBlank()) {
            finalAnswer = DEFAULT_FINAL_ANSWER;
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

        MicroLessonContentDto content;
        try {
            content = aiObjectMapper.readValue(repairedJson, MicroLessonContentDto.class);
        } catch (JsonProcessingException e) {
            log.warn("Direct parse failed on repaired JSON ({}). Attempting fallback field extraction.", e.getMessage());
            content = extractFieldsDeterministically(rawResponse);
            if (content == null) {
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
                : DEFAULT_CORE_CONCEPT;

        String trap = (content.trap() != null && !content.trap().isBlank())
                ? content.trap()
                : DEFAULT_TRAP;

        List<String> cheatSheet = (content.cheatSheet() != null && !content.cheatSheet().isEmpty())
                ? content.cheatSheet()
                : DEFAULT_CHEAT_SHEET;

        StepByStepBreakdownDto breakdown = enforceNonNullBreakdown(content.stepByStepBreakdown());

        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private StepByStepBreakdownDto enforceNonNullBreakdown(StepByStepBreakdownDto breakdown) {
        if (breakdown == null) {
            return new StepByStepBreakdownDto(DEFAULT_QUESTION_STEM, DEFAULT_STEPS, DEFAULT_FINAL_ANSWER);
        }

        String qStem = (breakdown.questionStem() != null && !breakdown.questionStem().isBlank())
                ? breakdown.questionStem()
                : DEFAULT_QUESTION_STEM;

        List<String> steps = resolveBreakdownSteps(breakdown.steps());

        String finalAnswer = (breakdown.finalAnswer() != null && !breakdown.finalAnswer().isBlank())
                ? breakdown.finalAnswer()
                : DEFAULT_FINAL_ANSWER;

        return new StepByStepBreakdownDto(qStem, steps, finalAnswer);
    }

    private List<String> resolveBreakdownSteps(List<String> steps) {
        if (steps == null || steps.isEmpty()) {
            return DEFAULT_STEPS;
        }
        if (steps.size() == 1) {
            List<String> expanded = new ArrayList<>(steps);
            expanded.add("Step 2: Verify dimensional consistency and calculate the final result.");
            return expanded;
        }
        return steps;
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
