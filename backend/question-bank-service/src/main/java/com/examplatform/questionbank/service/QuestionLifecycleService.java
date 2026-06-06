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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
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

    private static final String AUDIT_TOPIC = "exam.audit.events";

    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
            "DRAFT", Set.of("REVIEW"),
            "REVIEW", Set.of("APPROVED", "DRAFT"),
            "APPROVED", Set.of("PUBLISHED"),
            "PUBLISHED", Set.of("ARCHIVED")
    );

    private final QuestionRepository questionRepository;
    private final ReviewWorkflowService reviewWorkflowService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

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

        // Publish QUESTION_STATE_TRANSITION audit event (fire-and-forget)
        publishAuditEvent(questionId, actorId, tenantId, currentState, targetState);

        // 6. Trigger review workflow processing
        reviewWorkflowService.processTransition(saved, currentState, targetState, actorId,
                request.getComments(), tenantId);

        // 7. Return response
        return toResponse(saved);
    }

    private void publishAuditEvent(UUID questionId, UUID actorId, String tenantId,
                                    String fromState, String toState) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "QUESTION_STATE_TRANSITION");
            event.put("questionId", questionId.toString());
            event.put("actorId", actorId.toString());
            event.put("tenantId", tenantId);
            event.put("fromState", fromState);
            event.put("toState", toState);
            event.put("occurredAt", Instant.now().toString());

            kafkaTemplate.send(AUDIT_TOPIC, questionId.toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish audit event [QUESTION_STATE_TRANSITION] for question [{}]: {}",
                                    questionId, ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Unexpected error publishing state transition audit event: {}", e.getMessage());
        }
    }

    /**
     * Approves a question in REVIEW state — transitions to APPROVED.
     *
     * @param questionId the question UUID
     * @param reviewerId UUID of the reviewer performing approval
     * @param tenantId   tenant identifier for access scoping
     * @return the updated question response
     * @throws EntityNotFoundException    if the question is not found
     * @throws InvalidTransitionException if the question is not in REVIEW state
     */
    public QuestionResponse approve(UUID questionId, UUID reviewerId, String tenantId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question not found: " + questionId));

        if (!"REVIEW".equals(question.getState())) {
            throw new InvalidTransitionException(question.getState(), "APPROVED");
        }

        question.setReviewerId(reviewerId);
        question.setState("APPROVED");
        Question saved = questionRepository.save(question);

        log.info("Question approved: id={}, reviewer={}, tenant={}", questionId, reviewerId, tenantId);

        publishAuditEvent(questionId, reviewerId, tenantId, "REVIEW", "APPROVED");
        reviewWorkflowService.processTransition(saved, "REVIEW", "APPROVED", reviewerId, null, tenantId);

        return toResponse(saved);
    }

    /**
     * Rejects a question in REVIEW state — transitions back to DRAFT for revision.
     *
     * @param questionId the question UUID
     * @param reviewerId UUID of the reviewer performing rejection
     * @param comments   reviewer comments explaining the rejection
     * @param tenantId   tenant identifier for access scoping
     * @return the updated question response
     * @throws EntityNotFoundException    if the question is not found
     * @throws InvalidTransitionException if the question is not in REVIEW state
     */
    public QuestionResponse reject(UUID questionId, UUID reviewerId, String comments, String tenantId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question not found: " + questionId));

        if (!"REVIEW".equals(question.getState())) {
            throw new InvalidTransitionException(question.getState(), "DRAFT");
        }

        question.setReviewerId(reviewerId);
        question.setState("DRAFT");
        Question saved = questionRepository.save(question);

        log.info("Question rejected: id={}, reviewer={}, tenant={}, comments={}", questionId, reviewerId, tenantId, comments);

        publishAuditEvent(questionId, reviewerId, tenantId, "REVIEW", "DRAFT");
        reviewWorkflowService.processTransition(saved, "REVIEW", "DRAFT", reviewerId, comments, tenantId);

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
