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

import com.examplatform.questionbank.microlesson.ai.KaTexValidator;
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
 * Validates {@link MicroLessonContentDto} against deterministic pedagogical & technical rubrics:
 * <ul>
 *   <li>Rubric 1: Structural completeness (all fields present and non-blank).</li>
 *   <li>Rubric 2: KaTeX syntax validity via {@link KaTexValidator} ($$...$$ required, no \( \) or \[ \] or single $).</li>
 *   <li>Rubric 3: Domain & subtopic coherence (verifies non-generic, domain-aligned content).</li>
 *   <li>Rubric 4: Minimum counts (cheat sheet has >= 2 items, breakdown has >= 2 steps).</li>
 * </ul>
 */
@Component
public class MicroLessonCriticAgent {

    private static final Logger log = LoggerFactory.getLogger(MicroLessonCriticAgent.class);

    private static final Pattern DOUBLE_DOLLAR_PATTERN = Pattern.compile("\\$\\$(.*?)\\$\\$", Pattern.DOTALL);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
            "(?i)\\[Insert|\\bTODO\\b|\\bTBD\\b|\\bPLACEHOLDER\\b|\\bLorem ipsum\\b|Formula [12] strictly for the subtopic|Final Answer with units in KaTeX"
    );

    private final KaTexValidator kaTexValidator;

    public MicroLessonCriticAgent(KaTexValidator kaTexValidator) {
        this.kaTexValidator = kaTexValidator;
    }

    public MicroLessonCriticResult review(MicroLessonContentDto content) {
        return review(content, null, null, null);
    }

    public MicroLessonCriticResult review(
            MicroLessonContentDto content,
            String subject,
            String topic,
            String subtopic) {

        List<String> issues = new ArrayList<>();
        List<String> verifiedRubrics = new ArrayList<>();
        double score = 1.0;

        if (content == null) {
            return new MicroLessonCriticResult(false, 0.0, List.of("MicroLesson content cannot be null"), List.of());
        }

        // ==========================================
        // Rubric 1: Structural Completeness
        // ==========================================
        boolean structuralComplete = true;

        if (content.coreConcept() == null || content.coreConcept().isBlank()) {
            issues.add("Rubric 1: Core concept is missing or blank");
            score -= 0.15;
            structuralComplete = false;
        }

        if (content.trap() == null || content.trap().isBlank()) {
            issues.add("Rubric 1: Trap description is missing or blank");
            score -= 0.15;
            structuralComplete = false;
        }

        if (content.cheatSheet() == null || content.cheatSheet().isEmpty()) {
            issues.add("Rubric 1: Cheat sheet is missing or empty");
            score -= 0.15;
            structuralComplete = false;
        }

        StepByStepBreakdownDto breakdown = content.stepByStepBreakdown();
        if (breakdown == null) {
            issues.add("Rubric 1: Step-by-step breakdown is null");
            score -= 0.35;
            structuralComplete = false;
        } else {
            if (breakdown.questionStem() == null || breakdown.questionStem().isBlank()) {
                issues.add("Rubric 1: Breakdown question stem is missing or blank");
                score -= 0.10;
                structuralComplete = false;
            }
            if (breakdown.steps() == null || breakdown.steps().isEmpty()) {
                issues.add("Rubric 1: Breakdown steps list is missing or empty");
                score -= 0.15;
                structuralComplete = false;
            }
            if (breakdown.finalAnswer() == null || breakdown.finalAnswer().isBlank()) {
                issues.add("Rubric 1: Breakdown final answer is missing or blank");
                score -= 0.10;
                structuralComplete = false;
            }
        }

        if (structuralComplete) {
            verifiedRubrics.add("Rubric 1: Structural completeness verified (all sections present and non-blank)");
        }

        // ==========================================
        // Rubric 2: KaTeX Syntax Validity
        // ==========================================
        List<String> katexErrors = kaTexValidator.validateContent(content);
        for (String err : katexErrors) {
            issues.add("Rubric 2 KaTeX Error: " + err);
            score -= 0.05;
        }

        List<String> allTextSnippets = collectAllText(content);
        boolean katexFormatValid = katexErrors.isEmpty();

        boolean foundParen = false;
        boolean foundBracket = false;
        boolean foundSingleDollar = false;
        boolean foundUnescapedPercent = false;

        for (String text : allTextSnippets) {
            if (text == null) continue;

            // Check prohibited \( ... \)
            if (!foundParen && (text.contains("\\(") || text.contains("\\)"))) {
                issues.add("Rubric 2: Prohibited LaTeX delimiter '\\(...\\)' detected. Must use '$$...$$'");
                score -= 0.10;
                katexFormatValid = false;
                foundParen = true;
            }

            // Check prohibited \[ ... \]
            if (!foundBracket && (text.contains("\\[") || text.contains("\\]"))) {
                issues.add("Rubric 2: Prohibited LaTeX delimiter '\\[...\\]' detected. Must use '$$...$$'");
                score -= 0.10;
                katexFormatValid = false;
                foundBracket = true;
            }

            // Check single dollar $ ... $
            if (!foundSingleDollar && hasIsolatedSingleDollar(text)) {
                issues.add("Rubric 2: Prohibited single '$' delimiter detected. All formulas MUST be enclosed in '$$...$$'");
                score -= 0.10;
                katexFormatValid = false;
                foundSingleDollar = true;
            }

            // Check unescaped % inside $$...$$
            if (!foundUnescapedPercent && hasUnescapedPercentInsideMath(text)) {
                issues.add("Rubric 2: Unescaped '%' inside KaTeX block detected (must be '\\%')");
                score -= 0.05;
                katexFormatValid = false;
                foundUnescapedPercent = true;
            }
        }

        if (katexFormatValid) {
            verifiedRubrics.add("Rubric 2: KaTeX syntax validity verified ($$...$$ format strictly adhered to)");
        }

        // ==========================================
        // Rubric 3: Domain & Subtopic Coherence
        // ==========================================
        boolean domainCoherent = true;

        // Check for template placeholders
        for (String text : allTextSnippets) {
            if (text != null && PLACEHOLDER_PATTERN.matcher(text).find()) {
                issues.add("Rubric 3: Unfilled template placeholder detected in content");
                score -= 0.25;
                domainCoherent = false;
                break;
            }
        }

        // Check domain mismatch with generic rotational mechanics fallback
        if (isNonPhysicsDomain(subject, topic, subtopic)) {
            String combinedText = String.join(" ", allTextSnippets);
            if (combinedText.contains("Indian bicycle chain")
                    || combinedText.contains("bicycle chain")
                    || combinedText.contains("\\Delta K + \\Delta U = W_{ext}")
                    || combinedText.contains("I = \\int r^2")
                    || combinedText.contains("4.5 \\times 10^3 \\text{ J}")) {
                issues.add("Rubric 3: Domain mismatch: non-physics topic '"
                        + (subtopic != null ? subtopic : topic)
                        + "' contains unrelated physics rotational mechanics formulas or fallback metaphor");
                score -= 0.35;
                domainCoherent = false;
            }
        }

        if (domainCoherent) {
            verifiedRubrics.add("Rubric 3: Domain and subtopic coherence verified");
        }

        // ==========================================
        // Rubric 4: Minimum Counts & Pedagogical Depth
        // ==========================================
        boolean countsSatisfied = true;
        int cheatSheetCount = content.cheatSheet() != null ? content.cheatSheet().size() : 0;
        if (cheatSheetCount < 2) {
            issues.add("Rubric 4: Cheat sheet must contain at least 2 items (found: " + cheatSheetCount + ")");
            score -= 0.15;
            countsSatisfied = false;
        }

        int stepCount = (breakdown != null && breakdown.steps() != null) ? breakdown.steps().size() : 0;
        if (stepCount < 2) {
            issues.add("Rubric 4: Step-by-step breakdown must contain at least 2 steps (found: " + stepCount + ")");
            score -= 0.15;
            countsSatisfied = false;
        }

        if (countsSatisfied) {
            verifiedRubrics.add("Rubric 4: High-yield depth verified (at least 2 cheat sheet items and 2 breakdown steps)");
        }

        double finalScore = Math.max(0.0, Math.min(1.0, score));
        boolean approved = issues.isEmpty();

        return new MicroLessonCriticResult(approved, finalScore, issues, verifiedRubrics);
    }

    private boolean isNonPhysicsDomain(String subject, String topic, String subtopic) {
        if (subject == null && topic == null) {
            return false;
        }
        String combined = (
                (subject != null ? subject : "") + " "
                + (topic != null ? topic : "") + " "
                + (subtopic != null ? subtopic : "")
        ).toLowerCase();

        boolean isPhysics = combined.contains("physic")
                || combined.contains("mechanic")
                || combined.contains("kinematic")
                || combined.contains("dynamic")
                || combined.contains("rotat")
                || combined.contains("inertia")
                || combined.contains("gravitat")
                || combined.contains("thermodynamic");

        return !isPhysics;
    }

    private List<String> collectAllText(MicroLessonContentDto content) {
        List<String> list = new ArrayList<>();
        if (content.coreConcept() != null) list.add(content.coreConcept());
        if (content.trap() != null) list.add(content.trap());
        if (content.cheatSheet() != null) list.addAll(content.cheatSheet());
        if (content.stepByStepBreakdown() != null) {
            StepByStepBreakdownDto b = content.stepByStepBreakdown();
            if (b.questionStem() != null) list.add(b.questionStem());
            if (b.steps() != null) list.addAll(b.steps());
            if (b.finalAnswer() != null) list.add(b.finalAnswer());
        }
        return list;
    }

    private boolean hasIsolatedSingleDollar(String text) {
        if (text == null) return false;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                i++; // skip escaped character like \$
                continue;
            }
            if (c == '$') {
                if (i + 1 < len && text.charAt(i + 1) == '$') {
                    i++; // skip display math $$
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasUnescapedPercentInsideMath(String text) {
        if (text == null) return false;
        Matcher matcher = DOUBLE_DOLLAR_PATTERN.matcher(text);
        while (matcher.find()) {
            String mathContent = matcher.group(1);
            int len = mathContent.length();
            for (int i = 0; i < len; i++) {
                char c = mathContent.charAt(i);
                if (c == '\\') {
                    i++; // skip escaped char like \%
                    continue;
                }
                if (c == '%') {
                    return true;
                }
            }
        }
        return false;
    }
}
