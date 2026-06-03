package com.examplatform.questionbank.service;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.dto.CreateQuestionRequest;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.repository.QuestionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    private final QuestionRepository questionRepository;
    private final SimilarityDetectionService similarityDetectionService;

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

        return toResponse(saved);
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
