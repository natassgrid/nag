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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministically repairs and refines a {@link MicroLessonContentDto} to guarantee
 * 100% KaTeX and structural compliance:
 * <ul>
 *   <li>Converts all \( ... \) and \[ ... \] to $$ ... $$.</li>
 *   <li>Converts isolated single-dollar inline math $ ... $ to $$ ... $$.</li>
 *   <li>Repairs unescaped % inside $$ ... $$ blocks to \%.</li>
 *   <li>Balances unclosed {, }, [, ], or $$ delimiters in all content fields.</li>
 *   <li>Wraps cheatSheet formulas containing math notation in $$ ... $$.</li>
 *   <li>Ensures steps are cleanly and sequentially prefixed with "Step 1:", "Step 2:", etc.</li>
 *   <li>Ensures minimum high-yield pedagogical depth (>= 2 cheat sheet items, >= 2 steps).</li>
 * </ul>
 */
@Component
public class MicroLessonRefinementAgent {

    private static final Logger log = LoggerFactory.getLogger(MicroLessonRefinementAgent.class);

    private static final Pattern PAREN_DELIMITER_PATTERN =
            Pattern.compile("\\\\\\((.*?)\\\\\\)", Pattern.DOTALL);

    private static final Pattern BRACKET_DELIMITER_PATTERN =
            Pattern.compile("\\\\\\[(.*?)\\\\\\]", Pattern.DOTALL);

    private static final Pattern SINGLE_DOLLAR_PATTERN =
            Pattern.compile("(?<![\\\\$])\\$(?!\\$)(.*?)(?<![\\\\$])\\$(?!\\$)", Pattern.DOTALL);

    private static final Pattern DOUBLE_DOLLAR_PATTERN =
            Pattern.compile("\\$\\$(.*?)\\$\\$", Pattern.DOTALL);

    private static final Pattern UNESCAPED_PERCENT_IN_LATEX =
            Pattern.compile("(?<!\\\\)%");

    private static final Pattern STEP_PREFIX_PATTERN =
            Pattern.compile("^(?:Step\\s*\\d+\\s*[:.-]?\\s*|\\d+\\s*[:.-]\\s*)(.*)$", Pattern.CASE_INSENSITIVE);

    public MicroLessonContentDto refine(MicroLessonContentDto content) {
        return refine(content, null, null);
    }

    public MicroLessonContentDto refine(MicroLessonContentDto content, MicroLessonCriticResult criticResult) {
        return refine(content, criticResult, null);
    }

