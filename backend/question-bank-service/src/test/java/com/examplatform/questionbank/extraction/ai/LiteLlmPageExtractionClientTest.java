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
package com.examplatform.questionbank.extraction.ai;

import com.examplatform.questionbank.ai.generation.ModelRouter;
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.microlesson.ai.agent.DeterministicJsonRepairEngine;
import com.examplatform.shared.constants.QuestionTypeConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LiteLlmPageExtractionClient Unit Tests")
class LiteLlmPageExtractionClientTest {

    @Mock
    private ChatClient chatClient;

    private ModelRouter modelRouter;
    private QuestionPaperPdfParser pdfParser;
    private ObjectMapper objectMapper;
    private DeterministicJsonRepairEngine jsonRepairEngine;
    private DiagramCropper diagramCropper;
    private MockPageExtractionFixtures mockFixtures;
    private LiteLlmPageExtractionClient client;

    @BeforeEach
    void setUp() {
        modelRouter = new ModelRouter();
        pdfParser = new QuestionPaperPdfParser();
        objectMapper = new ObjectMapper();
        jsonRepairEngine = new DeterministicJsonRepairEngine();
        diagramCropper = new DiagramCropper();
        mockFixtures = new MockPageExtractionFixtures(objectMapper);

        // Instantiate with null chatClient for deterministic tests by default
        client = new LiteLlmPageExtractionClient(null, modelRouter, pdfParser, objectMapper, jsonRepairEngine);
    }

    @Test
    @DisplayName("Deterministic Cover Page extraction detects metadata")
    void testDeterministicCoverPageExtraction() {
        String coverText = """
                TEST BOOKLET - 2024
                DO NOT OPEN THIS TEST BOOKLET UNTIL YOU ARE ASKED TO DO SO
                Booklet Series: A
                Test Booklet No. TB-98721
                Paper Code: CS-2024
                Subject: Computer Science & Engineering
                Time Allowed: 3 Hours
                Maximum Marks: 300
                Total Questions: 100
                INSTRUCTIONS TO CANDIDATES:
                1. Immediately after commencement of exam, check that this booklet does not have missing pages.
                2. There will be negative marking for wrong answers: penalty of 0.25 marks per wrong answer.
                """;

        PdfPageContent pageContent = PdfPageContent.builder()
                .pageNumber(1)
                .totalPages(24)
                .digitalText(coverText)
                .parsingTier(PdfPageContent.TIER_0_LOCAL_TEXT)
                .build();

        LlmPageExtractionResult result = client.extractDeterministically(pageContent);

        assertThat(result).isNotNull();
        assertThat(result.isCoverPage()).isTrue();
        assertThat(result.getDetectedBookletSerialNo()).isEqualTo("TB-98721");
        assertThat(result.getDetectedExamYear()).isEqualTo(2024);
        assertThat(result.getDetectedDurationMinutes()).isEqualTo(180);
        assertThat(result.getDetectedMaxMarks()).isEqualTo(300.0);
        assertThat(result.getDetectedNegativeMarkingRatio()).isEqualTo(0.25);
        assertThat(result.getDetectedTotalQuestions()).isEqualTo(100);
    }

    @Test
    @DisplayName("Deterministic Question Extraction segments questions and options")
    void testDeterministicQuestionExtraction() {
        String questionPageText = """
                1. Which data structure guarantees O(1) time complexity for indexed random access?
                (A) Linked List
                (B) Array
                (C) Binary Tree
                (D) Hash Table without collisions
                Ans: (B)

                2. Find the root of the equation \\(x^2 - 4 = 0\\) for $x > 0$.
                (A) 1
                (B) 2
                (C) 3
                (D) 4
                Ans: (B)
                """;

        PdfPageContent pageContent = PdfPageContent.builder()
                .pageNumber(2)
                .totalPages(24)
                .digitalText(questionPageText)
                .parsingTier(PdfPageContent.TIER_0_LOCAL_TEXT)
                .build();

        LlmPageExtractionResult result = client.extractDeterministically(pageContent);

        assertThat(result).isNotNull();
        assertThat(result.isCoverPage()).isFalse();
        assertThat(result.getQuestions()).hasSize(2);

        // Verify Question 1
        LlmPageExtractionResult.ExtractedItemPayload q1 = result.getQuestions().get(0);
        assertThat(q1.getQuestionNumber()).isEqualTo(1);
        assertThat(q1.getQuestionType()).isEqualTo(QuestionTypeConstants.SINGLE_MCQ);
        assertThat(q1.getContent()).contains("Which data structure guarantees O(1)");
        assertThat(q1.getOptions()).hasSize(4);
        assertThat(q1.getAnswerKey()).isEqualTo("B");
        assertThat(q1.getOptions().stream().anyMatch(o -> o.getId().equals("B") && o.isCorrect())).isTrue();

        // Verify Question 2 (LaTeX normalization)
        LlmPageExtractionResult.ExtractedItemPayload q2 = result.getQuestions().get(1);
        assertThat(q2.getQuestionNumber()).isEqualTo(2);
        assertThat(q2.getContent()).contains("$$x^2 - 4 = 0$$");
        assertThat(q2.getContent()).contains("$$x > 0$$");
        assertThat(q2.isHasFormulas()).isTrue();
        assertThat(q2.getOptions()).hasSize(4);
    }

