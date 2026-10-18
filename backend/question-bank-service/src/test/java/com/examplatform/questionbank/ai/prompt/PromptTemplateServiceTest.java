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
package com.examplatform.questionbank.ai.prompt;

import com.examplatform.questionbank.microlesson.ai.MicroLessonPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PromptTemplateService Tests")
class PromptTemplateServiceTest {

    private PromptTemplateService promptTemplateService;
    private DefaultResourceLoader resourceLoader;

    @BeforeEach
    void setUp() {
        resourceLoader = new DefaultResourceLoader();
        promptTemplateService = new PromptTemplateService(
                resourceLoader,
                "classpath:prompts/",
                "v1"
        );
    }

    @Test
    @DisplayName("Should load and render microlesson system prompt from classpath resource")
    void shouldRenderMicroLessonSystemPrompt() {
        String prompt = promptTemplateService.render("microlesson/system-prompt", Map.of());

        assertThat(prompt).isNotBlank();
        assertThat(prompt).contains("You are an expert master faculty and pedagogical content designer");
        assertThat(prompt).contains("stepByStepBreakdown");
        assertThat(prompt).contains("cheatSheet");
        assertThat(prompt).contains("\\frac{1}{100}");
        assertThat(prompt).contains("r_{eff}");
    }

    @Test
    @DisplayName("Should render microlesson user prompt with parameter substitutions")
    void shouldRenderMicroLessonUserPromptWithParameters() {
        Map<String, Object> vars = Map.of(
                "subject", "Physics",
                "topic", "Electrodynamics",
                "subtopic", "Gauss's Law",
                "targetExam", "JEE_ADVANCED",
                "referenceSection", "\nFocus on cylindrical symmetry\n"
        );

        String prompt = promptTemplateService.render("microlesson/user-prompt", vars);

        assertThat(prompt).contains("Subject: Physics");
        assertThat(prompt).contains("Topic: Electrodynamics");
        assertThat(prompt).contains("Sub-Topic: Gauss's Law");
        assertThat(prompt).contains("Target Exam: JEE_ADVANCED");
        assertThat(prompt).contains("Focus on cylindrical symmetry");
        assertThat(prompt).contains("Ensure KaTeX notation is syntactically balanced");
    }

    @Test
    @DisplayName("Should load and render question generation system prompt from classpath resource")
    void shouldRenderQuestionSystemPrompt() {
        Map<String, Object> vars = Map.of("questionType", "SINGLE_MCQ");
        String prompt = promptTemplateService.render("question/system-prompt", vars);

        assertThat(prompt).isNotBlank();
        assertThat(prompt).contains("You are an expert examination question generator");
        assertThat(prompt).contains("\"questionType\": \"SINGLE_MCQ\"");
    }

    @Test
    @DisplayName("Should render question user prompt with parameters and context")
    void shouldRenderQuestionUserPrompt() {
        Map<String, Object> vars = Map.of(
                "count", 3,
                "parameters", "- Subject: Mathematics\n- Topic: Calculus\n",
                "samplesSection", "\nSample 1: Integrate sin(x)\n",
                "ragSection", "\n1. Existing question on limits\n"
        );

        String prompt = promptTemplateService.render("question/user-prompt", vars);

        assertThat(prompt).contains("Generate 3 question(s) with these parameters:");
        assertThat(prompt).contains("Subject: Mathematics");
        assertThat(prompt).contains("Sample 1: Integrate sin(x)");
        assertThat(prompt).contains("1. Existing question on limits");
        assertThat(prompt).contains("Generate the questions now as a JSON array:");
    }

    @Test
    @DisplayName("Should resolve prompt from YAML bundle via dot notation")
    void shouldResolveYamlBundlePrompts() {
        String systemPrompt = promptTemplateService.render("microlesson.system", Map.of());
        assertThat(systemPrompt).contains("expert master faculty");

        String userPrompt = promptTemplateService.render("microlesson.user", Map.of(
                "subject", "Chemistry",
                "topic", "Thermodynamics",
                "subtopic", "Hess's Law",
                "targetExam", "NEET",
                "referenceSection", ""
        ));
        assertThat(userPrompt).contains("Subject: Chemistry");
        assertThat(userPrompt).contains("Sub-Topic: Hess's Law");

        String questionSystem = promptTemplateService.render("question.system", Map.of("questionType", "SINGLE_MCQ"));
        assertThat(questionSystem).contains("expert examination question generator");
        assertThat(questionSystem).contains("\"questionType\": \"SINGLE_MCQ\"");
    }

    @Test
    @DisplayName("Should render from direct Resource")
    void shouldRenderFromDirectResource() {
        String templateString = "Hello {name}, welcome to {place}!";
        Resource resource = new ByteArrayResource(templateString.getBytes(StandardCharsets.UTF_8));

        String rendered = promptTemplateService.render(resource, Map.of(
                "name", "Candidate",
                "place", "NAG Assessment"
        ));

        assertThat(rendered).isEqualTo("Hello Candidate, welcome to NAG Assessment!");
    }

    @Test
    @DisplayName("Should clear cache and reload successfully")
    void shouldClearCache() {
        promptTemplateService.render("microlesson/system-prompt", Map.of());
        promptTemplateService.clearCache();

        String prompt = promptTemplateService.render("microlesson/system-prompt", Map.of());
        assertThat(prompt).isNotBlank();
    }

    @Test
    @DisplayName("Should throw NoSuchElementException for non-existent prompt")
    void shouldThrowForUnknownPrompt() {
        assertThatThrownBy(() -> promptTemplateService.render("nonexistent/prompt", Map.of()))
                .hasMessageContaining("Prompt template not found");
    }

    @Test
    @DisplayName("MicroLessonPromptBuilder should use PromptTemplateService when injected")
    void shouldIntegrateWithMicroLessonPromptBuilder() {
        MicroLessonPromptBuilder builder = new MicroLessonPromptBuilder(promptTemplateService);

        String sys = builder.getSystemPrompt();
        assertThat(sys).contains("expert master faculty");

        String user = builder.buildUserPrompt("Biology", "Genetics", "Mendelian Inheritance", "NEET", "Focus on Punnett square");
        assertThat(user).contains("Biology");
        assertThat(user).contains("Mendelian Inheritance");
        assertThat(user).contains("Focus on Punnett square");
    }

    @Test
    @DisplayName("MicroLessonPromptBuilder should fallback gracefully when PromptTemplateService is null")
    void shouldFallbackWhenPromptTemplateServiceIsNull() {
        MicroLessonPromptBuilder builder = new MicroLessonPromptBuilder();

        String sys = builder.getSystemPrompt();
        assertThat(sys).isEqualTo(MicroLessonPromptBuilder.SYSTEM_PROMPT);

        String user = builder.buildUserPrompt("Physics", "Kinematics", "Projectile Motion", "JEE_MAIN");
        assertThat(user).contains("Physics");
        assertThat(user).contains("Kinematics");
    }
}
