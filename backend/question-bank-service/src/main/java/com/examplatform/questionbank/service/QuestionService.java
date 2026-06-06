package com.examplatform.questionbank.service;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.dto.CreateQuestionRequest;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.repository.QuestionRepository;
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
import java.util.stream.Collectors;

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
    private final KafkaTemplate<String, Object> kafkaTemplate;

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

        // Check similarity against existing PUBLISHED questions before creating Draft
        similarityDetectionService.checkSimilarity(request.getContent());

        // Generate per-question DEK key name (unique per question)
        String dekKeyName = "question-dek-" + UUID.randomUUID();

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
                .answerKey(request.getAnswerKey())
                .state("DRAFT")
                .encryptionKeyId(dekKeyName)
                .authorId(authorId)
                .build();

        // Set tenant context
        question.setTenantId(tenantId);

        // Persist (content & answerKey encrypted automatically by EncryptedFieldConverter)
        Question saved = questionRepository.save(question);

        log.info("Question created: id={}, type={}, author={}, tenant={}, dekKey={}",
                saved.getId(), saved.getQuestionType(), authorId, tenantId, dekKeyName);

        // Publish QUESTION_CREATED audit event (fire-and-forget)
        publishAuditEvent("QUESTION_CREATED", saved.getId(), authorId, tenantId,
                Map.of("questionType", saved.getQuestionType(), "state", saved.getState()));

        return toResponse(saved);
    }

    /**
     * Lists questions for a tenant with optional filters.
     *
     * @param subject    optional subject filter
     * @param topic      optional topic filter
     * @param difficulty optional difficulty filter
     * @param state      optional state filter
     * @param tenantId   tenant identifier
     * @return filtered list of question responses
     */
    @Transactional(readOnly = true)
    public List<QuestionResponse> listQuestions(String subject, String topic, String difficulty,
                                                String state, String tenantId) {
        List<Question> questions = questionRepository.findByTenantId(tenantId);

        return questions.stream()
                .filter(q -> subject == null || subject.isBlank() || subject.equalsIgnoreCase(q.getSubject()))
                .filter(q -> topic == null || topic.isBlank() || topic.equalsIgnoreCase(q.getTopic()))
                .filter(q -> difficulty == null || difficulty.isBlank() || difficulty.equalsIgnoreCase(q.getDifficulty()))
                .filter(q -> state == null || state.isBlank() || state.equalsIgnoreCase(q.getState()))
                .map(this::toResponse)
                .collect(Collectors.toList());
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

    private QuestionResponse toResponse(Question question) {
        LocalDateTime createdAt = question.getCreatedAt() != null
                ? LocalDateTime.ofInstant(question.getCreatedAt(), ZoneOffset.UTC)
                : null;

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
                .state(question.getState())
                .authorId(question.getAuthorId())
                .createdAt(createdAt)
                .build();
    }
}