    public MicroLessonContentDto refine(
            MicroLessonContentDto content,
            MicroLessonCriticResult criticResult,
            MicroLessonContentDto domainFallback) {
        if (content == null) {
            return domainFallback;
        }

        if (criticResult != null && criticResult.isApproved()) {
            return content;
        }

        boolean hasRubric3Issue = criticResult != null && criticResult.issues() != null
                && criticResult.issues().stream().anyMatch(issue -> issue != null && issue.startsWith("Rubric 3:"));

        if (criticResult != null) {
            log.info("Refining micro-lesson content based on critic feedback (issues: {})", criticResult.issues());
        }

        String refinedCoreConcept = refineText(content.coreConcept());
        if (refinedCoreConcept == null || refinedCoreConcept.isBlank()
                || (hasRubric3Issue && domainFallback != null && isContaminatedText(refinedCoreConcept))) {
            refinedCoreConcept = (domainFallback != null && domainFallback.coreConcept() != null)
                    ? domainFallback.coreConcept()
                    : "Core concepts and governing relations of the subtopic.";
        }

        String refinedTrap = refineText(content.trap());
        if (refinedTrap == null || refinedTrap.isBlank()
                || (hasRubric3Issue && domainFallback != null && isContaminatedText(refinedTrap))) {
            refinedTrap = (domainFallback != null && domainFallback.trap() != null)
                    ? domainFallback.trap()
                    : "Students commonly overlook sign conventions, unit dimensions, and limiting boundary conditions.";
        }

        List<String> refinedCheatSheet = new ArrayList<>();
        if (content.cheatSheet() != null) {
            for (String formula : content.cheatSheet()) {
                if (formula != null && !formula.isBlank()) {
                    if (hasRubric3Issue && isContaminatedText(formula)) {
                        continue; // Strip leaked physics/schema formula
                    }
                    refinedCheatSheet.add(refineCheatSheetFormula(formula));
                }
            }
        }

        // If Rubric 3 stripped formulas or cheat sheet has < 2 items, replenish from domainFallback first
        if (refinedCheatSheet.size() < 2 && domainFallback != null && domainFallback.cheatSheet() != null) {
            for (String fbFormula : domainFallback.cheatSheet()) {
                if (!refinedCheatSheet.contains(fbFormula)) {
                    refinedCheatSheet.add(refineCheatSheetFormula(fbFormula));
                }
                if (refinedCheatSheet.size() >= 2) {
                    break;
                }
            }
        }

        // Ensure minimum 2 cheat sheet items
        if (refinedCheatSheet.size() < 2) {
            if (refinedCheatSheet.isEmpty()) {
                refinedCheatSheet.add("$$\\text{Relation 1: Establish governing conservation principles}$$");
            }
            refinedCheatSheet.add("$$\\text{Relation 2: Verify dimensional homogeneity and boundary limits}$$");
        }

        StepByStepBreakdownDto breakdown = content.stepByStepBreakdown();
        StepByStepBreakdownDto fallbackBreakdown = domainFallback != null ? domainFallback.stepByStepBreakdown() : null;

        boolean breakdownContaminated = hasRubric3Issue && breakdown != null && (
                isContaminatedText(breakdown.questionStem())
                || isContaminatedText(breakdown.finalAnswer())
                || (breakdown.steps() != null && breakdown.steps().stream().anyMatch(this::isContaminatedText))
        );

        if (breakdownContaminated && fallbackBreakdown != null) {
            breakdown = fallbackBreakdown;
        }

        String refinedQStem = (breakdown != null && breakdown.questionStem() != null && !breakdown.questionStem().isBlank())
                ? refineText(breakdown.questionStem())
                : (fallbackBreakdown != null ? fallbackBreakdown.questionStem()
                        : "Evaluate the primary governing parameters under baseline boundary conditions.");

        List<String> refinedSteps = new ArrayList<>();
        if (breakdown != null && breakdown.steps() != null) {
            for (int i = 0; i < breakdown.steps().size(); i++) {
                String step = breakdown.steps().get(i);
                if (step != null && !step.isBlank()) {
                    String clean = refineText(step);
                    clean = normalizeStepPrefix(clean, i + 1);
                    refinedSteps.add(clean);
                }
            }
        }

        // Ensure minimum 2 steps
        if (refinedSteps.size() < 2) {
            if (refinedSteps.isEmpty()) {
                refinedSteps.add("Step 1: State the primary governing relation and mark known boundary parameters.");
            }
            refinedSteps.add("Step 2: Substitute dimensionally consistent values and simplify to final solution.");
        }

        String refinedAnswer = (breakdown != null && breakdown.finalAnswer() != null)
                ? refineFinalAnswer(breakdown.finalAnswer())
                : (fallbackBreakdown != null ? fallbackBreakdown.finalAnswer() : "Standard numerical/algebraic solution");

        StepByStepBreakdownDto refinedBreakdown = new StepByStepBreakdownDto(refinedQStem, refinedSteps, refinedAnswer);
        return new MicroLessonContentDto(refinedCoreConcept, refinedTrap, refinedCheatSheet, refinedBreakdown);
    }

    private boolean isContaminatedText(String text) {
        if (text == null) {
            return false;
        }
        return text.contains("Indian bicycle chain")
                || text.contains("bicycle chain")
                || text.contains("\\Delta K + \\Delta U = W_{ext}")
                || text.contains("I = \\int r^2")
                || text.contains("4.5 \\times 10^3 \\text{ J}")
                || text.contains("Formula 1 strictly for the subtopic")
                || text.contains("Formula 2 strictly for the subtopic")
                || text.contains("Final Answer with units in KaTeX")
                || text.contains("[Insert");
    }

    /**
     * Refines an individual text snippet:
     * 1. Replaces \( ... \) with $$ ... $$
     * 2. Replaces \[ ... \] with $$ ... $$
     * 3. Replaces isolated single $ ... $ with $$ ... $$
     * 4. Escapes unescaped % inside $$ ... $$ blocks to \%
     * 5. Balances unclosed $$, {, and [ delimiters
     */
    public String refineText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String result = text;

        // 1. Convert \( ... \) to $$ ... $$
        Matcher parenMatcher = PAREN_DELIMITER_PATTERN.matcher(result);
        if (parenMatcher.find()) {
            result = parenMatcher.replaceAll(m -> Matcher.quoteReplacement("$$" + m.group(1).trim() + "$$"));
        }

        // 2. Convert \[ ... \] to $$ ... $$
        Matcher bracketMatcher = BRACKET_DELIMITER_PATTERN.matcher(result);
        if (bracketMatcher.find()) {
            result = bracketMatcher.replaceAll(m -> Matcher.quoteReplacement("$$" + m.group(1).trim() + "$$"));
        }

        // 3. Convert isolated single $ ... $ to $$ ... $$
        Matcher singleDollarMatcher = SINGLE_DOLLAR_PATTERN.matcher(result);
        if (singleDollarMatcher.find()) {
            result = singleDollarMatcher.replaceAll(m -> Matcher.quoteReplacement("$$" + m.group(1).trim() + "$$"));
        }

