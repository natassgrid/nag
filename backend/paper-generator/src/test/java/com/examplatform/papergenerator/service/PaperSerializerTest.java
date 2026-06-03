package com.examplatform.papergenerator.service;

import com.examplatform.papergenerator.domain.Paper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PaperSerializer")
class PaperSerializerTest {

    private PaperSerializer paperSerializer;
    private ObjectMapper objectMapper;

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final String SHIFT_ID = "shift-001";
    private static final UUID Q1 = UUID.randomUUID();
    private static final UUID Q2 = UUID.randomUUID();
    private static final UUID Q3 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        paperSerializer = new PaperSerializer(objectMapper);
    }

    @Test
    @DisplayName("Format and parse round-trip preserves paper data")
    void formatAndParse_roundTrip_preservesData() {
        // Given
        String definitionJson = "{\"questionIds\": [\"" + Q1 + "\", \"" + Q2 + "\", \"" + Q3 + "\"]}";
        Paper original = Paper.builder()
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .paperDefinitionJson(definitionJson)
                .difficultyScore(0.75)
                .status("DRAFT")
                .build();

        // When
        String json = paperSerializer.format(original);
        Paper parsed = paperSerializer.parse(json);

        // Then
        assertThat(parsed.getExamId()).isEqualTo(EXAM_ID);
        assertThat(parsed.getShiftId()).isEqualTo(SHIFT_ID);
        assertThat(parsed.getDifficultyScore()).isEqualTo(0.75);
        assertThat(parsed.getStatus()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("Format includes schema version 1.0")
    void format_includesSchemaVersion() {
        // Given
        String definitionJson = "{\"questionIds\": [\"" + Q1 + "\"]}";
        Paper paper = Paper.builder()
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .paperDefinitionJson(definitionJson)
                .difficultyScore(0.5)
                .status("DRAFT")
                .build();

        // When
        String json = paperSerializer.format(paper);

        // Then
        assertThat(json).contains("\"schemaVersion\":\"1.0\"");
    }

    @Test
    @DisplayName("Format includes question IDs without content")
    void format_includesQuestionIds_withoutContent() {
        // Given
        String definitionJson = "{\"questionIds\": [\"" + Q1 + "\", \"" + Q2 + "\"]}";
        Paper paper = Paper.builder()
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .paperDefinitionJson(definitionJson)
                .difficultyScore(0.6)
                .status("DRAFT")
                .build();

        // When
        String json = paperSerializer.format(paper);

        // Then
        assertThat(json).contains(Q1.toString());
        assertThat(json).contains(Q2.toString());
        assertThat(json).doesNotContain("content");
        assertThat(json).doesNotContain("options");
    }

    @Test
    @DisplayName("Parse rejects unsupported schema version")
    void parse_unsupportedVersion_throwsException() {
        // Given
        String invalidJson = """
                {
                    "schemaVersion": "99.0",
                    "examId": "%s",
                    "shiftId": "%s",
                    "questionIds": ["%s"],
                    "difficultyScore": 0.5,
                    "generatedAt": "2024-01-01T00:00:00Z"
                }
                """.formatted(EXAM_ID, SHIFT_ID, Q1);

        // When / Then
        assertThatThrownBy(() -> paperSerializer.parse(invalidJson))
                .isInstanceOf(PaperSerializer.PaperSerializationException.class)
                .hasMessageContaining("Unsupported schema version");
    }

    @Test
    @DisplayName("Validate returns empty list for valid document")
    void validate_validDocument_returnsEmptyList() {
        // Given
        String validJson = """
                {
                    "schemaVersion": "1.0",
                    "examId": "%s",
                    "shiftId": "%s",
                    "questionIds": ["%s", "%s"],
                    "difficultyScore": 0.7,
                    "generatedAt": "2024-06-15T10:00:00Z"
                }
                """.formatted(EXAM_ID, SHIFT_ID, Q1, Q2);

        // When
        List<String> errors = paperSerializer.validate(validJson);

        // Then
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Validate returns errors for missing required fields")
    void validate_missingFields_returnsErrors() {
        // Given — missing examId, shiftId, questionIds, generatedAt
        String invalidJson = """
                {
                    "schemaVersion": "1.0"
                }
                """;

        // When
        List<String> errors = paperSerializer.validate(invalidJson);

        // Then
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("examId"));
        assertThat(errors).anyMatch(e -> e.contains("questionIds"));
        assertThat(errors).anyMatch(e -> e.contains("generatedAt"));
    }

    @Test
    @DisplayName("Validate returns error for unsupported schema version")
    void validate_unsupportedVersion_returnsError() {
        // Given
        String json = """
                {
                    "schemaVersion": "2.0",
                    "examId": "%s",
                    "shiftId": "%s",
                    "questionIds": ["%s"],
                    "difficultyScore": 0.5,
                    "generatedAt": "2024-01-01T00:00:00Z"
                }
                """.formatted(EXAM_ID, SHIFT_ID, Q1);

        // When
        List<String> errors = paperSerializer.validate(json);

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("Unsupported schema version");
    }

    @Test
    @DisplayName("Validate returns error for invalid JSON")
    void validate_invalidJson_returnsError() {
        // Given
        String invalidJson = "{ not valid json !!!";

        // When
        List<String> errors = paperSerializer.validate(invalidJson);

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("Invalid JSON format");
    }

    @Test
    @DisplayName("Parse valid JSON creates Paper with correct fields")
    void parse_validJson_createsPaper() {
        // Given
        String validJson = """
                {
                    "schemaVersion": "1.0",
                    "examId": "%s",
                    "shiftId": "%s",
                    "questionIds": ["%s", "%s", "%s"],
                    "difficultyScore": 0.85,
                    "generatedAt": "2024-06-15T10:00:00Z"
                }
                """.formatted(EXAM_ID, SHIFT_ID, Q1, Q2, Q3);

        // When
        Paper paper = paperSerializer.parse(validJson);

        // Then
        assertThat(paper.getExamId()).isEqualTo(EXAM_ID);
        assertThat(paper.getShiftId()).isEqualTo(SHIFT_ID);
        assertThat(paper.getDifficultyScore()).isEqualTo(0.85);
        assertThat(paper.getStatus()).isEqualTo("DRAFT");
        assertThat(paper.getPaperDefinitionJson()).contains(Q1.toString());
        assertThat(paper.getPaperDefinitionJson()).contains(Q2.toString());
        assertThat(paper.getPaperDefinitionJson()).contains(Q3.toString());
    }
}
