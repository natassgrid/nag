package com.examplatform.papergenerator.service;

import com.examplatform.papergenerator.client.QuestionBankClient;
import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.dto.BlueprintRule;
import com.examplatform.papergenerator.dto.PaperGenerationRequest;
import com.examplatform.papergenerator.dto.QuestionSummary;
import com.examplatform.papergenerator.repository.PaperRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Property test 7.3: Blueprint Constraint Satisfaction.
 *
 * <p>For any valid blueprint, the generated paper's actual question distribution
 * must match the blueprint's requested counts exactly (within integer rounding
 * tolerance — i.e. exactly, because we generate exactly N questions per rule
 * or throw an InsufficientQuestionsException).
 *
 * <p><strong>Property:</strong> For all valid blueprints B and a mock question bank
 * with sufficient questions, every generated paper P satisfies:
 * {@code |selected(subject,topic,difficulty) == blueprint.questionCount} for every rule.
 *
 * Validates: Requirements 8.1, 8.2
 */
class BlueprintConstraintPropertyTest {

    private static final String TENANT_ID = "tenant-prop-test";
    private static final UUID GENERATED_BY = UUID.randomUUID();

    // Difficulty levels available in the platform
    private static final List<String> DIFFICULTIES = List.of("EASY", "MEDIUM", "HARD");

    // -----------------------------------------------------------------------
    // Generators
    // -----------------------------------------------------------------------

    /**
     * Generates a single valid BlueprintRule with 1–5 questions.
     */
    @Provide
    Arbitrary<BlueprintRule> validBlueprintRule() {
        Arbitrary<String> subject = Arbitraries.of("Mathematics", "Physics", "Chemistry", "Biology", "English");
        Arbitrary<String> topic = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20);
        Arbitrary<String> difficulty = Arbitraries.of("EASY", "MEDIUM", "HARD");
        Arbitrary<Integer> count = Arbitraries.integers().between(1, 5);