    @Test
    @DisplayName("extractPage falls back to deterministic parsing when chatClient is null")
    void testExtractPageNullClientFallback() {
        String text = """
                1. What is the SI unit of electric current?
                (A) Volt
                (B) Ampere
                (C) Ohm
                (D) Watt
                Ans: B
                """;

        PdfPageContent pageContent = PdfPageContent.builder()
                .pageNumber(3)
                .totalPages(10)
                .digitalText(text)
                .parsingTier(PdfPageContent.TIER_0_LOCAL_TEXT)
                .build();

        LlmPageExtractionResult result = client.extractPage(pageContent, "Physics Section");

        assertThat(result).isNotNull();
        assertThat(result.getQuestions()).hasSize(1);
        assertThat(result.getQuestions().getFirst().getContent()).contains("SI unit of electric current");
        assertThat(result.getQuestions().getFirst().getAnswerKey()).isEqualTo("B");
    }

    @Test
    @DisplayName("extractPage parses valid JSON from ChatClient when available")
    void testExtractPageWithMockedChatClient() {
        String llmJsonResponse = """
                ```json
                {
                  "coverPage": false,
                  "questions": [
                    {
                      "questionNumber": 5,
                      "questionType": "SINGLE_MCQ",
                      "content": "What is the momentum of a photon of wavelength \\\\(\\\\lambda\\\\)?",
                      "options": [
                        {"id": "A", "text": "$$h / \\\\lambda$$", "isCorrect": true},
                        {"id": "B", "text": "$$h \\\\lambda$$", "isCorrect": false},
                        {"id": "C", "text": "$$h / c$$", "isCorrect": false},
                        {"id": "D", "text": "$$h c / \\\\lambda$$", "isCorrect": false}
                      ],
                      "answerKey": "A",
                      "hasFormulas": true,
                      "suggestedDifficulty": "MEDIUM",
                      "suggestedCognitiveLevel": "APPLY",
                      "confidence": 0.98
                    }
                  ]
                }
                ```
                """;

        var requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        var responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(llmJsonResponse);

        LiteLlmPageExtractionClient onlineClient = new LiteLlmPageExtractionClient(
                chatClient, modelRouter, pdfParser, objectMapper, jsonRepairEngine
        );

        PdfPageContent pageContent = PdfPageContent.builder()
                .pageNumber(5)
                .totalPages(20)
                .digitalText("Digital text for question 5")
                .parsingTier(PdfPageContent.TIER_0_LOCAL_TEXT)
                .build();

        LlmPageExtractionResult result = onlineClient.extractPage(pageContent, "Physics");

        assertThat(result).isNotNull();
        assertThat(result.getQuestions()).hasSize(1);
        LlmPageExtractionResult.ExtractedItemPayload q = result.getQuestions().getFirst();
        assertThat(q.getQuestionNumber()).isEqualTo(5);
        assertThat(q.getContent()).contains("$$\\lambda$$");
        assertThat(q.getOptions()).hasSize(4);
        assertThat(q.getAnswerKey()).isEqualTo("A");
    }

    @Test
    @DisplayName("Mock Mode: zero LLM calls and returns bilingual classpath fixture with passage set")
    void testMockModeLoadsFixtureWithoutLlmCalls() {
        LiteLlmPageExtractionClient mockClient = new LiteLlmPageExtractionClient(
                chatClient, modelRouter, pdfParser, objectMapper, jsonRepairEngine,
                new LlmRetryPolicy(2, 10, 50), diagramCropper, mockFixtures
        );
        mockClient.setExtractionMode("mock");

        PdfPageContent pageContent = PdfPageContent.builder()
                .pageNumber(1)
                .totalPages(10)
                .digitalText("Raw scanned text that should be ignored in mock mode")
                .parsingTier(PdfPageContent.TIER_2_SCANNED_FALLBACK)
                .build();

        LlmPageExtractionResult result = mockClient.extractPage(pageContent, "UPSC Chemistry");

        verifyNoInteractions(chatClient);
        assertThat(result).isNotNull();
        assertThat(result.getQuestions()).hasSize(2);

        // Verify Question 1 passage range and bilingual content
        LlmPageExtractionResult.ExtractedItemPayload q1 = result.getQuestions().get(0);
        assertThat(q1.getQuestionNumber()).isEqualTo(1);
        assertThat(q1.getPassageRangeStart()).isEqualTo(1);
        assertThat(q1.getPassageRangeEnd()).isEqualTo(2);
        assertThat(q1.getPassageText()).contains("Directions for Questions 1 to 2");
        assertThat(q1.getContent()).contains("$$\\ce{2SO2(g) + O2(g) <=> 2SO3(g)}$$");
        assertThat(q1.getHindiContent()).contains("ऊष्माक्षेपी उत्क्रमणीय अभिक्रिया");
        assertThat(q1.getOptions()).hasSize(4);
        assertThat(q1.getHindiOptions()).hasSize(4);
        assertThat(q1.getAnswerKey()).isEqualTo("A");

        // Verify Question 2 units and calculations
        LlmPageExtractionResult.ExtractedItemPayload q2 = result.getQuestions().get(1);
        assertThat(q2.getQuestionNumber()).isEqualTo(2);
        assertThat(q2.getContent()).contains("\\pu{1.10 V}");
        assertThat(q2.getHindiContent()).contains("मानक गिब्स मुक्त ऊर्जा परिवर्तन");
        assertThat(q2.getAnswerKey()).isEqualTo("A");
    }

