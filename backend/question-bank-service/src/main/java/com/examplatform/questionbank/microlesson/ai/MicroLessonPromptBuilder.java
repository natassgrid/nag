// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.ai;

import com.examplatform.questionbank.ai.prompt.PromptTemplateService;
import com.examplatform.questionbank.microlesson.ai.agent.MicroLessonBlueprint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MicroLessonPromptBuilder {

    public static final String SYSTEM_PROMPT = """
        You are an expert master faculty and pedagogical content designer for Indian Competitive Examinations
        (including JEE Main & Advanced, NEET-UG, UPSC Civil Services, IBPS/SBI PO, RRB NTPC, and SSC CGL).
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
        Always close both the nested "stepByStepBreakdown" object "}" and the root JSON object "}".
        NEVER copy the example placeholder formulas from the schema below; generate formulas, concepts, and problems strictly for the requested Sub-Topic.
        Do NOT wrap in markdown code blocks or fences (no ``` or ```json).
        Do NOT include any greetings, introductory text, commentary, preambles, or explanations outside the JSON object.
        Ensure all LaTeX backslashes inside JSON strings are properly escaped (e.g., \\ce, \\Delta, \\%, \\frac, \\times).

        The output must match this exact schema:
        {
          "coreConcept": "3 simple sentences with native Indian analogy and $$formula$$ notations specific to the subtopic",
          "trap": "Common pitfall or exam trap highlighting $$notations$$ specific to the subtopic",
          "cheatSheet": ["$$\\text{Formula 1 strictly for the subtopic}$$", "$$\\text{Formula 2 strictly for the subtopic}$$"],
          "stepByStepBreakdown": {
            "questionStem": "Problem statement with $$formulas$$ strictly for the subtopic",
            "steps": [
              "Step 1: Identify given parameters and state the governing formula in $$...$$ syntax.",
              "Step 2: Substitute the numerical values and simplify step-by-step in $$...$$ syntax.",
              "Step 3: Compute the final result with proper units or percentage in $$...$$ syntax."
            ],
            "finalAnswer": "$$\\text{Final Answer with units in KaTeX}$$"
          }
        }
        """;

    private final PromptTemplateService promptTemplateService;

    public MicroLessonPromptBuilder() {
        this(null);
    }

    @Autowired
    public MicroLessonPromptBuilder(@Autowired(required = false) PromptTemplateService promptTemplateService) {
        this.promptTemplateService = promptTemplateService;
    }

    public String getSystemPrompt() {
        if (promptTemplateService != null) {
            try {
                String rendered = promptTemplateService.render("microlesson/system-prompt", Map.of());
                if (rendered != null && !rendered.isBlank()) {
                    return rendered.replace("\\{", "{").replace("\\}", "}");
                }
            } catch (Exception ignored) {}
        }
        return SYSTEM_PROMPT;
    }

    public String buildSystemPrompt() {
        return getSystemPrompt();
    }

    public String buildUserPrompt(String subject, String topic, String subtopic, String targetExam) {
        return buildUserPrompt(subject, topic, subtopic, targetExam, null, null);
    }

    public String buildUserPrompt(String subject, String topic, String subtopic, String targetExam, String referenceText) {
        return buildUserPrompt(subject, topic, subtopic, targetExam, referenceText, null);
    }

    public String buildUserPrompt(
            String subject,
            String topic,
            String subtopic,
            String targetExam,
            String referenceText,
            MicroLessonBlueprint blueprint) {
        String exam = (targetExam != null && !targetExam.isBlank()) ? targetExam : "JEE_MAIN";

        StringBuilder extraContext = new StringBuilder();
        if (blueprint != null && blueprint.formattedBlueprintPrompt() != null && !blueprint.formattedBlueprintPrompt().isBlank()) {
            extraContext.append("\n").append(blueprint.formattedBlueprintPrompt().trim()).append("\n");
        }
        if (referenceText != null && !referenceText.isBlank()) {
            extraContext.append("\nAdditional Faculty Reference Context / Steering Notes:\n")
                    .append(referenceText.trim())
                    .append("\n");
        }

        if (promptTemplateService != null) {
            try {
                Map<String, Object> vars = new HashMap<>();
                vars.put("subject", subject != null ? subject : "General");
                vars.put("topic", topic != null ? topic : "General");
                vars.put("subtopic", subtopic != null ? subtopic : "General");
                vars.put("targetExam", exam);
                vars.put("referenceSection", extraContext.toString());

                String rendered = promptTemplateService.render("microlesson/user-prompt", vars);
                if (rendered != null && !rendered.isBlank()) {
                    return rendered.replace("\\{", "{").replace("\\}", "}");
                }
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

        if (!extraContext.isEmpty()) {
            sb.append(extraContext);
        }

        sb.append("\nEnsure KaTeX notation is syntactically balanced and accurate. Ensure all formulas and chemical reactions in cheatSheet, steps, and finalAnswer are enclosed in $$...$$ with double-escaped backslashes (\\ce, \\frac, \\times, \\%). Always close both the stepByStepBreakdown object and the root JSON object with '}}'. Return ONLY the raw valid JSON object with no markdown fences.\n");
        return sb.toString();
    }
}