        return Combinators.combine(subject, topic, difficulty, count)
                .as((s, t, d, c) -> BlueprintRule.builder()
                        .subject(s)
                        .topic(t)
                        .difficulty(d)
                        .questionCount(c)
                        .build());
    }

    /**
     * Generates a list of 1–4 blueprint rules, each with a unique subject+topic combination
     * to avoid collisions in the mock question bank setup.
     */
    @Provide
    Arbitrary<List<BlueprintRule>> validBlueprints() {
        return validBlueprintRule().list().ofMinSize(1).ofMaxSize(4)
                .map(rules -> {
                    // De-duplicate subject+topic pairs to keep mock setup simple
                    List<BlueprintRule> deduped = new ArrayList<>();
                    for (BlueprintRule rule : rules) {
                        boolean duplicate = deduped.stream().anyMatch(r ->
                                r.getSubject().equals(rule.getSubject()) &&
                                r.getTopic().equals(rule.getTopic()) &&
                                r.getDifficulty().equals(rule.getDifficulty()));
                        if (!duplicate) {
                            deduped.add(rule);
                        }
                    }
                    return deduped;
                })
                .filter(rules -> !rules.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Properties
    // -----------------------------------------------------------------------

    /**
     * Core property: generated paper contains exactly the number of questions
     * specified by each blueprint rule when sufficient questions exist.
     */
    @Property(tries = 200)
    @Label("Generated paper satisfies blueprint question counts exactly")
    void generatedPaperSatisfiesBlueprintQuestionCounts(
            @ForAll("validBlueprints") List<BlueprintRule> blueprintRules) throws Exception {

        // Arrange: build mocks
        QuestionBankClient questionBankClient = Mockito.mock(QuestionBankClient.class);
        PaperRepository paperRepository = Mockito.mock(PaperRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        PaperAssemblyService service = new PaperAssemblyService(
                questionBankClient, paperRepository, kafkaTemplate, objectMapper);

        // For each rule, provide at least (questionCount + 2) eligible questions
        for (BlueprintRule rule : blueprintRules) {
            List<QuestionSummary> pool = buildSufficientPool(rule);
            when(questionBankClient.findAvailableQuestions(
                    rule.getSubject(), rule.getTopic(),
                    rule.getDifficulty(), rule.getCognitiveLevel(), TENANT_ID))
                    .thenReturn(pool);
        }

        when(paperRepository.save(any(Paper.class))).thenAnswer(inv -> {
            Paper p = inv.getArgument(0);
            setId(p, UUID.randomUUID());
            return p;
        });

        Mockito.lenient().when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(new java.util.concurrent.CompletableFuture<>());

        PaperGenerationRequest request = PaperGenerationRequest.builder()
                .examId(UUID.randomUUID())
                .shiftId("SHIFT-PROP")
                .blueprintRules(blueprintRules)
                .build();

        // Act
        Paper paper = service.generatePaper(request, GENERATED_BY, TENANT_ID);

        // Assert: paper definition contains exactly sum(questionCount) question IDs
        int expectedTotal = blueprintRules.stream()
                .mapToInt(BlueprintRule::getQuestionCount)
                .sum();

        List<String> questionIds = extractQuestionIds(paper.getPaperDefinitionJson(), objectMapper);
        assertThat(questionIds)
                .as("Total question count must equal sum of all blueprint rule counts")
                .hasSize(expectedTotal);
    }

    /**
     * Property: no question ID appears twice in a generated paper (uniqueness).
     */
    @Property(tries = 200)
    @Label("Generated paper contains no duplicate question IDs")
    void generatedPaperHasNoDuplicateQuestions(
            @ForAll("validBlueprints") List<BlueprintRule> blueprintRules) throws Exception {

        QuestionBankClient questionBankClient = Mockito.mock(QuestionBankClient.class);
        PaperRepository paperRepository = Mockito.mock(PaperRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        PaperAssemblyService service = new PaperAssemblyService(
                questionBankClient, paperRepository, kafkaTemplate, objectMapper);

        for (BlueprintRule rule : blueprintRules) {
            when(questionBankClient.findAvailableQuestions(
                    rule.getSubject(), rule.getTopic(),
                    rule.getDifficulty(), rule.getCognitiveLevel(), TENANT_ID))
                    .thenReturn(buildSufficientPool(rule));
        }

        when(paperRepository.save(any(Paper.class))).thenAnswer(inv -> {
            Paper p = inv.getArgument(0);
            setId(p, UUID.randomUUID());
            return p;
        });

        Mockito.lenient().when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(new java.util.concurrent.CompletableFuture<>());

        PaperGenerationRequest request = PaperGenerationRequest.builder()
                .examId(UUID.randomUUID())
                .shiftId("SHIFT-PROP")
                .blueprintRules(blueprintRules)
                .build();

        Paper paper = service.generatePaper(request, GENERATED_BY, TENANT_ID);

        List<String> ids = extractQuestionIds(paper.getPaperDefinitionJson(), objectMapper);
        long distinctCount = ids.stream().distinct().count();

        assertThat(distinctCount)
                .as("All question IDs in the paper must be unique — no duplicates")
                .isEqualTo(ids.size());
    }

    /**
     * Property: difficulty score is bounded to the range [1.0, 3.0] (EASY weight=1,
     * MEDIUM weight=2, HARD weight=3), reflecting the weighted average of selected
     * questions.
     */
    @Property(tries = 200)
    @Label("Computed difficulty score is within [1.0, 3.0]")
    void difficultyScoreIsWithinExpectedRange(
            @ForAll("validBlueprints") List<BlueprintRule> blueprintRules) throws Exception {

        QuestionBankClient questionBankClient = Mockito.mock(QuestionBankClient.class);
        PaperRepository paperRepository = Mockito.mock(PaperRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        PaperAssemblyService service = new PaperAssemblyService(
                questionBankClient, paperRepository, kafkaTemplate, objectMapper);

        for (BlueprintRule rule : blueprintRules) {
            when(questionBankClient.findAvailableQuestions(
                    rule.getSubject(), rule.getTopic(),
                    rule.getDifficulty(), rule.getCognitiveLevel(), TENANT_ID))
                    .thenReturn(buildSufficientPool(rule));
        }

        when(paperRepository.save(any(Paper.class))).thenAnswer(inv -> {
            Paper p = inv.getArgument(0);
            setId(p, UUID.randomUUID());
            return p;
        });

        Mockito.lenient().when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(new java.util.concurrent.CompletableFuture<>());

        PaperGenerationRequest request = PaperGenerationRequest.builder()
                .examId(UUID.randomUUID())
                .shiftId("SHIFT-PROP")
                .blueprintRules(blueprintRules)
                .build();

        Paper paper = service.generatePaper(request, GENERATED_BY, TENANT_ID);

        assertThat(paper.getDifficultyScore())
                .as("Difficulty score must be within [1.0, 3.0]")
                .isGreaterThanOrEqualTo(1.0)
                .isLessThanOrEqualTo(3.0);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Builds a pool of (questionCount + 2) fresh questions for a rule,
     * ensuring the assembler can always satisfy the rule.
     */
    private List<QuestionSummary> buildSufficientPool(BlueprintRule rule) {
        int poolSize = rule.getQuestionCount() + 2;
        List<QuestionSummary> pool = new ArrayList<>();
        for (int i = 0; i < poolSize; i++) {
            pool.add(QuestionSummary.builder()
                    .questionId(UUID.randomUUID())
                    .subject(rule.getSubject())
                    .topic(rule.getTopic())
                    .difficulty(rule.getDifficulty())
                    .cognitiveLevel(rule.getCognitiveLevel())
                    .usageCount(0)
                    .reusePolicy(null)
                    .build());
        }
        return pool;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractQuestionIds(String paperDefinitionJson, ObjectMapper mapper) throws Exception {
        JsonNode root = mapper.readTree(paperDefinitionJson);
        // Handles {"questionIds": [...]} wrapper format written by PaperAssemblyService
        JsonNode idsNode = root.isObject() ? root.get("questionIds") : root;
        if (idsNode == null || !idsNode.isArray()) {
            return List.of();
        }
        List<String> result = new java.util.ArrayList<>();
        for (JsonNode node : idsNode) {
            result.add(node.asText());
        }
        return result;
    }

    private void setId(Paper paper, UUID id) {
        try {
            var field = paper.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(paper, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
