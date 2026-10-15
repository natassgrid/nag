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
        3. The Cheat Sheet: Provide 2 to 3 indispensable mathematical formulas or core frameworks with KaTeX notation
           (enclosed in $...$ or $$...$$).
        4. Step-by-Step Breakdown: Present one standard medium-difficulty exam problem deconstructed into explicit numbered deduction steps
           followed by the unambiguous final answer with proper physical/mathematical units.

        CRITICAL INSTRUCTION:
        Output must be strictly raw, valid JSON only.
        Do NOT wrap in markdown code blocks or fences (no ``` or ```json).
        Do NOT include any greetings, introductory text, commentary, preambles, or explanations outside the JSON object.
        Ensure all LaTeX backslashes inside JSON strings are properly escaped (e.g., \\Delta, \\%, \\frac).

        The output must match this exact schema:
        {
          "coreConcept": "3 simple sentences with native Indian analogy",
          "trap": "Common pitfall or exam trap",
          "cheatSheet": ["$formula_1$", "$formula_2$"],
          "stepByStepBreakdown": {
            "questionStem": "Problem statement",
            "steps": ["Step 1: ...", "Step 2: ...", "Step 3: ..."],
            "finalAnswer": "Explicit answer"
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

        sb.append("\nEnsure KaTeX notation is syntactically balanced and accurate. Return ONLY the raw valid JSON object with no markdown fences and no other text.\n");
        return sb.toString();
    }
}
