// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.ai;

import com.examplatform.shared.constants.SharedConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MicroLessonPromptBuilder Prompt Engineering Tests")
class MicroLessonPromptBuilderTest {

    private MicroLessonPromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new MicroLessonPromptBuilder();
    }

    @Test
    @DisplayName("System prompt enforces Indian contexts, 4-part structure, and KaTeX notation")
    void testSystemPromptRequirements() {
        String systemPrompt = MicroLessonPromptBuilder.SYSTEM_PROMPT;
        assertThat(systemPrompt).contains("Indian Competitive Examinations");
        assertThat(systemPrompt).contains("Core Concept");
        assertThat(systemPrompt).contains("The Trap");
        assertThat(systemPrompt).contains("The Cheat Sheet");
        assertThat(systemPrompt).contains("Step-by-Step Breakdown");
        assertThat(systemPrompt).contains("KaTeX notation");
    }

    @Test
    @DisplayName("User prompt injects subject, topic, subtopic and defaults target exam")
    void testUserPromptFormatting() {
        String userPrompt = promptBuilder.buildUserPrompt("Physics", "Mechanics", "Rotational Dynamics", null);
        assertThat(userPrompt).contains("Physics");
        assertThat(userPrompt).contains("Mechanics");
        assertThat(userPrompt).contains("Rotational Dynamics");
        assertThat(userPrompt).contains(SharedConstants.JEE_MAIN); // default
    }

    @Test
    @DisplayName("User prompt preserves custom exam target (NEET_UG)")
    void testUserPromptWithCustomExam() {
        String userPrompt = promptBuilder.buildUserPrompt("Biology", "Cell Biology", "Mitosis", "NEET_UG");
        assertThat(userPrompt).contains("NEET_UG");
        assertThat(userPrompt).contains("Mitosis");
    }
}
