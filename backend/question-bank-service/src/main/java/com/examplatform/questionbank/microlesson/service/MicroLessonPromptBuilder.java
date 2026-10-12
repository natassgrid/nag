// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.service;

import org.springframework.stereotype.Component;

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

        Output must be strictly valid JSON with no markdown fences, matching this schema:
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

    public String buildUserPrompt(String subject, String topic, String subtopic, String targetExam) {
        String exam = (targetExam != null && !targetExam.isBlank()) ? targetExam : "JEE_MAIN";
        return String.format("""
            Generate a remedial micro-lesson for the following syllabus sub-topic:
            - Subject: %s
            - Topic: %s
            - Sub-Topic: %s
            - Target Exam: %s

            Ensure KaTeX notation is syntactically balanced and accurate. Return ONLY the JSON object.
            """, subject, topic, subtopic, exam);
    }
}
