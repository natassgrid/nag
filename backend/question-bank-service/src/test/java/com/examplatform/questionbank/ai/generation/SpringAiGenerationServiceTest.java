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
package com.examplatform.questionbank.ai.generation;

import com.examplatform.questionbank.ai.embedding.EmbeddingService;
import com.examplatform.questionbank.ai.similarity.SimilarityCheckResult;
import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.repository.SimilarityResult;
import com.examplatform.questionbank.service.SimilarityDetectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link SpringAiGenerationService}.
 * Tests the full generation flow with mocked external dependencies:
 * ChatClient, EmbeddingService, QuestionRepository, and SimilarityDetectionService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiGenerationService")
class SpringAiGenerationServiceTest {

    private static final String TENANT_ID = "tenant-001";

    @Mock
    private ChatClient chatClient;

    @Mock
    private ModelRouter modelRouter;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private SimilarityDetectionService similarityDetectionService;

    @Mock
    private QuestionRepository questionRepository;

    private SpringAiGenerationService generationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        generationService = new SpringAiGenerationService(
                chatClient, modelRouter, embeddingService,
                similarityDetectionService, questionRepository, objectMapper);
    }

    /**
     * Sets up the ChatClient fluent API mock chain so that any call through
     * chatClient.prompt().system(...).user(...).options(...).call().content()
     * returns the specified JSON response string.
     */
    private void mockChatClientResponse(String jsonResponse) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(jsonResponse);
    }

    /**
     * Mocks EmbeddingService to return a dummy 384-dimensional float array.
     */
    private void mockEmbeddingService() {
        float[] dummyEmbedding = new float[384];
        for (int i = 0; i < 384; i++) {
            dummyEmbedding[i] = 0.01f * i;
        }
        when(embeddingService.embed(anyString())).thenReturn(dummyEmbedding);
    }

    /**
     * Mocks QuestionRepository for RAG retrieval to return an empty list (no existing questions).
     */
    private void mockEmptyRagContext() {
        when(questionRepository.findTopSimilarQuestions(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of());
    }

    @Nested
    @DisplayName("Successful SINGLE_MCQ generation")
    class SuccessfulGeneration {

        @Test
        @DisplayName("generates valid SINGLE_MCQ questions when ChatClient returns correct JSON")
        void generate_validSingleMcq_returnsProcessedQuestions() {
            // Arrange
            String llmResponse = """
                    [
                      {
                        "content": "Solve: $$x^2 - 5x + 6 = 0$$",
                        "answerKey": "A",
                        "explanation": "Factoring: $$(x-2)(x-3) = 0$$",
                        "options": [
                          {"id": "A", "text": "$$x = 2, 3$$", "isCorrect": true},
                          {"id": "B", "text": "$$x = 1, 6$$", "isCorrect": false},
                          {"id": "C", "text": "$$x = -2, -3$$", "isCorrect": false},
                          {"id": "D", "text": "$$x = 2, -3$$", "isCorrect": false}
                        ],
                        "difficulty": "MEDIUM",
                        "cognitiveLevel": "APPLY",
                        "questionType": "SINGLE_MCQ"
                      }
                    ]
                    """;

            when(modelRouter.selectModel(any())).thenReturn("nova-lite");
            mockChatClientResponse(llmResponse);
            mockEmbeddingService();
            mockEmptyRagContext();
            when(similarityDetectionService.checkSimilarity(anyString(), anyString(), anyString()))
                    .thenReturn(SimilarityCheckResult.pass());

            QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                    .subject("Mathematics")
                    .topic("Quadratic Equations")
                    .difficulty("MEDIUM")
                    .cognitiveLevel("APPLY")
                    .questionType("SINGLE_MCQ")
                    .count(1)
                    .avoidDuplicate(true)
                    .autoSave(false)
                    .build();

            // Act
            QuestionGenerationResponse response = generationService.generate(request, TENANT_ID, java.util.UUID.randomUUID());

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getModelUsed()).isEqualTo("nova-lite");
            assertThat(response.getTotalGenerated()).isEqualTo(1);
            assertThat(response.getTotalValid()).isEqualTo(1);
            assertThat(response.getTotalDuplicates()).isEqualTo(0);
            assertThat(response.getQuestions()).hasSize(1);

            QuestionGenerationResponse.GeneratedQuestion question = response.getQuestions().getFirst();
            assertThat(question.getContent()).isEqualTo("Solve: $$x^2 - 5x + 6 = 0$$");
            assertThat(question.getAnswerKey()).isEqualTo("A");
            assertThat(question.getOptions()).hasSize(4);
            assertThat(question.getDifficulty()).isEqualTo("MEDIUM");
            assertThat(question.getCognitiveLevel()).isEqualTo("APPLY");
            assertThat(question.getQuestionType()).isEqualTo("SINGLE_MCQ");
            assertThat(question.getValidation().isValid()).isTrue();
            assertThat(question.getValidation().getErrors()).isEmpty();
            assertThat(question.getDuplicate()).isNull();
            assertThat(question.getSavedQuestionId()).isNull();
        }

        @Test
        @DisplayName("generates multiple questions and reports correct counts")
        void generate_multipleQuestions_reportsCorrectCounts() {
            // Arrange
            String llmResponse = """
                    [
                      {
                        "content": "What is 2+2?",
                        "answerKey": "B",
                        "explanation": "Basic addition",
                        "options": [
                          {"id": "A", "text": "3", "isCorrect": false},
                          {"id": "B", "text": "4", "isCorrect": true},
                          {"id": "C", "text": "5", "isCorrect": false},
                          {"id": "D", "text": "6", "isCorrect": false}
                        ],
                        "difficulty": "EASY",
                        "cognitiveLevel": "REMEMBER",
                        "questionType": "SINGLE_MCQ"
                      },
                      {
                        "content": "What is 3*3?",
                        "answerKey": "C",
                        "explanation": "Basic multiplication",
                        "options": [
                          {"id": "A", "text": "6", "isCorrect": false},
                          {"id": "B", "text": "8", "isCorrect": false},
                          {"id": "C", "text": "9", "isCorrect": true},
                          {"id": "D", "text": "12", "isCorrect": false}
                        ],
                        "difficulty": "EASY",
                        "cognitiveLevel": "REMEMBER",
                        "questionType": "SINGLE_MCQ"
                      }
                    ]
                    """;

            when(modelRouter.selectModel(any())).thenReturn("nova-lite");
            mockChatClientResponse(llmResponse);
            mockEmbeddingService();
            mockEmptyRagContext();
            when(similarityDetectionService.checkSimilarity(anyString(), anyString(), anyString()))
                    .thenReturn(SimilarityCheckResult.pass());

            QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                    .subject("Mathematics")
                    .topic("Arithmetic")
                    .difficulty("EASY")
                    .cognitiveLevel("REMEMBER")
                    .questionType("SINGLE_MCQ")
                    .count(2)
                    .avoidDuplicate(true)
                    .autoSave(false)
                    .build();

            // Act
            QuestionGenerationResponse response = generationService.generate(request, TENANT_ID, java.util.UUID.randomUUID());

            // Assert
            assertThat(response.getTotalGenerated()).isEqualTo(2);
            assertThat(response.getTotalValid()).isEqualTo(2);
            assertThat(response.getTotalDuplicates()).isEqualTo(0);
            assertThat(response.getQuestions()).hasSize(2);
        }

        @Test
        @DisplayName("handles LLM response wrapped in markdown code fences")
        void generate_markdownWrappedResponse_parsesCorrectly() {
            // Arrange
            String llmResponse = """
                    ```json
                    [
                      {
                        "content": "Define photosynthesis.",
                        "answerKey": "A",
                        "explanation": "Process by which plants convert sunlight to energy.",
                        "options": [
                          {"id": "A", "text": "Conversion of light to chemical energy", "isCorrect": true},
                          {"id": "B", "text": "Breakdown of glucose", "isCorrect": false},
                          {"id": "C", "text": "Absorption of water", "isCorrect": false},
                          {"id": "D", "text": "Release of carbon dioxide", "isCorrect": false}
                        ],
                        "difficulty": "EASY",
                        "cognitiveLevel": "REMEMBER",
                        "questionType": "SINGLE_MCQ"
                      }
                    ]
                    ```""";

            when(modelRouter.selectModel(any())).thenReturn("nova-lite");
            mockChatClientResponse(llmResponse);
            mockEmbeddingService();
            mockEmptyRagContext();
            when(similarityDetectionService.checkSimilarity(anyString(), anyString(), anyString()))
                    .thenReturn(SimilarityCheckResult.pass());

            QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                    .subject("General Science")
                    .topic("Biology")
                    .difficulty("EASY")
                    .cognitiveLevel("REMEMBER")
                    .questionType("SINGLE_MCQ")
                    .count(1)
                    .avoidDuplicate(true)
                    .autoSave(false)
                    .build();

            // Act
            QuestionGenerationResponse response = generationService.generate(request, TENANT_ID, java.util.UUID.randomUUID());

            // Assert
            assertThat(response.getTotalGenerated()).isEqualTo(1);
            assertThat(response.getTotalValid()).isEqualTo(1);
            assertThat(response.getQuestions().getFirst().getContent()).isEqualTo("Define photosynthesis.");
        }
    }

    @Nested
    @DisplayName("Model routing")
    class ModelRouting {

        @Test
        @DisplayName("selects math model for Mathematics subject")
        void generate_mathSubject_usesMathModel() {
            // Arrange
            when(modelRouter.selectModel(any())).thenReturn("nova-lite");
            mockChatClientResponse("[]");
            mockEmbeddingService();
            mockEmptyRagContext();

            QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                    .subject("Mathematics")
                    .topic("Algebra")
                    .difficulty("MEDIUM")
                    .cognitiveLevel("APPLY")
                    .questionType("SINGLE_MCQ")
                    .count(1)
                    .build();

            // Act
            QuestionGenerationResponse response = generationService.generate(request, TENANT_ID, java.util.UUID.randomUUID());

            // Assert
            assertThat(response.getModelUsed()).isEqualTo("nova-lite");
            verify(modelRouter).selectModel(any());
        }

        @Test
        @DisplayName("selects trivia model for Indian History subject")
        void generate_historySubject_usesTriviaModel() {
            // Arrange
            when(modelRouter.selectModel(any())).thenReturn("nova-micro");
            mockChatClientResponse("[]");
            mockEmbeddingService();
            mockEmptyRagContext();

            QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                    .subject("Indian History")
                    .topic("Mughal Empire")
                    .difficulty("MEDIUM")
                    .cognitiveLevel("REMEMBER")
                    .questionType("SINGLE_MCQ")
                    .count(1)
                    .build();

            // Act
            QuestionGenerationResponse response = generationService.generate(request, TENANT_ID, java.util.UUID.randomUUID());

            // Assert
            assertThat(response.getModelUsed()).isEqualTo("nova-micro");
            verify(modelRouter).selectModel(any());
        }

        @Test
        @DisplayName("selects general model for English subject")
        void generate_generalSubject_usesGeneralModel() {
            // Arrange
            when(modelRouter.selectModel(any())).thenReturn("nova-lite");
            mockChatClientResponse("[]");
            mockEmbeddingService();
            mockEmptyRagContext();

            QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                    .subject("English")
                    .topic("Grammar")
                    .difficulty("EASY")
                    .cognitiveLevel("UNDERSTAND")
                    .questionType("SINGLE_MCQ")
                    .count(1)
                    .build();

            // Act
            QuestionGenerationResponse response = generationService.generate(request, TENANT_ID, java.util.UUID.randomUUID());

            // Assert
            assertThat(response.getModelUsed()).isEqualTo("nova-lite");
            verify(modelRouter).selectModel(any());
        }
    }

    @Nested
    @DisplayName("Validation failure scenarios")
    class ValidationFailure {

        @Test
        @DisplayName("marks question invalid when content is missing")
        void generate_missingContent_validationFails() {
            // Arrange
            String llmResponse = """
                    [
                      {
                        "content": "",
                        "answerKey": "A",
                        "explanation": "Some explanation",
                        "options": [
                          {"id": "A", "text": "Option A", "isCorrect": true},
                          {"id": "B", "text": "Option B", "isCorrect": false},
                          {"id": "C", "text": "Option C", "isCorrect": false},
                          {"id": "D", "text": "Option D", "isCorrect": false}
                        ],
                        "difficulty": "EASY",
                        "cognitiveLevel": "REMEMBER",
                        "questionType": "SINGLE_MCQ"
                      }
                    ]
                    """;

            when(modelRouter.selectModel(any())).thenReturn("nova-lite");
            mockChatClientResponse(llmResponse);
            mockEmbeddingService();
            mockEmptyRagContext();

            QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                    .subject("Mathematics")
                    .topic("Algebra")
                    .difficulty("EASY")
                    .cognitiveLevel("REMEMBER")
                    .questionType("SINGLE_MCQ")
                    .count(1)
                    .avoidDuplicate(true)
                    .autoSave(false)
                    .build();

            // Act
            QuestionGenerationResponse response = generationService.generate(request, TENANT_ID, java.util.UUID.randomUUID());

            // Assert
            assertThat(response.getTotalGenerated()).isEqualTo(1);
            assertThat(response.getTotalValid()).isEqualTo(0);

            QuestionGenerationResponse.GeneratedQuestion question = response.getQuestions().getFirst();
            assertThat(question.getValidation().isValid()).isFalse();
            assertThat(question.getValidation().getErrors()).contains("Content is required");
        }

        @Test
        @DisplayName("marks question invalid when MCQ has wrong number of options")
        void generate_wrongOptionCount_validationFails() {
            // Arrange â€” only 2 options instead of required 4
            String llmResponse = """
                    [
                      {
                        "content": "What is 1+1?",
                        "answerKey": "A",
                        "explanation": "Basic addition",
                        "options": [
                          {"id": "A", "text": "2", "isCorrect": true},
                          {"id": "B", "text": "3", "isCorrect": false}
                        ],
                        "difficulty": "EASY",
                        "cognitiveLevel": "REMEMBER",
                        "questionType": "SINGLE_MCQ"
                      }
                    ]
                    """;

            when(modelRouter.selectModel(any())).thenReturn("nova-lite");
            mockChatClientResponse(llmResponse);
            mockEmbeddingService();
            mockEmptyRagContext();

            QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                    .subject("Mathematics")
                    .topic("Arithmetic")
                    .difficulty("EASY")
                    .cognitiveLevel("REMEMBER")
                    .questionType("SINGLE_MCQ")
                    .count(1)
                    .avoidDuplicate(true)
                    .autoSave(false)
                    .build();

            // Act
            QuestionGenerationResponse response = generationService.generate(request, TENANT_ID, java.util.UUID.randomUUID());

            // Assert
            QuestionGenerationResponse.GeneratedQuestion question = response.getQuestions().getFirst();
            assertThat(question.getValidation().isValid()).isFalse();
            assertThat(question.getValidation().getErrors())
                    .anyMatch(e -> e.contains("exactly 4 options"));
        }

        @Test
        @DisplayName("marks question invalid when SINGLE_MCQ has multiple correct options")
        void generate_multipleCorrectInSingleMcq_validationFails() {
            // Arrange â€” 2 correct options in SINGLE_MCQ
            String llmResponse = """
                    [
                      {
                        "content": "What is the capital of India?",
                        "answerKey": "A",
                        "explanation": "New Delhi is the capital",
                        "options": [
                          {"id": "A", "text": "New Delhi", "isCorrect": true},
                          {"id": "B", "text": "Mumbai", "isCorrect": true},
                          {"id": "C", "text": "Kolkata", "isCorrect": false},
                          {"id": "D", "text": "Chennai", "isCorrect": false}
                        ],
                        "difficulty": "EASY",
                        "cognitiveLevel": "REMEMBER",
                        "questionType": "SINGLE_MCQ"
                      }
                    ]
                    """;

            when(modelRouter.selectModel(any())).thenReturn("nova-micro");
            mockChatClientResponse(llmResponse);
            mockEmbeddingService();
            mockEmptyRagContext();

            QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                    .subject("Indian Geography")
                    .topic("Capitals")
                    .difficulty("EASY")
                    .cognitiveLevel("REMEMBER")
                    .questionType("SINGLE_MCQ")
                    .count(1)
                    .avoidDuplicate(true)
                    .autoSave(false)
                    .build();

            // Act
            QuestionGenerationResponse response = generationService.generate(request, TENANT_ID, java.util.UUID.randomUUID());

            // Assert
            QuestionGenerationResponse.GeneratedQuestion question = response.getQuestions().getFirst();
            assertThat(question.getValidation().isValid()).isFalse();
            assertThat(question.getValidation().getErrors())
                    .anyMatch(e -> e.contains("exactly 1 correct option"));
        }

        @Test
        @DisplayName("returns empty questions list when LLM returns unparseable JSON")
        void generate_unparseableResponse_returnsEmptyList() {
            // Arrange
            String llmResponse = "This is not valid JSON at all {{{";

            when(modelRouter.selectModel(any())).thenReturn("nova-lite");
            mockChatClientResponse(llmResponse);
            mockEmbeddingService();
            mockEmptyRagContext();

            QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                    .subject("Mathematics")
                    .topic("Algebra")
                    .difficulty("MEDIUM")
                    .cognitiveLevel("APPLY")
                    .questionType("SINGLE_MCQ")
                    .count(1)
                    .avoidDuplicate(false)
                    .autoSave(false)
                    .build();

            // Act
            QuestionGenerationResponse response = generationService.generate(request, TENANT_ID, java.util.UUID.randomUUID());

            // Assert
            assertThat(response.getTotalGenerated()).isEqualTo(0);
            assertThat(response.getQuestions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Duplicate detection")
    class DuplicateDetection {

        @Test
        @DisplayName("flags question as duplicate when similarity exceeds 0.92 threshold")
        void generate_duplicateDetected_flagsQuestion() {
            // Arrange
            UUID existingQuestionId = UUID.randomUUID();
            String llmResponse = """
                    [
                      {
                        "content": "Solve: $$x^2 - 5x + 6 = 0$$",
                        "answerKey": "A",
                        "explanation": "Factoring the quadratic",
                        "options": [
                          {"id": "A", "text": "$$x = 2, 3$$", "isCorrect": true},
                          {"id": "B", "text": "$$x = 1, 6$$", "isCorrect": false},
                          {"id": "C", "text": "$$x = -2, -3$$", "isCorrect": false},
                          {"id": "D", "text": "$$x = 2, -3$$", "isCorrect": false}
                        ],
                        "difficulty": "MEDIUM",
                        "cognitiveLevel": "APPLY",
                        "questionType": "SINGLE_MCQ"
                      }
                    ]
                    """;

            when(modelRouter.selectModel(any())).thenReturn("nova-lite");
            mockChatClientResponse(llmResponse);
            mockEmbeddingService();
            mockEmptyRagContext();

            // Simulate duplicate detection returning REJECT with a similar question
            SimilarityCheckResult rejectResult = new SimilarityCheckResult(
                    SimilarityCheckResult.Status.REJECT,
                    List.of(new SimilarityCheckResult.SimilarQuestion(
                            existingQuestionId, 0.95, "Solve: $$x^2 - 5x + 6 = 0$$"))
            );
            when(similarityDetectionService.checkSimilarity(anyString(), eq("Mathematics"), eq(TENANT_ID)))
                    .thenReturn(rejectResult);

            QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                    .subject("Mathematics")
                    .topic("Quadratic Equations")
                    .difficulty("MEDIUM")
                    .cognitiveLevel("APPLY")
                    .questionType("SINGLE_MCQ")
                    .count(1)
                    .avoidDuplicate(true)
                    .autoSave(false)
                    .build();

            // Act
            QuestionGenerationResponse response = generationService.generate(request, TENANT_ID, java.util.UUID.randomUUID());

            // Assert
            assertThat(response.getTotalDuplicates()).isEqualTo(1);
            assertThat(response.getTotalValid()).isEqualTo(1); // still valid schema-wise

            QuestionGenerationResponse.GeneratedQuestion question = response.getQuestions().getFirst();
            assertThat(question.getDuplicate()).isNotNull();
            assertThat(question.getDuplicate().getSimilarQuestionId()).isEqualTo(existingQuestionId);
            assertThat(question.getDuplicate().getSimilarity()).isEqualTo(0.95);
            assertThat(question.getSavedQuestionId()).isNull(); // not saved because duplicate
        }

        @Test
        @DisplayName("skips duplicate detection when avoidDuplicate is false")
        void generate_avoidDuplicateFalse_skipsDuplicateCheck() {
            // Arrange
            String llmResponse = """
                    [
                      {
                        "content": "What is gravity?",
                        "answerKey": "A",
                        "explanation": "Gravitational force explanation",
                        "options": [
                          {"id": "A", "text": "A fundamental force", "isCorrect": true},
                          {"id": "B", "text": "A type of energy", "isCorrect": false},
                          {"id": "C", "text": "A chemical reaction", "isCorrect": false},
                          {"id": "D", "text": "A magnetic field", "isCorrect": false}
                        ],
                        "difficulty": "EASY",
                        "cognitiveLevel": "REMEMBER",
                        "questionType": "SINGLE_MCQ"
                      }
                    ]
                    """;

            when(modelRouter.selectModel(any())).thenReturn("nova-lite");
            mockChatClientResponse(llmResponse);
            mockEmbeddingService();
            mockEmptyRagContext();

            QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                    .subject("Physics")
                    .topic("Mechanics")
                    .difficulty("EASY")
                    .cognitiveLevel("REMEMBER")
                    .questionType("SINGLE_MCQ")
                    .count(1)
                    .avoidDuplicate(false)
                    .autoSave(false)
                    .build();

            // Act
            QuestionGenerationResponse response = generationService.generate(request, TENANT_ID, java.util.UUID.randomUUID());

            // Assert
            assertThat(response.getTotalDuplicates()).isEqualTo(0);
            assertThat(response.getQuestions().getFirst().getDuplicate()).isNull();
            verify(similarityDetectionService, never()).checkSimilarity(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Auto-save mode")
    class AutoSaveMode {

        @Test
        @DisplayName("persists valid non-duplicate questions as DRAFT when autoSave is true")
        void generate_autoSaveEnabled_persistsAsDraft() {
            // Arrange
            UUID savedId = UUID.randomUUID();
            String llmResponse = """
                    [
                      {
                        "content": "What is Newton's first law?",
                        "answerKey": "B",
                        "explanation": "An object at rest stays at rest",
                        "options": [
                          {"id": "A", "text": "F=ma", "isCorrect": false},
                          {"id": "B", "text": "Inertia", "isCorrect": true},
                          {"id": "C", "text": "Action-reaction", "isCorrect": false},
                          {"id": "D", "text": "Gravity", "isCorrect": false}
                        ],
                        "difficulty": "MEDIUM",
                        "cognitiveLevel": "UNDERSTAND",
                        "questionType": "SINGLE_MCQ"
                      }
                    ]
                    """;

            when(modelRouter.selectModel(any())).thenReturn("nova-lite");
            mockChatClientResponse(llmResponse);
            mockEmbeddingService();
            mockEmptyRagContext();
            when(similarityDetectionService.checkSimilarity(anyString(), eq("Physics"), eq(TENANT_ID)))
                    .thenReturn(SimilarityCheckResult.pass());

            // Mock the repository save to return a question with an ID
            Question savedQuestion = Question.builder()
                    .subject("Physics")
                    .topic("Mechanics")
                    .state("DRAFT")
                    .build();
            savedQuestion.setTenantId(TENANT_ID);
            // Use reflection-like approach via the setter on BaseEntity
            try {
                var setIdMethod = savedQuestion.getClass().getSuperclass().getDeclaredMethod("setId", UUID.class);
                setIdMethod.setAccessible(true);
                setIdMethod.invoke(savedQuestion, savedId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set ID on mock question", e);
            }
            when(questionRepository.save(any(Question.class))).thenReturn(savedQuestion);

            QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                    .subject("Physics")
                    .topic("Mechanics")
                    .difficulty("MEDIUM")
                    .cognitiveLevel("UNDERSTAND")
                    .questionType("SINGLE_MCQ")
                    .count(1)
                    .avoidDuplicate(true)
                    .autoSave(true)
                    .build();

            // Act
            QuestionGenerationResponse response = generationService.generate(request, TENANT_ID, java.util.UUID.randomUUID());

            // Assert
            assertThat(response.getTotalValid()).isEqualTo(1);
            assertThat(response.getQuestions().getFirst().getSavedQuestionId()).isEqualTo(savedId);
            verify(questionRepository).save(any(Question.class));
        }

        @Test
        @DisplayName("does not persist questions when autoSave is false")
        void generate_autoSaveDisabled_doesNotPersist() {
            // Arrange
            String llmResponse = """
                    [
                      {
                        "content": "Name the capital of France.",
                        "answerKey": "C",
                        "explanation": "Paris is the capital of France",
                        "options": [
                          {"id": "A", "text": "London", "isCorrect": false},
                          {"id": "B", "text": "Berlin", "isCorrect": false},
                          {"id": "C", "text": "Paris", "isCorrect": true},
                          {"id": "D", "text": "Madrid", "isCorrect": false}
                        ],
                        "difficulty": "EASY",
                        "cognitiveLevel": "REMEMBER",
                        "questionType": "SINGLE_MCQ"
                      }
                    ]
                    """;

            when(modelRouter.selectModel(any())).thenReturn("nova-lite");
            mockChatClientResponse(llmResponse);
            mockEmbeddingService();
            mockEmptyRagContext();
            when(similarityDetectionService.checkSimilarity(anyString(), eq("English"), eq(TENANT_ID)))
                    .thenReturn(SimilarityCheckResult.pass());

            QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                    .subject("English")
                    .topic("General Knowledge")
                    .difficulty("EASY")
                    .cognitiveLevel("REMEMBER")
                    .questionType("SINGLE_MCQ")
                    .count(1)
                    .avoidDuplicate(true)
                    .autoSave(false)
                    .build();

            // Act
            QuestionGenerationResponse response = generationService.generate(request, TENANT_ID, java.util.UUID.randomUUID());

            // Assert
            assertThat(response.getTotalValid()).isEqualTo(1);
            assertThat(response.getQuestions().getFirst().getSavedQuestionId()).isNull();
            verify(questionRepository, never()).save(any(Question.class));
        }

        @Test
        @DisplayName("does not persist duplicate questions even with autoSave enabled")
        void generate_autoSaveWithDuplicate_doesNotPersist() {
            // Arrange
            UUID existingId = UUID.randomUUID();
            String llmResponse = """
                    [
                      {
                        "content": "Duplicate question content",
                        "answerKey": "A",
                        "explanation": "Explanation",
                        "options": [
                          {"id": "A", "text": "Option A", "isCorrect": true},
                          {"id": "B", "text": "Option B", "isCorrect": false},
                          {"id": "C", "text": "Option C", "isCorrect": false},
                          {"id": "D", "text": "Option D", "isCorrect": false}
                        ],
                        "difficulty": "EASY",
                        "cognitiveLevel": "REMEMBER",
                        "questionType": "SINGLE_MCQ"
                      }
                    ]
                    """;

            when(modelRouter.selectModel(any())).thenReturn("nova-lite");
            mockChatClientResponse(llmResponse);
            mockEmbeddingService();
            mockEmptyRagContext();

            SimilarityCheckResult rejectResult = new SimilarityCheckResult(
                    SimilarityCheckResult.Status.REJECT,
                    List.of(new SimilarityCheckResult.SimilarQuestion(existingId, 0.96, "Similar content"))
            );
            when(similarityDetectionService.checkSimilarity(anyString(), eq("Mathematics"), eq(TENANT_ID)))
                    .thenReturn(rejectResult);

            QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                    .subject("Mathematics")
                    .topic("Algebra")
                    .difficulty("EASY")
                    .cognitiveLevel("REMEMBER")
                    .questionType("SINGLE_MCQ")
                    .count(1)
                    .avoidDuplicate(true)
                    .autoSave(true)
                    .build();

            // Act
            QuestionGenerationResponse response = generationService.generate(request, TENANT_ID, java.util.UUID.randomUUID());

            // Assert
            assertThat(response.getTotalDuplicates()).isEqualTo(1);
            assertThat(response.getQuestions().getFirst().getSavedQuestionId()).isNull();
            verify(questionRepository, never()).save(any(Question.class));
        }
    }

    @Nested
    @DisplayName("RAG context retrieval")
    class RagContextRetrieval {

        @Test
        @DisplayName("retrieves RAG context from repository for prompt enrichment")
        void generate_withExistingQuestions_retrievesRagContext() {
            // Arrange
            SimilarityResult ragResult = mock(SimilarityResult.class);
            when(ragResult.getContent()).thenReturn("Existing question about quadratics");
            when(questionRepository.findTopSimilarQuestions(anyString(), eq("Mathematics"), eq(TENANT_ID), eq(5)))
                    .thenReturn(List.of(ragResult));

            String llmResponse = """
                    [
                      {
                        "content": "Find roots of $$x^2 + 4x + 4 = 0$$",
                        "answerKey": "A",
                        "explanation": "Perfect square",
                        "options": [
                          {"id": "A", "text": "$$x = -2$$", "isCorrect": true},
                          {"id": "B", "text": "$$x = 2$$", "isCorrect": false},
                          {"id": "C", "text": "$$x = -4$$", "isCorrect": false},
                          {"id": "D", "text": "$$x = 4$$", "isCorrect": false}
                        ],
                        "difficulty": "MEDIUM",
                        "cognitiveLevel": "APPLY",
                        "questionType": "SINGLE_MCQ"
                      }
                    ]
                    """;

            when(modelRouter.selectModel(any())).thenReturn("nova-lite");
            mockChatClientResponse(llmResponse);
            mockEmbeddingService();
            when(similarityDetectionService.checkSimilarity(anyString(), anyString(), anyString()))
                    .thenReturn(SimilarityCheckResult.pass());

            QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                    .subject("Mathematics")
                    .topic("Quadratic Equations")
                    .difficulty("MEDIUM")
                    .cognitiveLevel("APPLY")
                    .questionType("SINGLE_MCQ")
                    .count(1)
                    .avoidDuplicate(true)
                    .autoSave(false)
                    .build();

            // Act
            QuestionGenerationResponse response = generationService.generate(request, TENANT_ID, java.util.UUID.randomUUID());

            // Assert
            assertThat(response.getTotalGenerated()).isEqualTo(1);
            verify(questionRepository).findTopSimilarQuestions(anyString(), eq("Mathematics"), eq(TENANT_ID), eq(5));
            verify(embeddingService).embed("Quadratic Equations");
        }
    }
}
