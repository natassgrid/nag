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

import com.examplatform.questionbank.exception.QuestionGenerationException;

import com.examplatform.questionbank.ai.embedding.EmbeddingService;
import com.examplatform.questionbank.ai.generation.multiagent.ComplexityEvaluationResult;
import com.examplatform.questionbank.ai.generation.multiagent.ComplexityEvaluator;
import com.examplatform.questionbank.ai.generation.multiagent.CriticReviewResult;
import com.examplatform.questionbank.ai.generation.multiagent.PsychometricCriticAgent;
import com.examplatform.questionbank.ai.generation.multiagent.QuestionAuthorAgent;
import com.examplatform.questionbank.ai.generation.multiagent.RefinementAgent;
import com.examplatform.questionbank.ai.generation.multiagent.RequirementAnalystAgent;
import com.examplatform.questionbank.ai.generation.multiagent.SimilarityAuditorAgent;
import com.examplatform.questionbank.ai.parser.NormalizedSampleQuestion;
import com.examplatform.questionbank.ai.parser.SampleDocumentParserService;
import com.examplatform.questionbank.ai.similarity.SimilarityCheckResult;
import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.repository.SimilarityResult;
import com.examplatform.questionbank.service.SimilarityDetectionService;
import com.examplatform.questionbank.service.SubjectTopicService;
import com.examplatform.questionbank.util.EmbeddingUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spring AI-based implementation of the question generation pipeline.
 *
 * <p>Orchestrates:
 * <ol>
 *   <li>Sample question ingestion and tiered multimodal document parsing</li>
 *   <li>Pre-generation vector search & RAG top-N retrieval to avoid duplicate generation</li>
 *   <li>Complexity triage via {@link ComplexityEvaluator} ("Agent Only When Needed")</li>
 *   <li>Fast Path execution (single lightweight model call, <2s latency, lowest cost)</li>
 *   <li>Multi-Agent collaborative workflow (Requirement Analyst, Question Author,
 *       Psychometric Critic, Similarity Auditor, Refinement Agent)</li>
 *   <li>Preservation of KaTeX/LaTeX math and chemical notations</li>
 *   <li>Schema validation, duplicate detection, and optional auto-save as DRAFT</li>
 * </ol>
 *
 * @see QuestionGenerationService
 * @see ModelRouter
 * @see ComplexityEvaluator
 */
@Slf4j
@Service
public class SpringAiGenerationService implements QuestionGenerationService {

    private static final int RAG_TOP_K = 5;
    private static final double GENERATION_TEMPERATURE = 0.7;

    private static final Set<String> MCQ_TYPES = Set.of("SINGLE_MCQ", "MULTI_MCQ");
    private static final Set<String> VALID_DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");
    private static final Set<String> VALID_COGNITIVE_LEVELS = Set.of(
            "REMEMBER", "UNDERSTAND", "APPLY", "ANALYZE", "EVALUATE", "CREATE");
    private static final Set<String> VALID_QUESTION_TYPES = Set.of(
            "SINGLE_MCQ", "MULTI_MCQ", "NUMERICAL", "DESCRIPTIVE",
            "ASSERTION_REASON", "PARAGRAPH_SET", "MATRIX_MATCH");

    private static final Pattern PAREN_DELIMITER_PATTERN =
            Pattern.compile(Pattern.quote("\\(") + "(.*?)" + Pattern.quote("\\)"), Pattern.DOTALL);

    private static final Pattern BRACKET_DELIMITER_PATTERN =
            Pattern.compile(Pattern.quote("\\[") + "(.*?)" + Pattern.quote("\\]"), Pattern.DOTALL);

    private final ChatClient chatClient;
    private final ModelRouter modelRouter;
    private final EmbeddingService embeddingService;
    private final SimilarityDetectionService similarityDetectionService;
    private final QuestionRepository questionRepository;
    private final SubjectTopicService subjectTopicService;
    private final ObjectMapper objectMapper;

    private final ComplexityEvaluator complexityEvaluator;
    private final RequirementAnalystAgent requirementAnalystAgent;
    private final QuestionAuthorAgent questionAuthorAgent;
    private final PsychometricCriticAgent psychometricCriticAgent;
    private final SimilarityAuditorAgent similarityAuditorAgent;
    private final RefinementAgent refinementAgent;
    private final SampleDocumentParserService sampleDocumentParserService;

