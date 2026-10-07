/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 Open Digital Public Infrastructure (DPI) Platform Contributors
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
package com.examplatform.questionbank.ai.generation.multiagent;

import com.examplatform.questionbank.ai.generation.ClarifyRequirementsRequest;
import com.examplatform.questionbank.ai.generation.ClarifyRequirementsResponse;
import com.examplatform.questionbank.ai.generation.QuestionGenerationRequest;
import com.examplatform.questionbank.ai.generation.QuestionGenerationResponse;
import com.examplatform.questionbank.ai.parser.NormalizedSampleQuestion;
import com.examplatform.questionbank.ai.similarity.SimilarityCheckResult;
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.repository.SimilarityResult;
import com.examplatform.questionbank.service.SimilarityDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Multi-Agent Specialized Agents Tests")
class MultiAgentPipelineTest {

    private RequirementAnalystAgent requirementAnalystAgent;
    private QuestionAuthorAgent questionAuthorAgent;
    private PsychometricCriticAgent psychometricCriticAgent;
    private RefinementAgent refinementAgent;

    @Mock
    private SimilarityDetectionService similarityDetectionService;
    private SimilarityAuditorAgent similarityAuditorAgent;

    @BeforeEach
    void setUp() {
        requirementAnalystAgent = new RequirementAnalystAgent();
        questionAuthorAgent = new QuestionAuthorAgent();
        psychometricCriticAgent = new PsychometricCriticAgent();
        refinementAgent = new RefinementAgent();
        similarityAuditorAgent = new SimilarityAuditorAgent(similarityDetectionService);
    }

    @Test
    @DisplayName("QuestionGenerationRequest builds composite search query for vector search")
    void testCompositeSearchQuery() {
        QuestionGenerationRequest req = QuestionGenerationRequest.builder()
                .subject("Physics")
                .topic("Mechanics")
                .subtopic("Rotational Motion")
                .description("Calculate torque for a rigid body")
                .build();

        assertThat(req.buildSearchQuery())
                .isEqualTo("Physics Mechanics Rotational Motion Calculate torque for a rigid body");

        // Also test with rawTextInput
        req.setDescription(null);
        req.setRawTextInput("Moment of inertia of a disc");
        assertThat(req.buildSearchQuery())
                .isEqualTo("Physics Mechanics Rotational Motion Moment of inertia of a disc");

        // Test fallback when no description or rawTextInput
        QuestionGenerationRequest fallbackReq = QuestionGenerationRequest.builder()
                .subject("Chemistry")
                .topic("Electrochemistry")
                .subtopic("Nernst Equation")
                .build();
        assertThat(fallbackReq.buildSearchQuery())
                .isEqualTo("Electrochemistry Nernst Equation");
    }

    @Test
    @DisplayName("RequirementAnalystAgent formulates generation blueprint from author request & samples")
    void testRequirementAnalystBlueprint() {
        QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                .subject("Chemistry")
                .topic("Electrochemistry")
                .difficulty("HARD")
                .cognitiveLevel("ANALYZE")
                .questionType("SINGLE_MCQ")
                .targetExam("JEE_ADV")
                .build();

        NormalizedSampleQuestion sample = NormalizedSampleQuestion.builder()
                .stem("Consider standard reduction potentials...")
                .hasMathOrChemistry(true)
                .parsingTier("TIER_0_LOCAL_TEXT")
                .build();

        String blueprint = requirementAnalystAgent.formulateBlueprint(request, List.of(sample));

        assertThat(blueprint).contains("JEE_ADV");
        assertThat(blueprint).contains("Electrochemistry");
        assertThat(blueprint).contains("Preserve LaTeX math ($$...$$) and chemistry");
    }

    @Test
    @DisplayName("RequirementAnalystAgent clarifies ambiguous author requests")
    void testRequirementAnalystClarification() {
        ClarifyRequirementsRequest request = ClarifyRequirementsRequest.builder()
                .subject("Chemistry")
                .authorPrompt("tough electrochem questions like sample")
                .build();

        ClarifyRequirementsResponse response = requirementAnalystAgent.clarifyRequirements(request);

        assertThat(response.isClarificationNeeded()).isTrue();
        assertThat(response.getClarificationQuestions()).isNotEmpty();
        assertThat(response.getSuggestedSubtopics()).contains("Nernst Equation & Cell EMF");
    }

