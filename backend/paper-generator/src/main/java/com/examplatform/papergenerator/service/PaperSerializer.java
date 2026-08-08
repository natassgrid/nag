package com.examplatform.papergenerator.service;

import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.dto.PaperDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for serializing and deserializing Paper entities.
 * Serializes to JSON with schema versioning; stores only question identifiers
 * (no question content) for security.
 *
 * Validates: Requirements 8.7, 28.1, 28.2, 28.3, 28.5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperSerializer {

    private static final String CURRENT_SCHEMA_VERSION = "1.0";
    private static final List<String> SUPPORTED_VERSIONS = List.of("1.0");

    private final ObjectMapper objectMapper;

    /**
     * Serializes a Paper entity to a JSON string with schema version.
     * Only question identifiers are stored — no content.
     *
     * @param paper the paper entity to serialize
     * @return JSON string representation of the paper document
     * @throws PaperSerializationException if serialization fails
     */
    public String format(Paper paper) {
        try {
            List<UUID> questionIds = extractQuestionIds(paper.getPaperDefinitionJson());

            PaperDocument document = PaperDocument.builder()
                    .schemaVersion(CURRENT_SCHEMA_VERSION)
                    .examId(paper.getExamId())
                    .shiftId(paper.getShiftId())
                    .questionIds(questionIds)
                    .difficultyScore(paper.getDifficultyScore())
                    .generatedAt(paper.getCreatedAt() != null ? paper.getCreatedAt() : Instant.now())
                    .build();

            return objectMapper.writeValueAsString(document);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize paper id={}: {}", paper.getId(), e.getMessage());
            throw new PaperSerializationException("Failed to serialize paper", e);
        }
    }

    /**
     * Deserializes a JSON string back to a Paper entity.
     * Validates the schema version before deserialization.
     *
     * @param json the JSON string to parse
     * @return Paper entity reconstructed from the document
     * @throws PaperSerializationException if parsing fails or schema version is unsupported
     */
    public Paper parse(String json) {
        try {
            PaperDocument document = objectMapper.readValue(json, PaperDocument.class);

            validateSchemaVersion(document.getSchemaVersion());

            Paper paper = Paper.builder()
                    .examId(document.getExamId())
                    .shiftId(document.getShiftId())
                    .paperDefinitionJson(objectMapper.writeValueAsString(
                            Map.of("questionIds", document.getQuestionIds())))
                    .difficultyScore(document.getDifficultyScore())
                    .status("DRAFT")
                    .build();

            return paper;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse paper JSON: {}", e.getMessage());
            throw new PaperSerializationException("Failed to parse paper JSON", e);
        }
    }

    /**
     * Validates a JSON string against the paper schema.
     * Returns a list of validation errors, or empty list if valid.
     *
     * @param json the JSON string to validate
     * @return list of validation error messages, empty if valid
     */
    public List<String> validate(String json) {
        // Guard: empty or blank input is not valid JSON
        if (json == null || json.isBlank()) {
            return List.of("Invalid JSON format: input is empty");
        }
        try {
            // Use JsonNode first so we can detect null / non-object documents
            // before attempting PaperDocument deserialization
            JsonNode root = objectMapper.readTree(json);
            if (root == null || root.isNull() || !root.isObject()) {
                return List.of("Invalid JSON format: document must be a JSON object");
            }

            PaperDocument document = objectMapper.treeToValue(root, PaperDocument.class);

            java.util.ArrayList<String> errors = new java.util.ArrayList<>();

            // schemaVersion: check the raw node — @Builder.Default fills it in at
            // Java level but the JSON field may be genuinely absent
            if (!root.has("schemaVersion") || root.get("schemaVersion").isNull()
                    || root.get("schemaVersion").asText().isBlank()) {
                errors.add("schemaVersion is required");
            } else if (!SUPPORTED_VERSIONS.contains(document.getSchemaVersion())) {
                errors.add("Unsupported schema version: " + document.getSchemaVersion()
                        + ". Supported versions: " + SUPPORTED_VERSIONS);
            }

            if (document.getExamId() == null) {
                errors.add("examId is required");
            }

            if (document.getShiftId() == null || document.getShiftId().isBlank()) {
                errors.add("shiftId is required");
            }

            if (document.getQuestionIds() == null || document.getQuestionIds().isEmpty()) {
                errors.add("questionIds must not be empty");
            }

            if (document.getGeneratedAt() == null) {
                errors.add("generatedAt is required");
            }

            return errors;
        } catch (JsonProcessingException e) {
            return List.of("Invalid JSON format: " + e.getOriginalMessage());
        }
    }

    private void validateSchemaVersion(String version) {
        if (version == null || !SUPPORTED_VERSIONS.contains(version)) {
            throw new PaperSerializationException(
                    "Unsupported schema version: " + version + ". Supported: " + SUPPORTED_VERSIONS);
        }
    }

    private List<UUID> extractQuestionIds(String paperDefinitionJson) {
        if (paperDefinitionJson == null || paperDefinitionJson.isBlank()) {
            return List.of();
        }
        try {
            Map<String, Object> definition = objectMapper.readValue(paperDefinitionJson,
                    new TypeReference<Map<String, Object>>() {});

            Object questionIdsObj = definition.get("questionIds");
            if (questionIdsObj instanceof List<?> list) {
                return list.stream()
                        .map(item -> {
                            if (item instanceof String s) {
                                return UUID.fromString(s);
                            }
                            return UUID.fromString(item.toString());
                        })
                        .toList();
            }

            // Fallback: try extracting from questions array
            Object questionsObj = definition.get("questions");
            if (questionsObj instanceof List<?> list) {
                return list.stream()
                        .filter(item -> item instanceof Map)
                        .map(item -> {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> q = (Map<String, Object>) item;
                            Object id = q.get("id");
                            return id != null ? UUID.fromString(id.toString()) : UUID.randomUUID();
                        })
                        .toList();
            }

            return List.of();
        } catch (JsonProcessingException e) {
            log.warn("Could not extract question IDs from paper definition: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Exception thrown when paper serialization/deserialization fails.
     */
    public static class PaperSerializationException extends RuntimeException {
        public PaperSerializationException(String message) {
            super(message);
        }

        public PaperSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
