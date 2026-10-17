// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.ai;

import com.examplatform.questionbank.ai.generation.ModelRouter;
import com.examplatform.questionbank.microlesson.ai.agent.*;
import com.examplatform.questionbank.microlesson.domain.MicroLessonStatus;
import com.examplatform.questionbank.microlesson.domain.SubtopicMicroLesson;
import com.examplatform.questionbank.microlesson.dto.MicroLessonContentDto;
import com.examplatform.questionbank.microlesson.dto.MicroLessonResponse;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import com.examplatform.questionbank.microlesson.repository.SubtopicMicroLessonRepository;
import com.examplatform.shared.constants.QuestionTaxonomyConstants;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.examplatform.questionbank.exception.AiSecurityException;
import com.examplatform.questionbank.microlesson.ai.guardrails.InputGuardrail;
import com.examplatform.questionbank.microlesson.ai.guardrails.OutputGuardrail;
import com.examplatform.questionbank.microlesson.ai.resilience.MicroLessonAiResilienceExecutor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class MicroLessonGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(MicroLessonGeneratorService.class);
    private static final double DETERMINISTIC_TEMPERATURE = 0.2;

    private final ChatClient chatClient;
    private final MicroLessonPromptBuilder promptBuilder;
    private final KaTexValidator kaTexValidator;
    private final SubtopicMicroLessonRepository repository;
    private final ObjectMapper objectMapper;
    private final ObjectMapper aiObjectMapper;
    private final ModelRouter modelRouter;

    private final MicroLessonBlueprintAgent blueprintAgent;
    private final DeterministicJsonRepairEngine jsonRepairEngine;
    private final MicroLessonCriticAgent criticAgent;
    private final MicroLessonRefinementAgent refinementAgent;
    private final DeterministicMicroLessonAgent deterministicAgent;

    private final InputGuardrail inputGuardrail;
    private final OutputGuardrail outputGuardrail;
    private final MicroLessonAiResilienceExecutor resilienceExecutor;

    @Value("${app.ai.agent.max-refinement-cycles:${AI_AGENT_MAX_REFINEMENT_CYCLES:2}}")
    private int maxRefinementCycles = 2;

    @Value("${app.ai.guardrails.max-output-tokens:${AI_MAX_OUTPUT_TOKENS:2048}}")
    private int maxOutputTokens = 2048;

    public MicroLessonGeneratorService(
            ChatClient chatClient,
            MicroLessonPromptBuilder promptBuilder,
            KaTexValidator kaTexValidator,
            SubtopicMicroLessonRepository repository,
            ObjectMapper objectMapper) {
        this(chatClient, promptBuilder, kaTexValidator, repository, objectMapper, null);
    }

    public MicroLessonGeneratorService(
            ChatClient chatClient,
            MicroLessonPromptBuilder promptBuilder,
            KaTexValidator kaTexValidator,
            SubtopicMicroLessonRepository repository,
            ObjectMapper objectMapper,
            ModelRouter modelRouter) {
        this(chatClient, promptBuilder, kaTexValidator, repository, objectMapper, modelRouter,
                new MicroLessonBlueprintAgent(),
                new DeterministicJsonRepairEngine(),
                new MicroLessonCriticAgent(kaTexValidator),
                new MicroLessonRefinementAgent(),
                new DeterministicMicroLessonAgent());
    }

    public MicroLessonGeneratorService(
            ChatClient chatClient,
            MicroLessonPromptBuilder promptBuilder,
            KaTexValidator kaTexValidator,
            SubtopicMicroLessonRepository repository,
            ObjectMapper objectMapper,
            ModelRouter modelRouter,
            MicroLessonBlueprintAgent blueprintAgent,
            DeterministicJsonRepairEngine jsonRepairEngine,
            MicroLessonCriticAgent criticAgent,
            MicroLessonRefinementAgent refinementAgent,
            DeterministicMicroLessonAgent deterministicAgent) {
        this(chatClient, promptBuilder, kaTexValidator, repository, objectMapper, modelRouter,
                blueprintAgent, jsonRepairEngine, criticAgent, refinementAgent, deterministicAgent,
                new InputGuardrail(), new OutputGuardrail(), new MicroLessonAiResilienceExecutor());
    }

    @Autowired
    public MicroLessonGeneratorService(
            ChatClient chatClient,
            MicroLessonPromptBuilder promptBuilder,
            KaTexValidator kaTexValidator,
            SubtopicMicroLessonRepository repository,
            ObjectMapper objectMapper,
            @Autowired(required = false) ModelRouter modelRouter,
            @Autowired(required = false) MicroLessonBlueprintAgent blueprintAgent,
            @Autowired(required = false) DeterministicJsonRepairEngine jsonRepairEngine,
            @Autowired(required = false) MicroLessonCriticAgent criticAgent,
            @Autowired(required = false) MicroLessonRefinementAgent refinementAgent,
            @Autowired(required = false) DeterministicMicroLessonAgent deterministicAgent,
            @Autowired(required = false) InputGuardrail inputGuardrail,
            @Autowired(required = false) OutputGuardrail outputGuardrail,
            @Autowired(required = false) MicroLessonAiResilienceExecutor resilienceExecutor) {
        this.chatClient = chatClient;
        this.promptBuilder = promptBuilder;
        this.kaTexValidator = kaTexValidator;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.aiObjectMapper = objectMapper.copy()
                .configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true)
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.modelRouter = modelRouter;
        this.blueprintAgent = blueprintAgent != null ? blueprintAgent : new MicroLessonBlueprintAgent();
        this.jsonRepairEngine = jsonRepairEngine != null ? jsonRepairEngine : new DeterministicJsonRepairEngine();
        this.criticAgent = criticAgent != null ? criticAgent : new MicroLessonCriticAgent(kaTexValidator);
        this.refinementAgent = refinementAgent != null ? refinementAgent : new MicroLessonRefinementAgent();
        this.deterministicAgent = deterministicAgent != null ? deterministicAgent : new DeterministicMicroLessonAgent(this.blueprintAgent);
        this.inputGuardrail = inputGuardrail != null ? inputGuardrail : new InputGuardrail();
        this.outputGuardrail = outputGuardrail != null ? outputGuardrail : new OutputGuardrail();
        this.resilienceExecutor = resilienceExecutor != null ? resilienceExecutor : new MicroLessonAiResilienceExecutor();
    }

    /**
     * Resolves the complexity difficulty level based on target exam and topic/subtopic depth.
     */
    public String resolveDifficulty(String subject, String topic, String subtopic, String targetExam) {
        String exam = targetExam != null ? targetExam.toUpperCase().trim() : "";
        if (exam.contains("JEE_ADV") || exam.contains("GATE") || exam.contains("UPSC") || exam.contains("CAT")) {
            return QuestionTaxonomyConstants.Difficulty.HARD;
        }
        if (exam.contains("JEE_MAIN") || exam.contains("NEET") || exam.contains("SSC")
                || exam.contains("RRB") || exam.contains("BANK") || exam.contains("IBPS")
                || exam.contains("SBI") || exam.contains("DEFICIT")) {
            return QuestionTaxonomyConstants.Difficulty.MEDIUM;
        }

        // Topic and Subtopic complexity heuristic
        String combined = (subject + " " + topic + " " + subtopic).toLowerCase();
        if (combined.contains("quantum") || combined.contains("relativity") || combined.contains("calculus")
                || combined.contains("dynamic programming") || combined.contains("electromagnetism")) {
            return QuestionTaxonomyConstants.Difficulty.HARD;
        }
        if (combined.contains("algebra") || combined.contains("mechanics") || combined.contains("thermodynamics")
                || combined.contains("organic") || combined.contains("circuits") || combined.contains("percentage")
                || combined.contains("ratio") || combined.contains("geometry")) {
            return QuestionTaxonomyConstants.Difficulty.MEDIUM;
        }

        return QuestionTaxonomyConstants.Difficulty.EASY;
    }

    @Transactional
    public SubtopicMicroLesson generateForSubtopic(
            Long subjectId, String subject,
            Long topicId, String topic,
            Long subtopicId, String subtopic,
            String targetExam) {
        return generateForSubtopic(subjectId, subject, topicId, topic, subtopicId, subtopic, targetExam, null);
    }

    @Transactional
    public SubtopicMicroLesson generateForSubtopic(
            Long subjectId, String subject,
            Long topicId, String topic,
            Long subtopicId, String subtopic,
            String targetExam,
            String referenceText) {

        // Step 0: Input Guardrail validation & sanitization (OWASP LLM01, LLM04)
        inputGuardrail.validateAndSanitize("subject", subject);
        inputGuardrail.validateAndSanitize("topic", topic);
        inputGuardrail.validateAndSanitize("subtopic", subtopic);
        inputGuardrail.validateAndSanitize("targetExam", targetExam);
        if (referenceText != null) {
            inputGuardrail.validateAndSanitize("referenceText", referenceText);
        }

        String exam = (targetExam != null && !targetExam.isBlank()) ? targetExam : "JEE_MAIN";
        String difficulty = resolveDifficulty(subject, topic, subtopic, exam);
        String targetModel = (modelRouter != null) ? modelRouter.selectModel(difficulty) : null;

        // Stage 1: Synthesize domain-grounded pedagogical blueprint (with RAG sample question lookup)
        MicroLessonBlueprint blueprint = blueprintAgent.createBlueprint(subject, topic, subtopic, exam, difficulty);

        log.info("Generating remedial micro-lesson for subtopic: '{}' in topic: '{}' (Exam: {}, Difficulty: {}, Domain: {}, Model: {})",
                subtopic, topic, exam, difficulty, blueprint.domainCategory(), targetModel);

        MicroLessonContentDto content;
        try {
            // Stage 2: Build clean prompt with blueprint directives and no ST4 brace corruption
            String userPrompt = promptBuilder.buildUserPrompt(subject, topic, subtopic, exam, referenceText, blueprint);

            // Stage 3: Execute LLM call via Resilience4j Circuit Breaker & Retry (OWASP LLM04, LLM06)
            String rawResponse = resilienceExecutor.executeChatCall(() -> {
                var promptSpec = chatClient.prompt()
                        .system(promptBuilder.getSystemPrompt())
                        .user(userPrompt);
                if (targetModel != null && !targetModel.isBlank()) {
                    promptSpec = promptSpec.options(ChatOptions.builder()
                            .model(targetModel)
                            .temperature(DETERMINISTIC_TEMPERATURE)
                            .maxTokens(maxOutputTokens));
                }
                return promptSpec.call().content();
            });
            content = parseAndValidate(rawResponse, subject, topic, subtopic);
        } catch (AiSecurityException e) {
            // OWASP Security exceptions must not be swallowed into deterministic fallback
            throw e;
        } catch (Exception e) {
            log.warn("AI generation failed for subtopic '{}': {}. Synthesizing domain-specific lesson via DeterministicMicroLessonAgent.",
                    subtopic, e.getMessage());
            content = deterministicAgent.synthesize(blueprint);
        }

        // Determine next version
        int nextVersion = repository.findTopBySubtopicIdAndTargetExamOrderByContentVersionDesc(subtopicId, exam)
                .map(prev -> prev.getContentVersion() + 1)
                .orElse(1);

        String cheatSheetJson;
        String stepJson;
        try {
            cheatSheetJson = objectMapper.writeValueAsString(content.cheatSheet());
            stepJson = objectMapper.writeValueAsString(content.stepByStepBreakdown());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize micro-lesson content", e);
        }

        SubtopicMicroLesson entity = SubtopicMicroLesson.builder()
                .subjectId(subjectId)
                .topicId(topicId)
                .subtopicId(subtopicId)
                .subject(subject != null ? subject : "General")
                .topic(topic != null ? topic : "General")
                .subtopic(subtopic)
                .targetExam(exam)
                .coreConcept(content.coreConcept())
                .trap(content.trap())
                .cheatSheetJson(cheatSheetJson)
                .stepByStepBreakdownJson(stepJson)
                .status(MicroLessonStatus.AI_GENERATED.name())
                .contentVersion(nextVersion)
                .build();

        return repository.save(entity);
    }

    public MicroLessonContentDto parseAndValidate(String responseText) throws JsonProcessingException {
        return parseAndValidate(responseText, null, null, null);
    }

    public MicroLessonContentDto parseAndValidate(
            String responseText,
            String subject,
            String topic,
            String subtopic) throws JsonProcessingException {
        if (responseText == null || responseText.isBlank()) {
            throw new IllegalArgumentException("Empty response from AI chat client");
        }

        // Stage 4: Self-healing JSON & LaTeX repair (fixes truncated objects at line 14, \frac/\times/\text escapes, ST4 \{ braces)
        MicroLessonContentDto content = jsonRepairEngine.parseAndRepair(responseText, aiObjectMapper);

        // Stage 5: Bounded critic review and deterministic KaTeX / domain refinement (OWASP LLM06)
        for (int cycle = 0; cycle < maxRefinementCycles; cycle++) {
            MicroLessonCriticResult criticResult = criticAgent.review(content, subject, topic, subtopic);
            if (criticResult == null || criticResult.isApproved()) {
                break;
            }
            log.debug("Refining generated micro-lesson for subtopic '{}' (cycle={}/{}, critic score={}, issues={})",
                    subtopic, cycle + 1, maxRefinementCycles, criticResult.score(), criticResult.issues());
            boolean hasDomainIssue = criticResult.issues() != null
                    && criticResult.issues().stream().anyMatch(i -> i != null && i.startsWith("Rubric 3:"));
            MicroLessonContentDto domainFallback = hasDomainIssue
                    ? buildFallbackContent(subject, topic, subtopic, "JEE_MAIN")
                    : null;
            content = refinementAgent.refine(content, criticResult, domainFallback);
        }

        // Stage 6: Output Guardrail inspection (OWASP LLM02, LLM07, LLM09)
        outputGuardrail.validateOutput(content, responseText);

        List<String> errors = kaTexValidator.validateContent(content);
        if (!errors.isEmpty()) {
            log.warn("KaTeX / validation warnings in generated micro-lesson after refinement: {}", errors);
        }
        return content;
    }

    public static String sanitizeLatexJson(String json) {
        return DeterministicJsonRepairEngine.sanitizeLatexJson(json);
    }

    public MicroLessonContentDto buildFallbackContent(String subject, String topic, String subtopic, String targetExam) {
        String difficulty = resolveDifficulty(subject, topic, subtopic, targetExam);
        MicroLessonBlueprint blueprint = blueprintAgent.createBlueprint(subject, topic, subtopic, targetExam, difficulty);
        return deterministicAgent.synthesize(blueprint);
    }

    public MicroLessonResponse toResponse(SubtopicMicroLesson entity) {
        List<String> cheatSheet = Collections.emptyList();
        StepByStepBreakdownDto breakdown = null;

        if (entity.getCheatSheetJson() != null && !entity.getCheatSheetJson().isBlank()) {
            try {
                cheatSheet = objectMapper.readValue(entity.getCheatSheetJson(), new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse cheatSheet for micro-lesson {}: {}", entity.getId(), e.getMessage());
            }
        }

        if (entity.getStepByStepBreakdownJson() != null && !entity.getStepByStepBreakdownJson().isBlank()) {
            try {
                breakdown = objectMapper.readValue(entity.getStepByStepBreakdownJson(), StepByStepBreakdownDto.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse stepByStepBreakdown for micro-lesson {}: {}", entity.getId(), e.getMessage());
            }
        }

        return new MicroLessonResponse(
                entity.getId(),
                entity.getSubjectId(),
                entity.getTopicId(),
                entity.getSubtopicId(),
                entity.getSubject(),
                entity.getTopic(),
                entity.getSubtopic(),
                entity.getTargetExam(),
                entity.getCoreConcept(),
                entity.getTrap(),
                cheatSheet,
                breakdown,
                entity.getStatus(),
                entity.getReviewedBy(),
                entity.getReviewedAt(),
                entity.getReviewComments(),
                entity.getContentVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public int getMaxRefinementCycles() {
        return maxRefinementCycles;
    }

    public void setMaxRefinementCycles(int maxRefinementCycles) {
        this.maxRefinementCycles = maxRefinementCycles;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(int maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    public InputGuardrail getInputGuardrail() {
        return inputGuardrail;
    }

    public OutputGuardrail getOutputGuardrail() {
        return outputGuardrail;
    }

    public MicroLessonAiResilienceExecutor getResilienceExecutor() {
        return resilienceExecutor;
    }
}