    public SpringAiGenerationService(
            ChatClient chatClient,
            ModelRouter modelRouter,
            EmbeddingService embeddingService,
            SimilarityDetectionService similarityDetectionService,
            QuestionRepository questionRepository,
            SubjectTopicService subjectTopicService,
            ObjectMapper objectMapper) {
        this(chatClient, modelRouter, embeddingService, similarityDetectionService,
                questionRepository, subjectTopicService, objectMapper,
                new ComplexityEvaluator(modelRouter),
                new RequirementAnalystAgent(),
                new QuestionAuthorAgent(),
                new PsychometricCriticAgent(),
                new SimilarityAuditorAgent(similarityDetectionService),
                new RefinementAgent(),
                new SampleDocumentParserService());
    }

    @Autowired
    public SpringAiGenerationService(
            ChatClient chatClient,
            ModelRouter modelRouter,
            EmbeddingService embeddingService,
            SimilarityDetectionService similarityDetectionService,
            QuestionRepository questionRepository,
            SubjectTopicService subjectTopicService,
            ObjectMapper objectMapper,
            ComplexityEvaluator complexityEvaluator,
            RequirementAnalystAgent requirementAnalystAgent,
            QuestionAuthorAgent questionAuthorAgent,
            PsychometricCriticAgent psychometricCriticAgent,
            SimilarityAuditorAgent similarityAuditorAgent,
            RefinementAgent refinementAgent,
            SampleDocumentParserService sampleDocumentParserService) {
        this.chatClient = chatClient;
        this.modelRouter = modelRouter;
        this.embeddingService = embeddingService;
        this.similarityDetectionService = similarityDetectionService;
        this.questionRepository = questionRepository;
        this.subjectTopicService = subjectTopicService;
        this.objectMapper = objectMapper;
        this.complexityEvaluator = complexityEvaluator;
        this.requirementAnalystAgent = requirementAnalystAgent;
        this.questionAuthorAgent = questionAuthorAgent;
        this.psychometricCriticAgent = psychometricCriticAgent;
        this.similarityAuditorAgent = similarityAuditorAgent;
        this.refinementAgent = refinementAgent;
        this.sampleDocumentParserService = sampleDocumentParserService;
    }

    @Override
    @Transactional
    public QuestionGenerationResponse generate(QuestionGenerationRequest request, String tenantId, UUID authorId) {
        List<NormalizedSampleQuestion> sampleQuestions = new ArrayList<>();
        if (request.getSampleQuestions() != null && !request.getSampleQuestions().isEmpty()) {
            for (String sampleText : request.getSampleQuestions()) {
                sampleQuestions.add(sampleDocumentParserService.normalizeTextQuestion(sampleText, "TIER_0_LOCAL_TEXT", null));
            }
        }
        return generateWithSamples(request, sampleQuestions, tenantId, authorId);
    }

    @Override
    @Transactional
    public QuestionGenerationResponse generateWithSamples(
            QuestionGenerationRequest request,
            List<NormalizedSampleQuestion> sampleQuestions,
            String tenantId,
            UUID authorId) {

        if (sampleQuestions == null) {
            sampleQuestions = new ArrayList<>();
        }

        // If sample questions list is empty but request has raw strings, normalize them
        if (sampleQuestions.isEmpty() && request.getSampleQuestions() != null && !request.getSampleQuestions().isEmpty()) {
            for (String sampleText : request.getSampleQuestions()) {
                sampleQuestions.add(sampleDocumentParserService.normalizeTextQuestion(sampleText, "TIER_0_LOCAL_TEXT", null));
            }
        }

        log.info("Starting question generation with {} sample questions (executionMode={}): subject={}, topic={}, count={}",
                sampleQuestions.size(), request.getExecutionMode(), request.getSubject(), request.getTopic(), request.getCount());

        // Step 0: Pre-Generation Vector Search & RAG Context Retrieval to Avoid Duplicate Question Generation
        List<SimilarityResult> ragContext = retrieveRagContext(request, tenantId);
        log.info("Retrieved {} existing RAG question(s) via vector search to avoid duplicate generation (query='{}')",
                ragContext.size(), request.buildSearchQuery());

        // Step 1: Intelligent Complexity Triage ("Agent Only When Needed")
        ComplexityEvaluationResult triage = complexityEvaluator.evaluate(request, sampleQuestions);
        log.info("Triage outcome: useMultiAgent={}, targetModel={}, rationale={}",
                triage.isUseMultiAgent(), triage.getTargetModel(), triage.getRationale());

        Map<String, Object> sampleMetadata = buildSampleParsingMetadata(sampleQuestions);

        if (triage.isUseMultiAgent()) {
            return executeMultiAgentPipeline(request, sampleQuestions, triage, ragContext, tenantId, authorId, sampleMetadata);
        } else {
            return executeFastPath(request, sampleQuestions, triage, ragContext, tenantId, authorId, sampleMetadata);
        }
    }

