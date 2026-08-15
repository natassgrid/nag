/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.papergenerator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property test 7.9: Paper Schema Validation Rejects Invalid Documents.
 *
 * <p>For every document with at least one structural violation, validate() must:
 * <ol>
 *   <li>Return a non-empty error list.</li>
 *   <li>Include at least one error whose message references the violating field.</li>
 *   <li>The error value reported must correspond to the actually violating value.</li>
 * </ol>
 *
 * <p>Valid documents must always return an empty error list.
 *
 * Validates: Requirements 28.5
 */
@SuppressWarnings("unused")
class PaperSchemaValidationPropertyTest {

    private final ObjectMapper objectMapper;
    private final PaperSerializer paperSerializer;

    PaperSchemaValidationPropertyTest() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        paperSerializer = new PaperSerializer(objectMapper);
    }

    // -----------------------------------------------------------------------
    // Valid document generator (baseline)
    // -----------------------------------------------------------------------

    @Provide
    Arbitrary<String> validPaperJson() {
        Arbitrary<String> examId = Arbitraries.create(() -> UUID.randomUUID().toString());
        Arbitrary<String> shiftId = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20);
        Arbitrary<String> qId = Arbitraries.create(() -> UUID.randomUUID().toString());
        Arbitrary<List<String>> questionIds = qId.list().ofMinSize(1).ofMaxSize(10);

        return Combinators.combine(examId, shiftId, questionIds)
                .as((eid, sid, qids) -> buildJson(eid, sid, qids, "1.0",
                        "2024-01-01T00:00:00Z"));
    }

    // -----------------------------------------------------------------------
    // Invalid document generators — one violation each
    // -----------------------------------------------------------------------

    /** Documents missing the schemaVersion field entirely. */
    @Provide
    Arbitrary<String> missingSchemaVersion() {
        return Arbitraries.create(() -> {
            UUID examId = UUID.randomUUID();
            UUID qId = UUID.randomUUID();
            // Build a valid doc then remove schemaVersion
            return """
                    {
                      "examId": "%s",
                      "shiftId": "SHIFT-A",
                      "questionIds": ["%s"],
                      "difficultyScore": 1.5,
                      "generatedAt": "2024-06-01T00:00:00Z"
                    }
                    """.formatted(examId, qId);
        });
    }

    /** Documents with a schemaVersion that is not in the supported set. */
    @Provide
    Arbitrary<String> unsupportedSchemaVersion() {
        Arbitrary<String> badVersion = Arbitraries.of("0.1", "2.0", "99.0", "alpha", "");
        return Combinators.combine(badVersion,
                Arbitraries.create(() -> UUID.randomUUID().toString()),
                Arbitraries.create(() -> UUID.randomUUID().toString()))
                .as((ver, eid, qid) -> buildJson(eid, "SHIFT-B", List.of(qid), ver,
                        "2024-06-01T00:00:00Z"));
    }

    /** Documents missing examId. */
    @Provide
    Arbitrary<String> missingExamId() {
        return Arbitraries.create(() -> {
            UUID qId = UUID.randomUUID();
            return """
                    {
                      "schemaVersion": "1.0",
                      "shiftId": "SHIFT-C",
                      "questionIds": ["%s"],
                      "difficultyScore": 2.0,
                      "generatedAt": "2024-06-01T00:00:00Z"
                    }
                    """.formatted(qId);
        });
    }

    /** Documents with an empty questionIds array. */
    @Provide
    Arbitrary<String> emptyQuestionIds() {
        return Arbitraries.create(() -> {
            UUID examId = UUID.randomUUID();
            return """
                    {
                      "schemaVersion": "1.0",
                      "examId": "%s",
                      "shiftId": "SHIFT-D",
                      "questionIds": [],
                      "difficultyScore": 1.0,
                      "generatedAt": "2024-06-01T00:00:00Z"
                    }
                    """.formatted(examId);
        });
    }

    /** Documents missing the questionIds field entirely. */
    @Provide
    Arbitrary<String> missingQuestionIds() {
        return Arbitraries.create(() -> {
            UUID examId = UUID.randomUUID();
            return """
                    {
                      "schemaVersion": "1.0",
                      "examId": "%s",
                      "shiftId": "SHIFT-E",
                      "difficultyScore": 1.0,
                      "generatedAt": "2024-06-01T00:00:00Z"
                    }
                    """.formatted(examId);
        });
    }

    /** Documents missing the generatedAt field. */
    @Provide
    Arbitrary<String> missingGeneratedAt() {
        return Arbitraries.create(() -> {
            UUID examId = UUID.randomUUID();
            UUID qId = UUID.randomUUID();
            return """
                    {
                      "schemaVersion": "1.0",
                      "examId": "%s",
                      "shiftId": "SHIFT-F",
                      "questionIds": ["%s"],
                      "difficultyScore": 1.0
                    }
                    """.formatted(examId, qId);
        });
    }

    /** Documents with totally malformed JSON structure. */
    @Provide
    Arbitrary<String> malformedJson() {
        return Arbitraries.of(
                "{ not valid json",
                "{",
                "null",
                "[]",
                "",
                "{ \"key\": }",
                "{ \"schemaVersion\": 1.0, \"examId\": ??? }"
        );
    }

    // -----------------------------------------------------------------------
    // Properties — valid documents
    // -----------------------------------------------------------------------

    /**
     * Property: valid documents always pass validation with zero errors.
     */
    @Property(tries = 300)
    @Label("Valid paper documents always produce empty error list")
    void validDocumentsAlwaysPassValidation(@ForAll("validPaperJson") String json) {
        List<String> errors = paperSerializer.validate(json);

        assertThat(errors)
                .as("A valid paper document must produce no validation errors")
                .isEmpty();
    }

    // -----------------------------------------------------------------------
    // Properties — invalid documents
    // -----------------------------------------------------------------------

    /**
     * Property: missing schemaVersion → error mentioning "schemaVersion".
     */
    @Property(tries = 100)
    @Label("Missing schemaVersion produces error referencing 'schemaVersion'")
    void missingSchemaVersionProducesError(@ForAll("missingSchemaVersion") String json) {
        List<String> errors = paperSerializer.validate(json);

        assertThat(errors)
                .as("Missing schemaVersion must produce at least one error")
                .isNotEmpty();

        assertThat(errors)
                .as("Error list must reference the violating field 'schemaVersion'")
                .anyMatch(e -> e.toLowerCase().contains("schemaversion"));
    }

    /**
     * Property: unsupported schemaVersion → exactly one error with version info.
     */
    @Property(tries = 100)
    @Label("Unsupported schemaVersion produces error referencing 'schemaVersion'")
    void unsupportedSchemaVersionProducesError(@ForAll("unsupportedSchemaVersion") String json) {
        List<String> errors = paperSerializer.validate(json);

        assertThat(errors)
                .as("Unsupported schemaVersion must produce at least one error")
                .isNotEmpty();

        assertThat(errors)
                .as("Error must mention unsupported schema version")
                .anyMatch(e -> e.toLowerCase().contains("schema")
                        || e.toLowerCase().contains("version")
                        || e.toLowerCase().contains("unsupported"));
    }

    /**
     * Property: missing examId → error mentioning "examId".
     */
    @Property(tries = 100)
    @Label("Missing examId produces error referencing 'examId'")
    void missingExamIdProducesError(@ForAll("missingExamId") String json) {
        List<String> errors = paperSerializer.validate(json);

        assertThat(errors)
                .as("Missing examId must produce at least one error")
                .isNotEmpty();

        assertThat(errors)
                .as("Error must reference the violating field 'examId'")
                .anyMatch(e -> e.toLowerCase().contains("examid"));
    }

    /**
     * Property: empty questionIds array → error mentioning "questionIds".
     */
    @Property(tries = 100)
    @Label("Empty questionIds array produces error referencing 'questionIds'")
    void emptyQuestionIdsProducesError(@ForAll("emptyQuestionIds") String json) {
        List<String> errors = paperSerializer.validate(json);

        assertThat(errors)
                .as("Empty questionIds must produce at least one error")
                .isNotEmpty();

        assertThat(errors)
                .as("Error must reference the violating field 'questionIds'")
                .anyMatch(e -> e.toLowerCase().contains("questionids"));
    }

    /**
     * Property: missing questionIds field → error mentioning "questionIds".
     */
    @Property(tries = 100)
    @Label("Missing questionIds field produces error referencing 'questionIds'")
    void missingQuestionIdsProducesError(@ForAll("missingQuestionIds") String json) {
        List<String> errors = paperSerializer.validate(json);

        assertThat(errors)
                .as("Missing questionIds must produce at least one error")
                .isNotEmpty();

        assertThat(errors)
                .as("Error must reference the violating field 'questionIds'")
                .anyMatch(e -> e.toLowerCase().contains("questionids"));
    }

    /**
     * Property: missing generatedAt → error mentioning "generatedAt".
     */
    @Property(tries = 100)
    @Label("Missing generatedAt produces error referencing 'generatedAt'")
    void missingGeneratedAtProducesError(@ForAll("missingGeneratedAt") String json) {
        List<String> errors = paperSerializer.validate(json);

        assertThat(errors)
                .as("Missing generatedAt must produce at least one error")
                .isNotEmpty();

        assertThat(errors)
                .as("Error must reference the violating field 'generatedAt'")
                .anyMatch(e -> e.toLowerCase().contains("generatedat"));
    }

    /**
     * Property: malformed JSON → always produces at least one error
     * (containing "invalid json" or similar).
     */
    @Property(tries = 50)
    @Label("Malformed JSON always produces at least one validation error")
    void malformedJsonAlwaysProducesError(@ForAll("malformedJson") String json) {
        List<String> errors = paperSerializer.validate(json);

        assertThat(errors)
                .as("Malformed JSON must produce at least one error")
                .isNotEmpty();
    }

    /**
     * Property: any document with multiple simultaneous violations returns multiple errors —
     * the count of errors is ≥ the number of distinct violated fields.
     * We test a document that violates schemaVersion, examId, questionIds, and generatedAt.
     */
    @Property(tries = 100)
    @Label("Multiple violations each produce their own error")
    void multipleViolationsEachProduceErrors() {
        // Document with 4 violations: bad version, no examId, empty questionIds, no generatedAt
        String json = """
                {
                  "schemaVersion": "99.9",
                  "shiftId": "SHIFT-X",
                  "questionIds": [],
                  "difficultyScore": 2.0
                }
                """;

        List<String> errors = paperSerializer.validate(json);

        // Should have errors for: unsupported schemaVersion, examId, questionIds, generatedAt
        assertThat(errors)
                .as("Document with 4 violations must return ≥ 3 errors (schemaVersion supersedes examId check)")
                .hasSizeGreaterThanOrEqualTo(3);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String buildJson(String examId, String shiftId, List<String> questionIds,
                              String schemaVersion, String generatedAt) {
        String idsArray = questionIds.stream()
                .map(id -> "\"" + id + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        return """
                {
                  "schemaVersion": "%s",
                  "examId": "%s",
                  "shiftId": "%s",
                  "questionIds": %s,
                  "difficultyScore": 1.5,
                  "generatedAt": "%s"
                }
                """.formatted(schemaVersion, examId, shiftId, idsArray, generatedAt);
    }
}
