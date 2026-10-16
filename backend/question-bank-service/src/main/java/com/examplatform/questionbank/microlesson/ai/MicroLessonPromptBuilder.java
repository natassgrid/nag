// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.ai;

import com.examplatform.questionbank.ai.prompt.PromptTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MicroLessonPromptBuilder {

    public static final String SYSTEM_PROMPT = """
        You are an expert master faculty and pedagogical content designer for Indian Competitive Examinations
        (including JEE Main & Advanced, NEET-UG, UPSC Civil Services, and SSC CGL).
        Your task is to produce high-impact, modular remedial micro-lessons designed to cure conceptual gaps
        diagnosed during mock test evaluation.

        Guidelines:
        1. Core Concept: Must be explained in exactly 3 simple, lucid sentences using native Indian contexts or
           everyday relatable analogies (e.g., flywheel in an atta chakki, steering a loaded bicycle, tea kettle steam,
           railway track expansion, monsoon rain runoffs).
        2. The Trap: Directly highlight the exact psychological, sign convention, or calculation trap test-setters
           deliberately insert to mislead students.
        3. The Cheat Sheet: Provide 2 to 3 indispensable mathematical formulas or core frameworks with KaTeX notation.
           EVERY formula in cheatSheet MUST be individually enclosed in $$...$$ syntax (e.g. "$$[1 - x] = \\frac{1}{100}$$").
        4. Step-by-Step Breakdown: Present one standard medium-difficulty exam problem deconstructed into explicit numbered deduction steps
           followed by the unambiguous final answer with proper physical/mathematical units.
           ALL mathematical equations, variables, expressions, and numerical calculations within steps and finalAnswer MUST be enclosed in $$...$$ syntax (e.g., "$$r = \\frac{500}{500 \\times 365}$$", "$$r_{eff} = 4.025\\%$$").

        Formatting & KaTeX Rules:
        - ALL mathematical, physical, and chemical formulas, expressions, variables, percentages, and unit notations in EVERY field (coreConcept, trap, cheatSheet, questionStem, steps, and finalAnswer) MUST be enclosed in $$...$$ LaTeX syntax.
        - Chemical reactions MUST use valid LaTeX syntax (e.g. $$\\ce{2H2 + O2 -> 2H2O}$$).
        - NEVER use \\( ... \\) or \\[ ... \\] or plain parentheses/brackets around formulas. NEVER use single $.
        - In LaTeX math mode ($$...$$), always write percentage symbols as \\% (e.g. $$99.9\\%$$ or $$4.025\\%$$).
        - In JSON strings, ALWAYS escape all LaTeX backslashes with double backslashes: write \\ce, \\frac, \\times, \\Delta, \\sqrt, \\theta, \\pm, \\sum, \\int, \\% (never emit single backslash \\frac or \\times).

        CRITICAL INSTRUCTION:
        Output must be strictly raw, valid JSON only.
        Do NOT wrap in markdown code blocks or fences (no ``` or ```json).
        Do NOT include any greetings, introductory text, commentary, preambles, or explanations outside the JSON object.
        Ensure all LaTeX backslashes inside JSON strings are properly escaped (e.g., \\ce, \\Delta, \\%, \\frac, \\times).

        The output must match this exact schema:
        {
          "coreConcept": "3 simple sentences with native Indian analogy and $$formula$$ notations if applicable",
          "trap": "Common pitfall or exam trap highlighting $$notations$$",
          "cheatSheet": ["$$[1 - x] = \\frac{1}{100}$$", "$$\\Delta K + \\Delta U = W_{ext}$$"],
          "stepByStepBreakdown": {
            "questionStem": "Problem statement with $$formulas$$",
            "steps": [
              "Step 1: Convert the interest rate per annum to a decimal: $$r = \\frac{500}{500 \\times 365}$$ per year.",
              "Step 2: Calculate the effective annual interest rate: $$r_{eff} = \\frac{1 + \\frac{500}{500 \\times 365}}{1 + \\frac{500}{500 \\times 365}} - 1$$.",
              "Step 3: Find the annual interest rate: $$r_{eff} \\times 100 = 4.025\\%$$, yielding the exact annual percentage."
            ],
            "finalAnswer": "$$r_{eff} = 4.025\\%$$"
          }
        }
        """;

    private final PromptTemplateService promptTemplateService;

    public MicroLessonPromptBuilder() {
        this(null);
    }

    @Autowired
    public MicroLessonPromptBuilder(PromptTemplateService promptTemplateService) {
        this.promptTemplateService = promptTemplateService;
    }

    public String getSystemPrompt() {
        if (promptTemplateService != null) {
            try {
                return promptTemplateService.render("microlesson/system-prompt", Map.of());
            } catch (Exception ignored) {}
        }
        return SYSTEM_PROMPT;
    }

    public String buildSystemPrompt() {
        return getSystemPrompt();
    }

    public String buildUserPrompt(String subject, String topic, String subtopic, String targetExam) {
        return buildUserPrompt(subject, topic, subtopic, targetExam, null);
    }

    public String buildUserPrompt(String subject, String topic, String subtopic, String targetExam, String referenceText) {
        String exam = (targetExam != null && !targetExam.isBlank()) ? targetExam : "JEE_MAIN";

        if (promptTemplateService != null) {
            try {
                String referenceSection = "";
                if (referenceText != null && !referenceText.isBlank()) {
                    referenceSection = "\nAdditional Faculty Reference Context / Steering Notes:\n" + referenceText.trim() + "\n";
                }
                Map<String, Object> vars = new HashMap<>();
                vars.put("subject", subject != null ? subject : "General");
                vars.put("topic", topic != null ? topic : "General");
                vars.put("subtopic", subtopic != null ? subtopic : "General");
                vars.put("targetExam", exam);
                vars.put("referenceSection", referenceSection);

                return promptTemplateService.render("microlesson/user-prompt", vars);
            } catch (Exception ignored) {}
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
            Generate a remedial micro-lesson for the following syllabus sub-topic:
            - Subject: %s
            - Topic: %s
            - Sub-Topic: %s
            - Target Exam: %s
            """, subject, topic, subtopic, exam));

        if (referenceText != null && !referenceText.isBlank()) {
            sb.append("\nAdditional Faculty Reference Context / Steering Notes:\n");
            sb.append(referenceText.trim());
            sb.append("\n");
        }

        sb.append("\nEnsure KaTeX notation is syntactically balanced and accurate. Ensure all formulas and chemical reactions in cheatSheet, steps, and finalAnswer are enclosed in $$...$$ with double-escaped backslashes (\\ce, \\frac, \\times, \\%). Return ONLY the raw valid JSON object with no markdown fences.\n");
        return sb.toString();
    }
}