    @Override
    public ClarifyRequirementsResponse clarifyRequirements(ClarifyRequirementsRequest request) {
        return requirementAnalystAgent.clarifyRequirements(request);
    }

    /**
     * Executes Single-Model Fast Path: single lightweight LLM call, fast JSON output (<2s).
     */
    private QuestionGenerationResponse executeFastPath(
            QuestionGenerationRequest request,
            List<NormalizedSampleQuestion> sampleQuestions,
            ComplexityEvaluationResult triage,
            List<SimilarityResult> ragContext,
            String tenantId,
            UUID authorId,
            Map<String, Object> sampleMetadata) {

        String modelName = triage.getTargetModel();

        String systemPrompt = buildSystemPrompt(request);
        String userPrompt = buildUserPromptWithSamples(request, ragContext, sampleQuestions);

        String llmResponse = callLlm(modelName, systemPrompt, userPrompt);
        List<RawGeneratedQuestion> rawQuestions = parseLlmResponse(llmResponse);
        int totalGenerated = rawQuestions.size();

        List<QuestionGenerationResponse.GeneratedQuestion> processedQuestions = new ArrayList<>();
        int totalValid = 0;
        int totalDuplicates = 0;

        for (RawGeneratedQuestion raw : rawQuestions) {
            QuestionGenerationResponse.ValidationResult validation = validateQuestion(raw, request.getQuestionType());
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

            UUID savedId = null;
            if (request.isAutoSave() && validation.isValid() && duplicateResult == null) {
                savedId = persistAsDraft(raw, request, tenantId, authorId);
            }

            processedQuestions.add(baseGeneratedQuestionBuilder(raw, request, validation)
                    .duplicate(duplicateResult)
                    .savedQuestionId(savedId)
                    .build());
        }

        return QuestionGenerationResponse.builder()
                .questions(processedQuestions)
                .modelUsed(modelName)
                .totalGenerated(totalGenerated)
                .totalValid(totalValid)
                .totalDuplicates(totalDuplicates)
                .executionMode("FAST")
                .triageRationale(triage.getRationale())
                .sampleParsingMetadata(sampleMetadata)
                .build();
    }