        // 4. Escape unescaped % inside $$ ... $$ blocks
        Matcher mathBlockMatcher = DOUBLE_DOLLAR_PATTERN.matcher(result);
        if (mathBlockMatcher.find()) {
            result = mathBlockMatcher.replaceAll(m -> {
                String mathContent = m.group(1);
                String escaped = UNESCAPED_PERCENT_IN_LATEX.matcher(mathContent).replaceAll("\\\\%");
                return Matcher.quoteReplacement("$$" + escaped + "$$");
            });
        }

        // 5. Balance unclosed delimiters and braces
        result = balanceDelimitersAndBraces(result);

        return result;
    }

    /**
     * Refines a cheat sheet formula and ensures it is wrapped in $$ ... $$
     * if it contains mathematical or chemical notation.
     */
    public String refineCheatSheetFormula(String formula) {
        if (formula == null || formula.isBlank()) {
            return formula;
        }

        String clean = formula.trim();

        // If not already enclosed in $$ ... $$, check for math/chem notation
        if (!clean.startsWith("$$") || !clean.endsWith("$$")) {
            boolean hasMathNotation = clean.contains("=")
                    || clean.contains("\\")
                    || clean.contains("^")
                    || clean.contains("_")
                    || clean.contains("->");

            if (hasMathNotation) {
                clean = "$$" + clean + "$$";
            }
        }

        return refineText(clean);
    }

    /**
     * Refines finalAnswer and wraps in $$ ... $$ if formulaic/numerical/percentage.
     */
    public String refineFinalAnswer(String answer) {
        if (answer == null || answer.isBlank()) {
            return "Standard numerical/algebraic solution";
        }

        String clean = answer.trim();
        if (!clean.startsWith("$$") || !clean.endsWith("$$")) {
            boolean hasMathNotation = clean.contains("=")
                    || clean.contains("\\")
                    || clean.contains("^")
                    || clean.contains("_")
                    || clean.contains("%")
                    || clean.contains("->");

            if (hasMathNotation) {
                clean = "$$" + clean + "$$";
            }
        }

        return refineText(clean);
    }

    /**
     * Ensures a step is cleanly prefixed with "Step <num>:".
     */
    public String normalizeStepPrefix(String step, int stepNumber) {
        if (step == null || step.isBlank()) {
            return "Step " + stepNumber + ":";
        }

        String prefix = "Step " + stepNumber + ":";
        Matcher matcher = STEP_PREFIX_PATTERN.matcher(step.trim());
        if (matcher.find()) {
            String rest = matcher.group(1).trim();
            return prefix + " " + rest;
        }

        return prefix + " " + step.trim();
    }

    /**
     * Balances unclosed $$, {, }, [, and ] delimiters.
     */
    private String balanceDelimitersAndBraces(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String result = text;

        // 1. Balance curly braces { and }
        int openBraces = 0;
        int closeBraces = 0;
        for (int i = 0; i < result.length(); i++) {
            char c = result.charAt(i);
            if (c == '\\') {
                i++; // skip escaped char
                continue;
            }
            if (c == '{') openBraces++;
            else if (c == '}') closeBraces++;
        }
        if (openBraces > closeBraces) {
            String closingBraces = "}".repeat(openBraces - closeBraces);
            String trimmed = result.trim();
            if (trimmed.endsWith("$$") && trimmed.length() > 2) {
                int lastDoubleDollar = result.lastIndexOf("$$");
                result = result.substring(0, lastDoubleDollar) + closingBraces + result.substring(lastDoubleDollar);
            } else {
                result = result + closingBraces;
            }
        }

        // 2. Balance brackets [ and ]
        int openBrackets = 0;
        int closeBrackets = 0;
        for (int i = 0; i < result.length(); i++) {
            char c = result.charAt(i);
            if (c == '\\') {
                i++; // skip escaped char
                continue;
            }
            if (c == '[') openBrackets++;
            else if (c == ']') closeBrackets++;
        }
        if (openBrackets > closeBrackets) {
            String closingBrackets = "]".repeat(openBrackets - closeBrackets);
            String trimmed = result.trim();
            if (trimmed.endsWith("$$") && trimmed.length() > 2) {
                int lastDoubleDollar = result.lastIndexOf("$$");
                result = result.substring(0, lastDoubleDollar) + closingBrackets + result.substring(lastDoubleDollar);
            } else {
                result = result + closingBrackets;
            }
        }

        // 3. Balance $$ delimiters
        int doubleDollarCount = 0;
        int pos = 0;
        while ((pos = result.indexOf("$$", pos)) != -1) {
            doubleDollarCount++;
            pos += 2;
        }
        if (doubleDollarCount % 2 != 0) {
            result = result + "$$";
        }

        return result;
    }
}
