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
 * Handles question updates with automatic versioning.
 * On every update, the old state is captured, the question is updated,
 * and a QuestionVersion record is created with the diff.
 *
 * Validates: Requirements 4.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class QuestionUpdateService {

    private final QuestionRepository questionRepository;
    private final QuestionVersioningService questionVersioningService;

    /**
     * Updates an existing question and creates a version record tracking the changes.
     *
     * @param questionId the question UUID to update
     * @param request    the update payload
     * @param authorId   UUID of the user performing the update
     * @param tenantId   tenant identifier
     * @return the updated question response
     * @throws EntityNotFoundException if the question does not exist
     */
    public QuestionResponse updateQuestion(UUID questionId, CreateQuestionRequest request, UUID authorId, String tenantId) {
        Question existing = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question not found: " + questionId));

        // Clone current state for diff comparison
        Question oldState = cloneQuestionState(existing);

        // Update fields from request
        existing.setSubject(request.getSubject());
        existing.setTopic(request.getTopic());
        existing.setSubtopic(request.getSubtopic());
        existing.setChapter(request.getChapter());
        existing.setDifficulty(request.getDifficulty().name());
        existing.setCognitiveLevel(request.getCognitiveLevel().name());
        existing.setQuestionType(request.getQuestionType().name());
        existing.setContent(request.getContent());
        existing.setAnswerKey(request.getAnswerKey());

        // Save updated question
        Question updated = questionRepository.save(existing);

        // Create version record
        questionVersioningService.createVersion(oldState, updated, authorId, tenantId);

        log.info("Question updated: id={}, author={}, tenant={}", questionId, authorId, tenantId);

        return toResponse(updated);
    }

    private Question cloneQuestionState(Question source) {
        return Question.builder()
                .subject(source.getSubject())
                .topic(source.getTopic())
                .subtopic(source.getSubtopic())
                .chapter(source.getChapter())
                .difficulty(source.getDifficulty())
                .cognitiveLevel(source.getCognitiveLevel())
                .questionType(source.getQuestionType())
                .content(source.getContent())
                .answerKey(source.getAnswerKey())
                .state(source.getState())
                .authorId(source.getAuthorId())
                .build();
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
