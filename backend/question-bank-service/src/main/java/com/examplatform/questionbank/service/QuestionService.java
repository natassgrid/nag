package com.examplatform.questionbank.service;
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


import com.examplatform.questionbank.ai.embedding.EmbeddingService;
import com.examplatform.questionbank.ai.similarity.SimilarityCheckResult;
import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.domain.enums.QuestionType;
import com.examplatform.questionbank.dto.CreateQuestionRequest;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.exception.SimilarQuestionException;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.util.EmbeddingUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service layer for question CRUD operations.
 * Handles question creation with per-question DEK encryption,
 * metadata validation, and Draft state persistence.
 *
 * Validates: Requirements 4.1, 4.2, 4.3, 4.5
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class QuestionService {

    private static final String AUDIT_TOPIC = "exam.audit.events";

    private final QuestionRepository questionRepository;
    private final SimilarityDetectionService similarityDetectionService;
    private final EmbeddingService embeddingService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @org.springframework.beans.factory.annotation.Value("${app.encryption.enabled:false}")
    private boolean encryptionEnabled;

    /**
     * Creates a new question in DRAFT state with a unique per-question DEK.
     *
     * @param request   validated creation request
     * @param authorId  UUID of the question author (from JWT sub claim)
     * @param tenantId  tenant identifier (from X-Tenant-Id header)
     * @return the created question response with decrypted content
     */
    public QuestionResponse createQuestion(CreateQuestionRequest request, UUID authorId, String tenantId) {
        // Validate question type is in the supported set
        if (request.getQuestionType() == null) {
            throw new IllegalArgumentException("Question type must be one of the supported types: "
                    + "SINGLE_MCQ, MULTI_MCQ, NUMERICAL, DESCRIPTIVE, MATRIX_MATCH, ASSERTION_REASON, CODING, CASE_STUDY");
        }

        // Check similarity against existing questions in same subject+tenant (FR-2)
        // Uses enforceNoDuplicate: throws SimilarQuestionException on > 0.92, returns WARN for 0.85–0.92
        // NFR-2: If LLM/embedding service is unavailable, question creation still succeeds with warning logged
        SimilarityCheckResult similarityResult = null;
        try {
            similarityResult = similarityDetectionService.enforceNoDuplicate(
                    request.getContent(), request.getSubject(), tenantId);
        } catch (SimilarQuestionException e) {
            // Near-duplicate detected (> 0.92) — propagate to reject creation
            throw e;
        } catch (Exception e) {
            // LLM/embedding service unavailable — proceed without duplicate detection (NFR-2)
            log.warn("Similarity check unavailable during question creation. " +
                    "Proceeding without duplicate detection. Reason: {}", e.getMessage());
        }

        // Generate per-question DEK key name only when encryption is enabled
        String dekKeyName = encryptionEnabled ? "question-dek-" + UUID.randomUUID() : null;

        // Validate and serialize options for MCQ/MSQ
        String answerKey = request.getAnswerKey();
        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            var options = request.getOptions();
            // Validate option count (2-6)
            if (options.size() < 2 || options.size() > 5) {
                throw new IllegalArgumentException("MCQ/MSQ questions must have between 2 and 5 options");
            }
            // Assign option IDs A-F based on position
            String[] ids = {"A", "B", "C", "D", "E", "F"};
            for (int i = 0; i < options.size(); i++) {
                options.get(i).setId(ids[i]);
            }
            // Validate correct options
            long correctCount = options.stream().filter(o -> o.isCorrect()).count();
            QuestionType questionType = request.getQuestionType();
            if (questionType == QuestionType.SINGLE_MCQ) {
                if (correctCount != 1) {
                    throw new IllegalArgumentException("MCQ questions must have exactly one correct option");
                }
            } else if (questionType == QuestionType.MULTI_MCQ) {
                if (correctCount < 1) {
                    throw new IllegalArgumentException("MSQ questions must have at least one correct option");
                }
            }
            // Serialize to JSON
            try {
                answerKey = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(options);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize options", e);
            }
        }

        // Build Question entity
        Question question = Question.builder()
                .subject(request.getSubject())
                .topic(request.getTopic())
                .subtopic(request.getSubtopic())
                .chapter(request.getChapter())
                .difficulty(request.getDifficulty().name())
                .cognitiveLevel(request.getCognitiveLevel().name())
                .questionType(request.getQuestionType().name())
                .content(request.getContent())
                .answerKey(answerKey)
                .options(request.getOptions())
                .explanation(request.getExplanation())
                .references(request.getReferences())
                .state("DRAFT")
                .encryptionKeyId(dekKeyName)
                .authorId(authorId)
                .build();

        // Set tenant context
        question.setTenantId(tenantId);

        // Persist — EncryptedFieldConverter encrypts content/answerKey only when app.encryption.enabled=true
        Question saved = questionRepository.save(question);

        // Generate and store embedding via native query (FR-1)
        // Column is insertable=false/updatable=false so we use native SQL with halfvec cast.
        // NFR-2: If LLM service is unavailable, question creation still succeeds without embedding
        try {
            float[] embedding = embeddingService.embed(request.getContent());
            if (embedding != null && embedding.length > 0) {
                questionRepository.updateEmbedding(saved.getId(), EmbeddingUtils.embeddingToString(embedding));
                log.debug("Embedding generated and stored for question: id={}", saved.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to generate embedding for question id={}. " +
                    "Question created without embedding. Reason: {}", saved.getId(), e.getMessage());
        }

        log.info("Question created: id={}, type={}, author={}, tenant={}, encrypted={}",
                saved.getId(), saved.getQuestionType(), authorId, tenantId, encryptionEnabled);

        // Publish QUESTION_CREATED audit event (fire-and-forget)
        publishAuditEvent("QUESTION_CREATED", saved.getId(), authorId, tenantId,
                Map.of("questionType", saved.getQuestionType(), "state", saved.getState()));

        // Build response with similarity warnings if applicable (FR-2: flag for human review)
        QuestionResponse response = toResponse(saved);
        if (similarityResult != null && similarityResult.status() == SimilarityCheckResult.Status.WARN) {
            List<QuestionResponse.SimilarQuestionWarning> warnings = similarityResult.similarQuestions().stream()
                    .map(sq -> QuestionResponse.SimilarQuestionWarning.builder()
                            .questionId(sq.questionId())
                            .similarity(sq.similarity())
                            .contentSnippet(sq.content())
                            .build())
                    .toList();
            response.setWarnings(warnings);
            log.info("Question created with similarity warnings: id={}, warningCount={}",
                    saved.getId(), warnings.size());
        }

        return response;
    }

    /**
     * Lists questions for a tenant with optional filters and pagination.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<QuestionResponse> listQuestions(
            String subject, String topic, String difficulty, String state,
            String search, int page, int size, String tenantId) {

        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size,
                        org.springframework.data.domain.Sort.by("createdAt").descending());

        org.springframework.data.jpa.domain.Specification<Question> spec =
                org.springframework.data.jpa.domain.Specification.where(tenantEquals(tenantId));

        if (subject != null && !subject.isBlank()) {
            spec = spec.and(fieldEquals("subject", subject));
        }
        if (topic != null && !topic.isBlank()) {
            spec = spec.and(fieldEquals("topic", topic));
        }
        if (difficulty != null && !difficulty.isBlank()) {
            spec = spec.and(fieldEquals("difficulty", difficulty));
        }
        if (state != null && !state.isBlank()) {
            spec = spec.and(fieldEquals("state", state));
        }
        if (search != null && !search.isBlank()) {
            spec = spec.and(searchLike(search));
        }

        return questionRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private org.springframework.data.jpa.domain.Specification<Question> tenantEquals(String tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    private org.springframework.data.jpa.domain.Specification<Question> fieldEquals(String field, String value) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get(field)), value.toLowerCase());
    }

    private org.springframework.data.jpa.domain.Specification<Question> searchLike(String search) {
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("subject")), pattern),
                cb.like(cb.lower(root.get("topic")), pattern),
                cb.like(cb.lower(root.get("content")), pattern)
        );
    }

    /**
     * Retrieves a question by its ID.
     *
     * @param questionId the question UUID
     * @return the question response with decrypted content
     * @throws EntityNotFoundException if the question does not exist
     */
    @Transactional(readOnly = true)
    public QuestionResponse getQuestion(UUID questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question not found: " + questionId));
        return toResponse(question);
    }

    /**
     * Submits a DRAFT question for review — transitions state from DRAFT to REVIEW.
     *
     * @param questionId the question UUID
     * @param authorId   UUID of the question author
     * @param tenantId   tenant identifier
     * @return the updated question response
     * @throws EntityNotFoundException   if the question is not found
     * @throws IllegalStateException     if the question is not in DRAFT state
     * @throws IllegalArgumentException  if the caller is not the author
     */
    public QuestionResponse submitForReview(UUID questionId, UUID authorId, String tenantId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question not found: " + questionId));

        if (!question.getAuthorId().equals(authorId)) {
            throw new IllegalArgumentException("Only the question author can submit for review");
        }

        if (!"DRAFT".equals(question.getState())) {
            throw new IllegalStateException("Question must be in DRAFT state to submit for review. Current state: " + question.getState());
        }

        question.setState("REVIEW");
        Question saved = questionRepository.save(question);

        log.info("Question submitted for review: id={}, author={}, tenant={}", questionId, authorId, tenantId);

        publishAuditEvent("QUESTION_SUBMITTED_FOR_REVIEW", saved.getId(), authorId, tenantId,
                Map.of("fromState", "DRAFT", "toState", "REVIEW"));

        return toResponse(saved);
    }

    private void publishAuditEvent(String eventType, UUID questionId, UUID actorId,
                                    String tenantId, Map<String, Object> extra) {
        try {
            Map<String, Object> event = new java.util.HashMap<>();
            event.put("eventType", eventType);
            event.put("questionId", questionId.toString());
            event.put("actorId", actorId.toString());
            event.put("tenantId", tenantId);
            event.put("occurredAt", Instant.now().toString());
            if (extra != null) {
                event.putAll(extra);
            }

            kafkaTemplate.send(AUDIT_TOPIC, questionId.toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish audit event [type={}] for question [{}]: {}",
                                    eventType, questionId, ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Unexpected error publishing audit event [type={}]: {}", eventType, e.getMessage());
        }
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private QuestionResponse toResponse(Question question) {
        LocalDateTime createdAt = question.getCreatedAt() != null
                ? LocalDateTime.ofInstant(question.getCreatedAt(), ZoneOffset.UTC)
                : null;

        // Use the options field directly from the entity (JSONB column).
        // Fall back to parsing from answerKey JSON for legacy data.
        java.util.List<com.examplatform.questionbank.dto.QuestionOption> options = question.getOptions();
        if ((options == null || options.isEmpty())) {
            String questionType = question.getQuestionType();
            if (questionType != null && (questionType.equals("SINGLE_MCQ") || questionType.equals("MULTI_MCQ"))
                    && question.getAnswerKey() != null && question.getAnswerKey().startsWith("[")) {
                try {
                    options = MAPPER.readValue(question.getAnswerKey(),
                            new com.fasterxml.jackson.core.type.TypeReference<
                                    java.util.List<com.examplatform.questionbank.dto.QuestionOption>>() {});
                } catch (Exception ignored) {}
            }
        }

        return QuestionResponse.builder()
                .id(question.getId())
                .subject(question.getSubject())
                .topic(question.getTopic())
                .subtopic(question.getSubtopic())
                .chapter(question.getChapter())
                .difficulty(question.getDifficulty())
                .cognitiveLevel(question.getCognitiveLevel())
                .questionType(question.getQuestionType())
                .content(question.getContent())
                .answerKey(question.getAnswerKey())
                .explanation(question.getExplanation())
                .references(question.getReferences())
                .state(question.getState())
                .authorId(question.getAuthorId())
                .createdAt(createdAt)
                .options(options)
                .build();
    }
}