    /**
     * Executes Multi-Agent Collaborative Committee workflow:
     * Requirement Analyst -> Question Author -> Psychometric Critic -> Refinement Agent -> Similarity Auditor.
     */
    private QuestionGenerationResponse executeMultiAgentPipeline(
            QuestionGenerationRequest request,
            List<NormalizedSampleQuestion> sampleQuestions,
            ComplexityEvaluationResult triage,
            List<SimilarityResult> ragContext,
            String tenantId,
            UUID authorId,
            Map<String, Object> sampleMetadata) {

        String modelName = triage.getTargetModel();

        // 1. Requirement Analyst Agent synthesizes blueprint
        String blueprint = requirementAnalystAgent.formulateBlueprint(request, sampleQuestions);
        log.debug("Multi-agent blueprint synthesized:\n{}", blueprint);

        // 2. Question Author Agent constructs specialized prompt with few-shot demonstrations and RAG context
        String userPrompt = questionAuthorAgent.buildPromptWithBlueprint(blueprint, request, sampleQuestions, ragContext);
        String systemPrompt = buildSystemPrompt(request);

        // Call LLM for candidate generation
        String llmResponse = callLlm(modelName, systemPrompt, userPrompt);
        List<RawGeneratedQuestion> rawQuestions = parseLlmResponse(llmResponse);
        int totalGenerated = rawQuestions.size();

        List<QuestionGenerationResponse.GeneratedQuestion> processedQuestions = new ArrayList<>();
        int totalValid = 0;
        int totalDuplicates = 0;

        for (RawGeneratedQuestion raw : rawQuestions) {
            QuestionGenerationResponse.ValidationResult validation = validateQuestion(raw, request.getQuestionType());

            // 3. Psychometric Critic Agent reviews question
            CriticReviewResult criticResult = psychometricCriticAgent.reviewQuestion(
                    raw.content, raw.answerKey, raw.explanation, raw.options, request.getQuestionType());

            QuestionGenerationResponse.GeneratedQuestion candidate = baseGeneratedQuestionBuilder(raw, request, validation)
                    .criticScore(criticResult.getScore())
                    .criticFeedback(criticResult.getIssues())
                    .build();

            // 4. Refinement Agent resolves critic issues (bounded to 1 iteration)
            if (!criticResult.isApproved()) {
                candidate = refinementAgent.refine(candidate, criticResult);
            }

            // 5. Similarity Auditor Agent verifies uniqueness (<0.85 threshold)
            QuestionGenerationResponse.DuplicateResult duplicateResult = null;
            if (request.isAvoidDuplicate()) {
                SimilarityAuditorAgent.AuditResult audit = similarityAuditorAgent.auditUniqueness(
                        candidate.getContent(), request.getSubject(), tenantId);
                if (!audit.passed()) {
                    totalDuplicates++;
                    duplicateResult = QuestionGenerationResponse.DuplicateResult.builder()
                            .similarQuestionId(audit.conflictingQuestionId())
                            .similarity(audit.topSimilarity())
                            .build();
                }
            }
            candidate.setDuplicate(duplicateResult);

            if (validation.isValid()) {
                totalValid++;
            }

            // Auto-save if valid and not duplicate
            UUID savedId = null;
            if (request.isAutoSave() && validation.isValid() && duplicateResult == null) {
                RawGeneratedQuestion refinedRaw = new RawGeneratedQuestion(
                        candidate.getContent(),
                        candidate.getAnswerKey(),
                        candidate.getExplanation(),
                        candidate.getOptions(),
                        candidate.getDifficulty(),
                        candidate.getCognitiveLevel(),
                        candidate.getQuestionType());
                savedId = persistAsDraft(refinedRaw, request, tenantId, authorId);
            }
            candidate.setSavedQuestionId(savedId);

            processedQuestions.add(candidate);
        }

        return QuestionGenerationResponse.builder()
                .questions(processedQuestions)
                .modelUsed(modelName)
                .totalGenerated(totalGenerated)
                .totalValid(totalValid)
                .totalDuplicates(totalDuplicates)
                .executionMode("MULTI_AGENT")
                .triageRationale(triage.getRationale())
                .sampleParsingMetadata(sampleMetadata)
                .build();
    }