    @Test
    @DisplayName("QuestionAuthorAgent embeds blueprint and few-shot examples into prompt")
    void testQuestionAuthorPrompt() {
        QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                .count(2)
                .build();

        NormalizedSampleQuestion sample = NormalizedSampleQuestion.builder()
                .stem("Sample Question Stem")
                .options(List.of(QuestionOption.builder().id("A").text("Option 1").correct(true).build()))
                .answerKey("A")
                .build();

        String prompt = questionAuthorAgent.buildPromptWithBlueprint("BLUEPRINT_TEXT", request, List.of(sample));

        assertThat(prompt).contains("BLUEPRINT_TEXT");
        assertThat(prompt).contains("Generate 2 original, high-quality question(s)");
        assertThat(prompt).contains("FEW-SHOT REFERENCE DEMONSTRATIONS");
        assertThat(prompt).contains("Sample Question Stem");
    }

    @Test
    @DisplayName("QuestionAuthorAgent includes RAG existing questions to avoid duplicates")
    void testQuestionAuthorAgentWithRagContext() {
        QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                .count(1)
                .build();

        SimilarityResult sim = new SimilarityResult() {
            @Override public UUID getId() { return UUID.randomUUID(); }
            @Override public String getContent() { return "Existing question about gravity"; }
            @Override public String getSubject() { return "Physics"; }
            @Override public Double getSimilarity() { return 0.88; }
        };

        String prompt = questionAuthorAgent.buildPromptWithBlueprint("BP", request, List.of(), List.of(sim));

        assertThat(prompt).contains("EXISTING QUESTIONS TO AVOID DUPLICATING");
        assertThat(prompt).contains("Existing question about gravity");
    }

    @Test
    @DisplayName("PsychometricCriticAgent identifies throwaways, improper delimiters, and single correct answer")
    void testPsychometricCriticReview() {
        List<QuestionOption> badOptions = List.of(
                QuestionOption.builder().id("A").text("Valid distractor").correct(false).build(),
                QuestionOption.builder().id("B").text("None of these").correct(false).build(),
                QuestionOption.builder().id("C").text("All of the above").correct(false).build(),
                QuestionOption.builder().id("D").text("Valid answer").correct(true).build()
        );

        CriticReviewResult result = psychometricCriticAgent.reviewQuestion(
                "Solve \\(x=2\\)", "D", "Short", badOptions, "SINGLE_MCQ");

        assertThat(result.isApproved()).isFalse();
        assertThat(result.getIssues()).anyMatch(i -> i.contains("throwaway"));
        assertThat(result.getIssues()).anyMatch(i -> i.contains("raw \\("));
    }

    @Test
    @DisplayName("RefinementAgent resolves throwaway distractors and normalizes LaTeX")
    void testRefinementAgentResolvesIssues() {
        List<QuestionOption> options = List.of(
                QuestionOption.builder().id("A").text("None of these").correct(false).build(),
                QuestionOption.builder().id("B").text("Answer \\(x = 5\\)").correct(true).build()
        );

        QuestionGenerationResponse.GeneratedQuestion candidate = QuestionGenerationResponse.GeneratedQuestion.builder()
                .content("Question \\(x\\)")
                .options(options)
                .answerKey("B")
                .explanation("Too short")
                .questionType("SINGLE_MCQ")
                .build();

        CriticReviewResult criticResult = CriticReviewResult.builder()
                .approved(false)
                .issues(List.of("Distractors contain throwaway", "Raw \\( delimiter"))
                .build();

        QuestionGenerationResponse.GeneratedQuestion refined = refinementAgent.refine(candidate, criticResult);

        assertThat(refined.getContent()).isEqualTo("Question $$x$$");
        assertThat(refined.getOptions().getFirst().getText()).isNotEqualTo("None of these");
        assertThat(refined.getExplanation()).hasSizeGreaterThanOrEqualTo(20);
        assertThat(refined.getCriticFeedback()).anyMatch(f -> f.contains("Refined by RefinementAgent"));
    }

    @Test
    @DisplayName("SimilarityAuditorAgent flags questions with cosine similarity >= 0.85")
    void testSimilarityAuditor() {
        UUID dupId = UUID.randomUUID();
        when(similarityDetectionService.checkSimilarity("Test question", "Physics", "tenant-1"))
                .thenReturn(new SimilarityCheckResult(
                        SimilarityCheckResult.Status.WARN,
                        List.of(new SimilarityCheckResult.SimilarQuestion(dupId, 0.90, "Similar question text"))
                ));

        SimilarityAuditorAgent.AuditResult audit = similarityAuditorAgent.auditUniqueness(
                "Test question", "Physics", "tenant-1");

        assertThat(audit.passed()).isFalse();
        assertThat(audit.conflictingQuestionId()).isEqualTo(dupId);
        assertThat(audit.topSimilarity()).isEqualTo(0.90);
    }
}
