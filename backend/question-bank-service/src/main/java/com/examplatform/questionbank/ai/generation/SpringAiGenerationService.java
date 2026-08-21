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
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.repository.SimilarityResult;
import com.examplatform.questionbank.service.SimilarityDetectionService;
import com.examplatform.questionbank.util.EmbeddingUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Spring AI-based implementation of the question generation pipeline.
 *
 * <p>Orchestrates:
 * <ol>
 *   <li>Model selection via {@link ModelRouter} (subject-based routing)</li>
 *   <li>RAG context retrieval (top-5 similar existing questions via pgvector)</li>
 *   <li>Prompt construction with generation parameters and RAG context</li>
 *   <li>LLM invocation via Spring AI {@link ChatClient} → LiteLLM gateway</li>
 *   <li>Structured JSON response parsing into question DTOs</li>
 *   <li>Schema + answer validation for each generated question</li>
 *   <li>Duplicate detection via {@link SimilarityDetectionService}</li>
 *   <li>Optional auto-save of valid, non-duplicate questions as DRAFT</li>
 * </ol>
 *
 * @see QuestionGenerationService
 * @see ModelRouter
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpringAiGenerationService implements QuestionGenerationService {

    private static final int RAG_TOP_K = 5;
    private static final double GENERATION_TEMPERATURE = 0.7;
    private static final double DUPLICATE_REJECT_THRESHOLD = 0.92;

    private static final Set<String> MCQ_TYPES = Set.of("SINGLE_MCQ", "MULTI_MCQ");
    private static final Set<String> VALID_DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");
    private static final Set<String> VALID_COGNITIVE_LEVELS = Set.of(
            "REMEMBER", "UNDERSTAND", "APPLY", "ANALYZE", "EVALUATE", "CREATE");
    private static final Set<String> VALID_QUESTION_TYPES = Set.of(
            "SINGLE_MCQ", "MULTI_MCQ", "NUMERICAL", "DESCRIPTIVE");

    private final ChatClient chatClient;
    private final ModelRouter modelRouter;
    private final EmbeddingService embeddingService;
    private final SimilarityDetectionService similarityDetectionService;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public QuestionGenerationResponse generate(QuestionGenerationRequest request, String tenantId, java.util.UUID authorId) {
        log.info("Starting question generation: subject={}, topic={}, count={}, model selection pending",
                request.getSubject(), request.getTopic(), request.getCount());

        // Step 1: Select model via ModelRouter
        String modelName = modelRouter.selectModel(request.getSubject());
        log.debug("Selected model: {} for subject: {}", modelName, request.getSubject());

        // Step 2: RAG — retrieve top-K similar existing questions for context
        List<SimilarityResult> ragContext = retrieveRagContext(request, tenantId);
        log.debug("RAG context retrieved: {} questions", ragContext.size());

        // Step 3: Build prompt
        String systemPrompt = buildSystemPrompt(request);
        String userPrompt = buildUserPrompt(request, ragContext);

        // Step 4: Call LLM via ChatClient with selected model
        String llmResponse = callLlm(modelName, systemPrompt, userPrompt);
        log.debug("LLM response received (length={})", llmResponse != null ? llmResponse.length() : 0);

        // Step 5: Parse JSON response into question DTOs
        List<RawGeneratedQuestion> rawQuestions = parseLlmResponse(llmResponse);
        int totalGenerated = rawQuestions.size();
        log.info("Parsed {} questions from LLM response", totalGenerated);

        // Step 6 & 7: Validate and check duplicates for each question
        List<QuestionGenerationResponse.GeneratedQuestion> processedQuestions = new ArrayList<>();
        int totalValid = 0;
        int totalDuplicates = 0;

        for (RawGeneratedQuestion raw : rawQuestions) {
            // Validate schema + answer
            QuestionGenerationResponse.ValidationResult validation = validateQuestion(raw, request.getQuestionType());

            // Duplicate detection
            QuestionGenerationResponse.DuplicateResult duplicateResult = null;
            if (validation.isValid() && request.isAvoidDuplicate()) {
                duplicateResult = checkDuplicate(raw.content, request.getSubject(), tenantId);
                if (duplicateResult != null) {
                    totalDuplicates++;
                }
            }

            if (validation.isValid()) {
                totalValid++;
            }

            // Auto-save if valid, not duplicate, and autoSave enabled
            UUID savedId = null;
            if (request.isAutoSave() && validation.isValid() && duplicateResult == null) {
                savedId = persistAsDraft(raw, request, tenantId, authorId);
            }

            processedQuestions.add(QuestionGenerationResponse.GeneratedQuestion.builder()
                    .content(raw.content)
                    .answerKey(raw.answerKey)
                    .explanation(raw.explanation)
                    .options(raw.options)
                    .difficulty(raw.difficulty != null ? raw.difficulty : request.getDifficulty())
                    .cognitiveLevel(raw.cognitiveLevel != null ? raw.cognitiveLevel : request.getCognitiveLevel())
                    .questionType(raw.questionType != null ? raw.questionType : request.getQuestionType())
                    .validation(validation)
                    .duplicate(duplicateResult)
                    .savedQuestionId(savedId)
                    .build());
        }

        log.info("Generation complete: total={}, valid={}, duplicates={}, model={}",
                totalGenerated, totalValid, totalDuplicates, modelName);

        return QuestionGenerationResponse.builder()
                .questions(processedQuestions)
                .modelUsed(modelName)
                .totalGenerated(totalGenerated)
                .totalValid(totalValid)
                .totalDuplicates(totalDuplicates)
                .build();
    }

    /**
     * Retrieves top-K similar existing questions for RAG context.
     * Generates an embedding of the topic/subtopic to query pgvector.
     */
    private List<SimilarityResult> retrieveRagContext(QuestionGenerationRequest request, String tenantId) {
        try {
            String queryText = request.getTopic()
                    + (request.getSubtopic() != null ? " " + request.getSubtopic() : "");
            float[] queryEmbedding = embeddingService.embed(queryText);
            String embeddingStr = EmbeddingUtils.embeddingToString(queryEmbedding);

            return questionRepository.findTopSimilarQuestions(
                    embeddingStr, request.getSubject(), tenantId, RAG_TOP_K);
        } catch (Exception e) {
            log.warn("Failed to retrieve RAG context, proceeding without it: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Builds the system prompt instructing the LLM on output format and constraints.
     */
    private String buildSystemPrompt(QuestionGenerationRequest request) {
        return """
                You are an expert examination question generator for Indian competitive examinations.
                You generate high-quality questions in structured JSON format.
                
                Rules:
                - Generate questions strictly matching the specified type, difficulty, and cognitive level.
                - Content may include plain text, LaTeX math ($$...$$), and inline SVG diagrams.
                - For MCQ (SINGLE_MCQ): exactly 4 options with ids A, B, C, D. Set "isCorrect": true on EXACTLY ONE option and "isCorrect": false on the other three. The "answerKey" must be the id (A/B/C/D) of the correct option.
                - For MSQ (MULTI_MCQ): exactly 4 options (A, B, C, D), 2 or more correct.
                - For NUMERICAL: no options, answerKey is the numeric value.
                - For DESCRIPTIVE: no options, answerKey contains the model answer.
                - Always provide a clear explanation for the correct answer.
                - Do NOT repeat questions from the provided context — generate novel questions.
                
                Output ONLY a JSON array of question objects. No markdown, no explanation outside JSON.
                Each question object must have these fields:
                {
                  "content": "question text (may include $$LaTeX$$ or <svg>)",
                  "answerKey": "correct answer key or value",
                  "explanation": "explanation of the correct answer",
                  "options": [{"id": "A", "text": "option text", "isCorrect": true}, {"id": "B", "text": "option text", "isCorrect": false}, {"id": "C", "text": "option text", "isCorrect": false}, {"id": "D", "text": "option text", "isCorrect": false}],
                  "difficulty": "EASY|MEDIUM|HARD",
                  "cognitiveLevel": "REMEMBER|UNDERSTAND|APPLY|ANALYZE|EVALUATE|CREATE",
                  "questionType": "%s"
                }
                """.formatted(request.getQuestionType());
    }

    /**
     * Builds the user prompt with generation parameters and RAG context.
     */
    private String buildUserPrompt(QuestionGenerationRequest request, List<SimilarityResult> ragContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate ").append(request.getCount()).append(" question(s) with these parameters:\n");
        prompt.append("- Subject: ").append(request.getSubject()).append("\n");
        prompt.append("- Topic: ").append(request.getTopic()).append("\n");
        if (request.getSubtopic() != null && !request.getSubtopic().isBlank()) {
            prompt.append("- Subtopic: ").append(request.getSubtopic()).append("\n");
        }
        prompt.append("- Difficulty: ").append(request.getDifficulty()).append("\n");
        prompt.append("- Cognitive Level: ").append(request.getCognitiveLevel()).append("\n");
        prompt.append("- Question Type: ").append(request.getQuestionType()).append("\n");

        if (!ragContext.isEmpty()) {
            prompt.append("\nHere are existing questions on this topic for reference (do NOT duplicate them):\n");
            for (int i = 0; i < ragContext.size(); i++) {
                SimilarityResult ctx = ragContext.get(i);
                prompt.append(i + 1).append(". ").append(ctx.getContent()).append("\n");
            }
        }

        prompt.append("\nGenerate the questions now as a JSON array:");
        return prompt.toString();
    }

    /**
     * Calls the LLM via Spring AI ChatClient with the selected model.
     */
    private String callLlm(String modelName, String systemPrompt, String userPrompt) {
        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .options(ChatOptions.builder()
                            .model(modelName)
                            .temperature(GENERATION_TEMPERATURE))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("LLM call failed for model={}: {}", modelName, e.getMessage(), e);
            throw new QuestionGenerationException("LLM call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the LLM JSON response into raw question DTOs.
     * Handles potential markdown code fences around JSON.
     */
    private List<RawGeneratedQuestion> parseLlmResponse(String response) {
        if (response == null || response.isBlank()) {
            log.warn("Empty LLM response received");
            return List.of();
        }

        // Strip markdown code fences if present
        String json = response.strip();
        if (json.startsWith("```json")) {
            json = json.substring(7);
        } else if (json.startsWith("```")) {
            json = json.substring(3);
        }
        if (json.endsWith("```")) {
            json = json.substring(0, json.length() - 3);
        }
        json = json.strip();

        // Handle single object vs array
        if (json.startsWith("{")) {
            json = "[" + json + "]";
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<RawGeneratedQuestion>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM response as JSON: {}", e.getMessage());
            log.debug("Raw response: {}", response);
            return List.of();
        }
    }

    /**
     * Validates a generated question for schema correctness and answer validity.
     */
    private QuestionGenerationResponse.ValidationResult validateQuestion(
            RawGeneratedQuestion question, String expectedType) {
        List<String> errors = new ArrayList<>();

        // Required fields check
        if (question.content == null || question.content.isBlank()) {
            errors.add("Content is required");
        }
        if (question.answerKey == null || question.answerKey.isBlank()) {
            errors.add("Answer key is required");
        }

        // Validate question type
        String questionType = question.questionType != null ? question.questionType : expectedType;
        if (questionType != null && !VALID_QUESTION_TYPES.contains(questionType.toUpperCase())) {
            errors.add("Invalid question type: " + questionType);
        }

        // Validate difficulty
        if (question.difficulty != null && !VALID_DIFFICULTIES.contains(question.difficulty.toUpperCase())) {
            errors.add("Invalid difficulty: " + question.difficulty);
        }

        // Validate cognitive level
        if (question.cognitiveLevel != null
                && !VALID_COGNITIVE_LEVELS.contains(question.cognitiveLevel.toUpperCase())) {
            errors.add("Invalid cognitive level: " + question.cognitiveLevel);
        }

        // MCQ-specific validations
        if (MCQ_TYPES.contains(expectedType.toUpperCase())) {
            if (question.options == null || question.options.isEmpty()) {
                errors.add("Options are required for " + expectedType);
            } else {
                // Must have exactly 4 options
                if (question.options.size() != 4) {
                    errors.add("MCQ must have exactly 4 options, got " + question.options.size());
                }

                // Check answer key exists in options
                long correctCount = question.options.stream()
                        .filter(QuestionOption::isCorrect)
                        .count();

                // Auto-fix: if no option has isCorrect=true, try to derive from answerKey.
                // LLMs often forget isCorrect or use numeric indices (1,2,3,4) instead of A,B,C,D.
                if (correctCount == 0 && question.answerKey != null && !question.answerKey.isBlank()) {
                    String key = question.answerKey.trim();
                    // Try matching by option ID (A/B/C/D)
                    for (QuestionOption opt : question.options) {
                        if (opt.getId() != null && opt.getId().equalsIgnoreCase(key)) {
                            opt.setCorrect(true);
                            correctCount = 1;
                            break;
                        }
                    }
                    // Try matching by numeric index (1-based: 1=A, 2=B, 3=C, 4=D)
                    if (correctCount == 0 && key.matches("\\d+")) {
                        int idx = Integer.parseInt(key) - 1;
                        if (idx >= 0 && idx < question.options.size()) {
                            question.options.get(idx).setCorrect(true);
                            correctCount = 1;
                        }
                    }
                }

                if ("SINGLE_MCQ".equalsIgnoreCase(expectedType)) {
                    if (correctCount != 1) {
                        errors.add("SINGLE_MCQ must have exactly 1 correct option, got " + correctCount);
                    }
                } else if ("MULTI_MCQ".equalsIgnoreCase(expectedType)) {
                    if (correctCount < 2) {
                        errors.add("MULTI_MCQ must have at least 2 correct options, got " + correctCount);
                    }
                }

                // Note: answerKey mismatch is NOT a validation failure for MCQs.
                // LLMs often put the textual answer instead of the option letter (A/B/C/D).
                // The isCorrect flag on options is the source of truth for MCQs.
            }
        }

        return QuestionGenerationResponse.ValidationResult.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .build();
    }

    /**
     * Checks if a generated question is a duplicate of existing questions.
     * Returns a DuplicateResult if similarity > 0.92, null otherwise.
     */
    private QuestionGenerationResponse.DuplicateResult checkDuplicate(
            String content, String subject, String tenantId) {
        try {
            SimilarityCheckResult result = similarityDetectionService.checkSimilarity(
                    content, subject, tenantId);

            if (result.status() == SimilarityCheckResult.Status.REJECT
                    && !result.similarQuestions().isEmpty()) {
                SimilarityCheckResult.SimilarQuestion topMatch = result.similarQuestions().getFirst();
                return QuestionGenerationResponse.DuplicateResult.builder()
                        .similarQuestionId(topMatch.questionId())
                        .similarity(topMatch.similarity())
                        .build();
            }
        } catch (Exception e) {
            log.warn("Duplicate detection failed for content, skipping: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Persists a valid, non-duplicate generated question as DRAFT.
     */
    private UUID persistAsDraft(RawGeneratedQuestion raw, QuestionGenerationRequest request, String tenantId, UUID authorId) {
        try {
            Question question = Question.builder()
                    .subject(request.getSubject())
                    .topic(request.getTopic())
                    .subtopic(request.getSubtopic())
                    .difficulty(raw.difficulty != null ? raw.difficulty : request.getDifficulty())
                    .cognitiveLevel(raw.cognitiveLevel != null ? raw.cognitiveLevel : request.getCognitiveLevel())
                    .questionType(raw.questionType != null ? raw.questionType : request.getQuestionType())
                    .content(raw.content)
                    .answerKey(raw.answerKey)
                    .explanation(raw.explanation)
                    .options(raw.options)
                    .references("AI-generated via " + modelRouter.selectModel(request.getSubject()))
                    .state("DRAFT")
                    .authorId(authorId)
                    .build();
            question.setTenantId(tenantId);

            Question saved = questionRepository.save(question);

            // Generate and store embedding via native query (halfvec cast)
            // The embedding column is insertable=false/updatable=false so JPA setEmbedding won't persist.
            // NFR-2: If embedding service is unavailable, question is still saved without embedding.
            try {
                float[] embedding = embeddingService.embed(raw.content);
                if (embedding != null && embedding.length > 0) {
                    questionRepository.updateEmbedding(saved.getId(), EmbeddingUtils.embeddingToString(embedding));
                    log.debug("Embedding generated for auto-saved question: id={}", saved.getId());
                }
            } catch (Exception e) {
                log.warn("Failed to generate embedding for auto-saved question id={}. Reason: {}",
                        saved.getId(), e.getMessage());
            }

            log.debug("Auto-saved generated question as DRAFT: id={}", saved.getId());
            return saved.getId();
        } catch (Exception e) {
            log.error("Failed to auto-save generated question: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Internal DTO for parsing raw LLM JSON output before validation.
     */
    private record RawGeneratedQuestion(
            String content,
            String answerKey,
            String explanation,
            List<QuestionOption> options,
            String difficulty,
            String cognitiveLevel,
            String questionType
    ) {
    }

    /**
     * Exception thrown when the LLM call fails.
     */
    public static class QuestionGenerationException extends RuntimeException {
        public QuestionGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
