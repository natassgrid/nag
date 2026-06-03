package com.examplatform.questionbank.service;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.dto.TransitionRequest;
import com.examplatform.questionbank.exception.FourEyesPrincipleViolationException;
import com.examplatform.questionbank.exception.InvalidTransitionException;
import com.examplatform.questionbank.repository.QuestionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service implementing the question lifecycle finite state machine (FSM).
 * Enforces valid transitions and the four-eyes principle (reviewer ≠ approver).
 *
 * Valid transitions:
 *   DRAFT → REVIEW
 *   REVIEW → APPROVED
 *   REVIEW → DRAFT
 *   APPROVED → PUBLISHED
 *   PUBLISHED → ARCHIVED
 *
 * Validates: Requirements 4.6, 5.5
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class QuestionLifecycleService {

    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
            "DRAFT", Set.of("REVIEW"),
            "REVIEW", Set.of("APPROVED", "DRAFT"),
            "APPROVED", Set.of("PUBLISHED"),
            "PUBLISHED", Set.of("ARCHIVED")
    );

    private final QuestionRepository questionRepository;

    /**
     * Transitions a question to the requested target state.
     *
     * @param questionId the question UUID
     * @param request    the transition request containing the target state
     * @param actorId    UUID of the actor performing the transition
     * @param tenantId   tenant identifier for access scoping
     * @return the updated question response
     * @throws EntityNotFoundException               if the question is not found
     * @throws InvalidTransitionException            if the transition is not valid per the FSM
     * @throws FourEyesPrincipleViolationException   if the approver is the same as the reviewer
     */
    public QuestionResponse transition(UUID questionId, TransitionRequest request, UUID actorId, String tenantId) {
        // 1. Find question → 404 if not found
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question not found: " + questionId));

        String currentState = question.getState();
        String targetState = request.getTargetState().toUpperCase();

        // 2. Validate transition against FSM
        Set<String> allowedTargets = VALID_TRANSITIONS.get(currentState);
        if (allowedTargets == null || !allowedTargets.contains(targetState)) {
            throw new InvalidTransitionException(currentState, targetState);
        }

        // 3. Four-eyes principle enforcement
        if ("PUBLISHED".equals(targetState)) {
            // The actor publishing (approver) must not be the same as the reviewer
            if (question.getReviewerId() != null && question.getReviewerId().equals(actorId)) {
                throw new FourEyesPrincipleViolationException();
            }
        }

        // Store reviewer when transitioning REVIEW → APPROVED
        if ("REVIEW".equals(currentState) && "APPROVED".equals(targetState)) {
            question.setReviewerId(actorId);
        }

        // 4. Update state
        question.setState(targetState);

        // 5. Save
        Question saved = questionRepository.save(question);

        log.info("Question transitioned: id={}, from={}, to={}, actor={}, tenant={}",
                questionId, currentState, targetState, actorId, tenantId);

        // 6. Return response
        return toResponse(saved);
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
