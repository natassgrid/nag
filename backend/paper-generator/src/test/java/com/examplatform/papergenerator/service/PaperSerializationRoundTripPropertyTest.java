package com.examplatform.papergenerator.service;

import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.dto.PaperDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property test 7.8: Paper Serialization Round-Trip.
 *
 * <p><strong>Properties tested:</strong>
 * <ol>
 *   <li>{@code parse(format(paper)).examId == paper.examId}</li>
 *   <li>{@code parse(format(paper)).shiftId == paper.shiftId}</li>
 *   <li>{@code parse(format(paper)).difficultyScore == paper.difficultyScore}</li>
 *   <li>{@code parse(format(paper)).questionIds == paper.questionIds} (order preserved)</li>
 *   <li>{@code format(parse(format(paper))) == format(paper)} (idempotent second serialization)</li>
 * </ol>
 *
 * Validates: Requirements 28.2, 28.3, 28.4
 */
@SuppressWarnings("unused")
class PaperSerializationRoundTripPropertyTest {

    private final ObjectMapper objectMapper;
    private final PaperSerializer paperSerializer;

    PaperSerializationRoundTripPropertyTest() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        paperSerializer = new PaperSerializer(objectMapper);
    }

    // -----------------------------------------------------------------------
    // Generators
    // -----------------------------------------------------------------------

    /** Arbitrary UUID list with 1–30 entries (realistic question counts). */
    @Provide
    Arbitrary<List<UUID>> questionIdList() {
        return Arbitraries.create(UUID::randomUUID).list().ofMinSize(1).ofMaxSize(30);
    }

    /** Arbitrary shift ID — alphanumeric, non-empty. */
    @Provide
    Arbitrary<String> shiftId() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30);
    }

    /** Arbitrary difficulty score in [1.0, 3.0] — the valid platform range. */
    @Provide
    Arbitrary<Double> difficultyScore() {
        return Arbitraries.doubles().between(1.0, 3.0);
    }

    /** Builds a Paper with a fully populated paperDefinitionJson (questionIds array). */
    @Provide
    Arbitrary<Paper> validPaper() {
        Arbitrary<UUID> examId = Arbitraries.create(UUID::randomUUID);
        Arbitrary<String> shift = shiftId();
        Arbitrary<List<UUID>> questions = questionIdList();
        Arbitrary<Double> score = difficultyScore();

        return Combinators.combine(examId, shift, questions, score)
                .as((eid, sid, qids, ds) -> {
                    String questionIdsJson = qids.stream()
                            .map(id -> "\"" + id + "\"")
                            .collect(Collectors.joining(",", "[", "]"));
                    String definitionJson = "{\"questionIds\":" + questionIdsJson + "}";

                    Paper paper = Paper.builder()
                            .examId(eid)
                            .shiftId(sid)
                            .paperDefinitionJson(definitionJson)
                            .difficultyScore(ds)
                            .status("DRAFT")
                            .build();
                    setId(paper, UUID.randomUUID());
                    return paper;
                });
    }

    // -----------------------------------------------------------------------
    // Properties
    // -----------------------------------------------------------------------

    /**
     * Core round-trip property: parse(format(paper)) preserves all meaningful fields.
     */
    @Property(tries = 1000)
    @Label("parse(format(paper)) preserves examId, shiftId, difficultyScore, and questionIds")
    void roundTripPreservesAllFields(@ForAll("validPaper") Paper paper) {
        // Act
        String json = paperSerializer.format(paper);
        Paper parsed = paperSerializer.parse(json);

        // Assert — structural equality on all meaningful fields
        assertThat(parsed.getExamId())
                .as("examId must survive round-trip")
                .isEqualTo(paper.getExamId());

        assertThat(parsed.getShiftId())
                .as("shiftId must survive round-trip")
                .isEqualTo(paper.getShiftId());

        assertThat(parsed.getDifficultyScore())
                .as("difficultyScore must survive round-trip")
                .isEqualTo(paper.getDifficultyScore());

        // The parsed paper's definition JSON must contain all original question IDs
        List<UUID> originalIds = extractIds(paper);
        List<UUID> parsedIds = extractIds(parsed);
        assertThat(parsedIds)
                .as("questionIds must be preserved in order through round-trip")
                .containsExactlyElementsOf(originalIds);
    }

    /**
     * Idempotency property: serializing twice produces structurally equivalent output.
     * format(parse(format(paper))) must contain the same data as format(paper).
     */
    @Property(tries = 500)
    @Label("Second serialization is idempotent: format(parse(format(paper))) == format(paper)")
    void doubleSerializationIsIdempotent(@ForAll("validPaper") Paper paper) throws Exception {
        // Act
        String json1 = paperSerializer.format(paper);
        Paper parsed = paperSerializer.parse(json1);
        String json2 = paperSerializer.format(parsed);

        // Deserialize both as PaperDocument to compare semantically
        PaperDocument doc1 = objectMapper.readValue(json1, PaperDocument.class);
        PaperDocument doc2 = objectMapper.readValue(json2, PaperDocument.class);

        assertThat(doc2.getExamId())
                .as("examId must be identical after double serialization")
                .isEqualTo(doc1.getExamId());

        assertThat(doc2.getShiftId())
                .as("shiftId must be identical after double serialization")
                .isEqualTo(doc1.getShiftId());

        assertThat(doc2.getDifficultyScore())
                .as("difficultyScore must be identical after double serialization")
                .isEqualTo(doc1.getDifficultyScore());

        assertThat(doc2.getSchemaVersion())
                .as("schemaVersion must be preserved through double serialization")
                .isEqualTo(doc1.getSchemaVersion());

        assertThat(doc2.getQuestionIds())
                .as("questionIds must be identical after double serialization")
                .containsExactlyElementsOf(doc1.getQuestionIds());
    }

    /**
     * Property: format() always produces valid JSON that passes schema validation.
     */
    @Property(tries = 500)
    @Label("format() always produces output that passes schema validation")
    void formatAlwaysProducesValidDocument(@ForAll("validPaper") Paper paper) {
        String json = paperSerializer.format(paper);
        List<String> errors = paperSerializer.validate(json);

        assertThat(errors)
                .as("format() output must always pass schema validation, but got errors: %s", errors)
                .isEmpty();
    }

    /**
     * Property: format() never embeds question content — only IDs appear.
     * The serialized form must not contain fields that suggest question content
     * is stored (content, options, text, correctAnswer).
     */
    @Property(tries = 300)
    @Label("format() stores only question identifiers, not question content")
    void formatStoresOnlyQuestionIds(@ForAll("validPaper") Paper paper) {
        String json = paperSerializer.format(paper);

        // These fields would indicate question content leaked into the serialized paper
        assertThat(json)
                .as("Serialized paper must not contain question content fields")
                .doesNotContainIgnoringCase("\"content\"")
                .doesNotContainIgnoringCase("\"options\"")
                .doesNotContainIgnoringCase("\"correctAnswer\"")
                .doesNotContainIgnoringCase("\"questionText\"");
    }

    /**
     * Property: the schema version is always "1.0" in the formatted output.
     */
    @Property(tries = 200)
    @Label("format() always emits schemaVersion 1.0")
    void formatAlwaysEmitsCurrentSchemaVersion(@ForAll("validPaper") Paper paper) {
        String json = paperSerializer.format(paper);
        assertThat(json).contains("\"schemaVersion\":\"1.0\"");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private List<UUID> extractIds(Paper paper) {
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = objectMapper.readValue(
                    paper.getPaperDefinitionJson(),
                    java.util.Map.class);
            Object raw = map.get("questionIds");
            if (raw instanceof List<?> list) {
                return list.stream()
                        .map(o -> UUID.fromString(o.toString()))
                        .collect(Collectors.toList());
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
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