    private Map<String, Object> buildSampleParsingMetadata(List<NormalizedSampleQuestion> sampleQuestions) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sampleCount", sampleQuestions.size());
        if (!sampleQuestions.isEmpty()) {
            List<String> tiers = sampleQuestions.stream().map(NormalizedSampleQuestion::getParsingTier).toList();
            metadata.put("tiersUsed", tiers);
            boolean anyDiagram = sampleQuestions.stream().anyMatch(NormalizedSampleQuestion::isHasDiagram);
            boolean anyMathChem = sampleQuestions.stream().anyMatch(NormalizedSampleQuestion::isHasMathOrChemistry);
            metadata.put("containsDiagram", anyDiagram);
            metadata.put("containsMathOrChemistry", anyMathChem);
        }
        return metadata;
    }

    private List<SimilarityResult> retrieveRagContext(QuestionGenerationRequest request, String tenantId) {
        try {
            String queryText = request.buildSearchQuery();
            if (queryText.isBlank()) {
                queryText = request.getSubject() + " " + request.getTopic();
            }
            float[] queryEmbedding = embeddingService.embed(queryText);
            String embeddingStr = EmbeddingUtils.embeddingToString(queryEmbedding);

            return questionRepository.findTopSimilarQuestions(
                    embeddingStr, request.getSubject(), tenantId, RAG_TOP_K);
        } catch (Exception e) {
            log.warn("Failed to retrieve RAG context, proceeding without it: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildSystemPrompt(QuestionGenerationRequest request) {
        return """
                You are an expert examination question generator for Indian competitive examinations.
                You generate high-quality questions in structured JSON format.

                Formatting & Syntax Rules:
                - Generate questions strictly matching the specified type, difficulty, and cognitive level.
                - ALL mathematical, physical, and chemical formulas, expressions, variables, percentages, and unit notations in EVERY field (content, options, answerKey, and explanation) MUST be enclosed in $$...$$ LaTeX syntax.
                - Chemical reactions MUST use valid LaTeX syntax (e.g. $$\\ce{2H2 + O2 -> 2H2O}$$).
                - NEVER use \\( ... \\) or \\[ ... \\] or single $.
                - In LaTeX math mode ($$...$$), always write percentage symbols as \\% (e.g. $$99.9\\%$$).
                - Use standard Markdown for multi-line formatting (e.g. **Statements:**, **Conclusions:**, tables).
                - Use double newlines (\\n\\n) to separate headings and paragraphs, and single newlines (\\n) between numbered statement items.
                - For MCQ (SINGLE_MCQ): exactly 4 options with ids A, B, C, D. Set "isCorrect": true on EXACTLY ONE option and "isCorrect": false on the other three. The "answerKey" must be the id (A/B/C/D) of the correct option.
                - For MSQ (MULTI_MCQ): exactly 4 options (A, B, C, D), 2 or more correct.
                - For NUMERICAL: no options, answerKey is the numeric value.
                - For DESCRIPTIVE: no options, answerKey contains the model answer.
                - Always provide a clear explanation for the correct answer.
                - Do NOT repeat questions from the provided context — generate novel questions.
                - Use only english language.

                Output ONLY a JSON array of question objects. No markdown, only English language, no explanation outside JSON.
                Each question object must have these fields:
                {
                  "content": "question text (may include $$LaTeX$$ or <svg> and \\n line breaks)",
                  "answerKey": "correct answer key or value",
                  "explanation": "explanation of the correct answer with $$LaTeX$$",
                  "options": [{"id": "A", "text": "option text", "isCorrect": true}, {"id": "B", "text": "option text", "isCorrect": false}, {"id": "C", "text": "option text", "isCorrect": false}, {"id": "D", "text": "option text", "isCorrect": false}],
                  "difficulty": "EASY|MEDIUM|HARD",
                  "cognitiveLevel": "REMEMBER|UNDERSTAND|APPLY|ANALYZE|EVALUATE|CREATE",
                  "questionType": "{{QUESTION_TYPE}}"
                }
                """.replace("{{QUESTION_TYPE}}", String.valueOf(request.getQuestionType()));
    }

    private String buildUserPromptWithSamples(
            QuestionGenerationRequest request,
            List<SimilarityResult> ragContext,
            List<NormalizedSampleQuestion> sampleQuestions) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate ").append(request.getCount()).append(" question(s) with these parameters:\n");
        QuestionPromptFormatter.appendGenerationParameters(
                prompt, request, "- ", "Description/Requirements", "Target Exam");

        if (sampleQuestions != null && !sampleQuestions.isEmpty()) {
            prompt.append("\nReference Sample Demonstrations (Model question style and depth):\n");
            for (int i = 0; i < sampleQuestions.size(); i++) {
                NormalizedSampleQuestion sq = sampleQuestions.get(i);
                prompt.append("Sample ").append(i + 1).append(": ").append(sq.getStem()).append("\n");
            }
        }

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

    private List<RawGeneratedQuestion> parseLlmResponse(String response) {
        if (response == null || response.isBlank()) {
            log.warn("Empty LLM response received");
            return List.of();
        }

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

        if (json.startsWith("{")) {
            json = "[" + json + "]";
        }

        try {
            List<RawGeneratedQuestion> parsed =
                    objectMapper.readValue(json, new TypeReference<List<RawGeneratedQuestion>>() {});
            return parsed.stream().map(this::normalizeLatexDelimiters).toList();
        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM response as JSON: {}", e.getMessage());
            log.debug("Raw response: {}", response);
            return List.of();
        }
    }

    private RawGeneratedQuestion normalizeLatexDelimiters(RawGeneratedQuestion raw) {
        if (raw == null) {
            return null;
        }
        List<QuestionOption> normalizedOptions = raw.options;
        if (normalizedOptions != null) {
            for (QuestionOption option : normalizedOptions) {
                if (option != null) {
                    option.setText(normalizeLatex(option.getText()));
                }
            }
        }
        return new RawGeneratedQuestion(
                normalizeLatex(raw.content),
                normalizeLatex(raw.answerKey),
                normalizeLatex(raw.explanation),
                normalizedOptions,
                raw.difficulty,
                raw.cognitiveLevel,
                raw.questionType);
    }

    private String normalizeLatex(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String res = PAREN_DELIMITER_PATTERN.matcher(text)
                .replaceAll(mr -> Matcher.quoteReplacement("$$" + mr.group(1) + "$$"));
        return BRACKET_DELIMITER_PATTERN.matcher(res)
                .replaceAll(mr -> Matcher.quoteReplacement("$$" + mr.group(1) + "$$"));
    }

    private QuestionGenerationResponse.ValidationResult validateQuestion(
            RawGeneratedQuestion question, String expectedType) {
        List<String> errors = new ArrayList<>();

        if (question.content == null || question.content.isBlank()) {
            errors.add("Content is required");
        }
        if (question.answerKey == null || question.answerKey.isBlank()) {
            errors.add("Answer key is required");
        }

        String questionType = question.questionType != null ? question.questionType : expectedType;
        if (questionType != null && !VALID_QUESTION_TYPES.contains(questionType.toUpperCase())) {
            errors.add("Invalid question type: " + questionType);
        }

        if (question.difficulty != null && !VALID_DIFFICULTIES.contains(question.difficulty.toUpperCase())) {
            errors.add("Invalid difficulty: " + question.difficulty);
        }

        if (question.cognitiveLevel != null
                && !VALID_COGNITIVE_LEVELS.contains(question.cognitiveLevel.toUpperCase())) {
            errors.add("Invalid cognitive level: " + question.cognitiveLevel);
        }

        if (MCQ_TYPES.contains(expectedType.toUpperCase())) {
            if (question.options == null || question.options.isEmpty()) {
                errors.add("Options are required for " + expectedType);
            } else {
                if (question.options.size() != 4) {
                    errors.add("MCQ must have exactly 4 options, got " + question.options.size());
                }

                long correctCount = question.options.stream()
                        .filter(QuestionOption::isCorrect)
                        .count();

                if (correctCount == 0 && question.answerKey != null && !question.answerKey.isBlank()) {
                    String key = question.answerKey.trim();
                    for (QuestionOption opt : question.options) {
                        if (opt.getId() != null && opt.getId().equalsIgnoreCase(key)) {
                            opt.setCorrect(true);
                            correctCount = 1;
                            break;
                        }
                    }
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
            }
        }

        return QuestionGenerationResponse.ValidationResult.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .build();
    }

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

    private UUID persistAsDraft(RawGeneratedQuestion raw, QuestionGenerationRequest request, String tenantId, UUID authorId) {
        try {
            SubjectTopicService.HierarchyIds ids;
            if (request.getSubjectId() != null && request.getTopicId() != null) {
                // Id-based path: validate that the ids exist; never create new nodes
                ids = subjectTopicService.resolveByIds(
                        request.getSubjectId(), request.getTopicId(), request.getSubtopicId(), tenantId);
            } else {
                // Name-based path: legacy resolve-or-create (used when ids are absent, e.g. old clients)
                ids = subjectTopicService.resolveOrCreateByName(
                        request.getSubject(), request.getTopic(), request.getSubtopic(), tenantId);
            }
            Question question = Question.builder()
                    .subjectId(ids.subjectId())
                    .topicId(ids.topicId())
                    .subtopicId(ids.subtopicId())
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
                    .sourceReferences("AI-generated via " + modelRouter.selectModel(request.getDifficulty()))
                    .state("DRAFT")
                    .authorId(authorId)
                    .build();
            question.setTenantId(tenantId);

            Question saved = questionRepository.save(question);

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

    private record RawGeneratedQuestion(
            String content,
            String answerKey,
            String explanation,
            List<QuestionOption> options,
            String difficulty,
            String cognitiveLevel,
            String questionType) {}

    private QuestionGenerationResponse.GeneratedQuestion.GeneratedQuestionBuilder baseGeneratedQuestionBuilder(
            RawGeneratedQuestion raw, QuestionGenerationRequest request, QuestionGenerationResponse.ValidationResult validation) {
        return QuestionGenerationResponse.GeneratedQuestion.builder()
                .content(raw.content)
                .answerKey(raw.answerKey)
                .explanation(raw.explanation)
                .options(raw.options)
                .difficulty(raw.difficulty != null ? raw.difficulty : request.getDifficulty())
                .cognitiveLevel(raw.cognitiveLevel != null ? raw.cognitiveLevel : request.getCognitiveLevel())
                .questionType(raw.questionType != null ? raw.questionType : request.getQuestionType())
                .validation(validation);
    }
}