    @Test
    @DisplayName("Mock Mode: falls back to deterministic parsing when fixture is missing")
    void testMockModeFallsBackWhenFixtureNotFound() {
        MockPageExtractionFixtures emptyFixtures = mock(MockPageExtractionFixtures.class);
        when(emptyFixtures.loadFixture(99)).thenReturn(java.util.Optional.empty());

        LiteLlmPageExtractionClient mockClient = new LiteLlmPageExtractionClient(
                chatClient, modelRouter, pdfParser, objectMapper, jsonRepairEngine,
                new LlmRetryPolicy(2, 10, 50), diagramCropper, emptyFixtures
        );
        mockClient.setExtractionMode("mock");

        String text = """
                1. Which of the following is an inert gas?
                (A) Helium
                (B) Oxygen
                (C) Nitrogen
                (D) Hydrogen
                Ans: A
                """;

        PdfPageContent pageContent = PdfPageContent.builder()
                .pageNumber(99)
                .totalPages(100)
                .digitalText(text)
                .parsingTier(PdfPageContent.TIER_0_LOCAL_TEXT)
                .build();

        LlmPageExtractionResult result = mockClient.extractPage(pageContent, null);

        verifyNoInteractions(chatClient);
        assertThat(result).isNotNull();
        assertThat(result.getQuestions()).hasSize(1);
        assertThat(result.getQuestions().getFirst().getContent()).contains("inert gas");
    }

    @Test
    @DisplayName("Quota exhaustion on ChatClient after retries throws LlmQuotaExhaustedException")
    void testQuotaExhaustionPropagatesLlmQuotaExhaustedException() {
        var requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("HTTP 429 Too Many Requests - quota exhausted"));

        LlmRetryPolicy fastRetryPolicy = new LlmRetryPolicy(2, 10, 50);
        fastRetryPolicy.setSleeper(ms -> {}); // Non-blocking

        LiteLlmPageExtractionClient onlineClient = new LiteLlmPageExtractionClient(
                chatClient, modelRouter, pdfParser, objectMapper, jsonRepairEngine,
                fastRetryPolicy, diagramCropper, mockFixtures
        );

        PdfPageContent pageContent = PdfPageContent.builder()
                .pageNumber(4)
                .totalPages(20)
                .digitalText("Question text on page 4")
                .parsingTier(PdfPageContent.TIER_0_LOCAL_TEXT)
                .build();

        assertThatThrownBy(() -> onlineClient.extractPage(pageContent, "GATE Exam"))
                .isInstanceOf(LlmQuotaExhaustedException.class)
                .satisfies(ex -> {
                    LlmQuotaExhaustedException quotaEx = (LlmQuotaExhaustedException) ex;
                    assertThat(quotaEx.getPageNumber()).isEqualTo(4);
                });
    }

    @Test
    @DisplayName("Non-quota error falls back to deterministic parsing without throwing")
    void testNonQuotaErrorFallsBackToDeterministicParsing() {
        var requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("500 Internal Server Error: Gateway timeout"));

        LiteLlmPageExtractionClient onlineClient = new LiteLlmPageExtractionClient(
                chatClient, modelRouter, pdfParser, objectMapper, jsonRepairEngine,
                new LlmRetryPolicy(2, 10, 50), diagramCropper, mockFixtures
        );
        onlineClient.setDeterministicFallbackEnabled(true);

        String text = """
                1. What is the derivative of $$x^2$$ with respect to $$x$$?
                (A) $$2x$$
                (B) $$x$$
                (C) $$2$$
                (D) $$x^3$$
                Ans: A
                """;

        PdfPageContent pageContent = PdfPageContent.builder()
                .pageNumber(6)
                .totalPages(10)
                .digitalText(text)
                .parsingTier(PdfPageContent.TIER_0_LOCAL_TEXT)
                .build();

        LlmPageExtractionResult result = onlineClient.extractPage(pageContent, "Math Paper");

        assertThat(result).isNotNull();
        assertThat(result.getQuestions()).hasSize(1);
        assertThat(result.getQuestions().getFirst().getContent()).contains("derivative");
    }

    @Test
    @DisplayName("Bilingual Separation Guard moves Devanagari text from content to hindiContent when hindiContent is empty")
    void testBilingualSeparationGuardDevanagariToHindi() {
        var item = LlmPageExtractionResult.ExtractedItemPayload.builder()
                .questionNumber(1)
                .content("भारतीय संविधान की प्रस्तावना में किस संशोधन द्वारा 'समाजवादी' शब्द जोड़ा गया?")
                .hindiContent(null)
                .build();

        client.applyBilingualSeparationGuard(item);

        assertThat(item.getContent()).isNull();
        assertThat(item.getHindiContent()).contains("भारतीय संविधान");
    }

    @Test
    @DisplayName("Bilingual Separation Guard moves Latin text from hindiContent to content when content is empty")
    void testBilingualSeparationGuardLatinToContent() {
        var item = LlmPageExtractionResult.ExtractedItemPayload.builder()
                .questionNumber(2)
                .content(null)
                .hindiContent("Which constitutional amendment added the word 'Secular' to the Preamble?")
                .build();

        client.applyBilingualSeparationGuard(item);

        assertThat(item.getHindiContent()).isNull();
        assertThat(item.getContent()).contains("constitutional amendment");
    }

    @Test
    @DisplayName("Bilingual Separation Guard does not duplicate when both English and Hindi are present")
    void testBilingualSeparationGuardNeverDuplicates() {
        var item = LlmPageExtractionResult.ExtractedItemPayload.builder()
                .questionNumber(3)
                .content("Which article covers Equality before Law?")
                .hindiContent("विधि के समक्ष समता किस अनुच्छेद में वर्णित है?")
                .build();

        client.applyBilingualSeparationGuard(item);

        assertThat(item.getContent()).isEqualTo("Which article covers Equality before Law?");
        assertThat(item.getHindiContent()).isEqualTo("विधि के समक्ष समता किस अनुच्छेद में वर्णित है?");
    }

    @Test
    @DisplayName("Bilingual Separation Guard moves Devanagari options to hindiOptions when hindiOptions is empty")
    void testBilingualSeparationGuardOptionsSeparation() {
        var item = LlmPageExtractionResult.ExtractedItemPayload.builder()
                .questionNumber(4)
                .content("Question stem in English")
                .options(new ArrayList<>(List.of(
                        QuestionOption.builder().id("A").text("अनुच्छेद 14").build(),
                        QuestionOption.builder().id("B").text("अनुच्छेद 19").build()
                )))
                .hindiOptions(new ArrayList<>())
                .build();

        client.applyBilingualSeparationGuard(item);

        assertThat(item.getOptions()).isEmpty();
        assertThat(item.getHindiOptions()).hasSize(2);
        assertThat(item.getHindiOptions().getFirst().getText()).isEqualTo("अनुच्छेद 14");
    }

    @Test
    @DisplayName("Diagram cropping populates croppedDiagramDataUris from bounding boxes")
    void testDiagramCroppingIntegration() {
        BufferedImage syntheticImage = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = syntheticImage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 500, 500);
        g.setColor(Color.RED);
        g.fillRect(50, 50, 100, 100);
        g.dispose();

        String pageDataUri = diagramCropper.encodeToDataUri(syntheticImage);

        PdfPageContent pageContent = PdfPageContent.builder()
                .pageNumber(8)
                .totalPages(10)
                .renderedPageDataUri(pageDataUri)
                .scannedOrImageHeavy(true)
                .parsingTier(PdfPageContent.TIER_2_SCANNED_FALLBACK)
                .build();

        var item = LlmPageExtractionResult.ExtractedItemPayload.builder()
                .questionNumber(1)
                .content("Identify the circuit component shown below:")
                .diagramBoundingBoxes(List.of(List.of(0.10, 0.10, 0.30, 0.30)))
                .hasDiagram(false)
                .build();

        LlmPageExtractionResult result = LlmPageExtractionResult.builder()
                .questions(List.of(item))
                .build();

        client.postProcessResult(result, pageContent);

        assertThat(item.isHasDiagram()).isTrue();
        assertThat(item.getCroppedDiagramDataUris()).hasSize(1);
        assertThat(item.getCroppedDiagramDataUris().getFirst()).startsWith("data:image/jpeg;base64,");
    }
}
