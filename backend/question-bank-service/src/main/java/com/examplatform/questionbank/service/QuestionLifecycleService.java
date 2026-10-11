/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

package com.examplatform.questionbank.service;

import static com.examplatform.shared.constants.MessagingConstants.AUDIT_EVENTS_TOPIC;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.dto.BulkTransitionItemResult;
import com.examplatform.questionbank.dto.BulkTransitionRequest;
import com.examplatform.questionbank.dto.BulkTransitionResponse;
import com.examplatform.questionbank.dto.QuestionAnalytics;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.dto.TransitionRequest;
import com.examplatform.questionbank.exception.FourEyesPrincipleViolationException;
import com.examplatform.questionbank.exception.InvalidTransitionException;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.shared.constants.QuestionStateConstants;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.messaging.EventPublisher;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementing the question lifecycle finite state machine (FSM).
 * Enforces valid transitions and the four-eyes principle (reviewer != approver).
 *
 * Valid transitions:
 *   DRAFT -> REVIEW
 *   REVIEW -> APPROVED
 *   REVIEW -> DRAFT
 *   APPROVED -> PUBLISHED
 *   PUBLISHED -> ARCHIVED
 *
 * Validates: Requirements 4.6, 5.5
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(noRollbackFor = {
        InvalidTransitionException.class,
        FourEyesPrincipleViolationException.class,
        EntityNotFoundException.class,
        IllegalArgumentException.class
})
public class QuestionLifecycleService {

    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
        QuestionStateConstants.DRAFT, Set.of(QuestionStateConstants.REVIEW),
        QuestionStateConstants.REVIEW, Set.of(QuestionStateConstants.APPROVED, QuestionStateConstants.DRAFT),
        QuestionStateConstants.APPROVED, Set.of(QuestionStateConstants.PUBLISHED),
        QuestionStateConstants.PUBLISHED, Set.of(QuestionStateConstants.ARCHIVED)
    );
    public static final String QUESTION_NOT_FOUND = "Question not found: ";

    private final QuestionRepository questionRepository;
    private final ReviewWorkflowService reviewWorkflowService;
    private final EventPublisher eventPublisher;

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
        // 1. Find question -> 404 if not found
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException(QUESTION_NOT_FOUND + questionId));

        String currentState = question.getState();
        String targetState = request.getTargetState().toUpperCase();

        // 2. Validate transition against FSM
        Set<String> allowedTargets = VALID_TRANSITIONS.get(currentState);
        if (allowedTargets == null || !allowedTargets.contains(targetState)) {
            throw new InvalidTransitionException(currentState, targetState);
        }

        // 3. Four-eyes principle enforcement
        if (QuestionStateConstants.PUBLISHED.equals(targetState) && question.getReviewerId() != null && question.getReviewerId().equals(actorId)) {
                throw new FourEyesPrincipleViolationException();
            }


        // Store reviewer when transitioning REVIEW -> APPROVED
        if (QuestionStateConstants.REVIEW.equals(currentState) && QuestionStateConstants.APPROVED.equals(targetState)) {
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

    /**
     * Executes bulk transitions on a collection of questions.
     * Enforces the FSM and four-eyes principle on each item individually without failing the entire batch.
     * Capped at 100 questions per batch.
     *
     * @param request  bulk transition request
     * @param actorId  UUID of the actor performing the transition
     * @param tenantId tenant identifier
     * @return summary response containing individual results
     */
    public BulkTransitionResponse bulkTransition(BulkTransitionRequest request, UUID actorId, String tenantId) {
        if (request.getQuestionIds() == null || request.getQuestionIds().isEmpty()) {
            return BulkTransitionResponse.builder()
                    .totalRequested(0)
                    .successCount(0)
                    .failureCount(0)
                    .results(List.of())
                    .build();
        }

        if (request.getQuestionIds().size() > 100) {
            throw new IllegalArgumentException("Bulk transition batch cannot exceed 100 questions");
        }

        List<BulkTransitionItemResult> itemResults = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        TransitionRequest itemReq = TransitionRequest.builder()
                .targetState(request.getTargetState())
                .comments(request.getReason())
                .build();

        for (UUID questionId : request.getQuestionIds()) {
            try {
                Question question = questionRepository.findById(questionId)
                        .orElseThrow(() -> new EntityNotFoundException(QUESTION_NOT_FOUND + questionId));
                String prevState = question.getState();

                // If targetState is APPROVED, enforce four-eyes: approver cannot be the author
                if (QuestionStateConstants.APPROVED.equalsIgnoreCase(request.getTargetState()) &&
                    question.getAuthorId() != null && question.getAuthorId().equals(actorId)) {
                        throw new FourEyesPrincipleViolationException("Author cannot approve their own question");
                    }


                QuestionResponse resp = transition(questionId, itemReq, actorId, tenantId);
                successCount++;
                itemResults.add(BulkTransitionItemResult.builder()
                        .questionId(questionId)
                        .success(true)
                        .previousState(prevState)
                        .newState(resp.getState())
                        .build());
            } catch (Exception e) {
                log.warn("Failed to transition question {} to {} in bulk batch: {}",
                        questionId, request.getTargetState(), e.getMessage());
                failureCount++;

                String prevState = null;
                try {
                    prevState = questionRepository.findById(questionId).map(Question::getState).orElse(null);
                } catch (Exception _) {
                    // Just ignore
                }

                itemResults.add(BulkTransitionItemResult.builder()
                        .questionId(questionId)
                        .success(false)
                        .previousState(prevState)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        return BulkTransitionResponse.builder()
                .totalRequested(request.getQuestionIds().size())
                .successCount(successCount)
                .failureCount(failureCount)
                .results(itemResults)
                .build();
    }

    private void publishAuditEvent(UUID questionId, UUID actorId, String tenantId,
                                    String fromState, String toState) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "QUESTION_STATE_TRANSITION");
            event.put(SharedConstants.QUESTION_ID, questionId.toString());
            event.put("actorId", actorId.toString());
            event.put(SharedConstants.TENANT_ID, tenantId);
            event.put("fromState", fromState);
            event.put("toState", toState);
            event.put("occurredAt", Instant.now().toString());

            eventPublisher.publish(AUDIT_EVENTS_TOPIC, questionId.toString(), event);
        } catch (Exception e) {
            log.error("Unexpected error publishing state transition audit event: {}", e.getMessage());
        }
    }

    /**
     * Approves a question in REVIEW state — transitions to APPROVED.
     * Enforces the Four-Eyes Principle: the reviewer (approver) cannot be the question's author.
     *
     * @param questionId the question UUID
     * @param reviewerId UUID of the reviewer performing approval
     * @param tenantId   tenant identifier for access scoping
     * @return the updated question response
     * @throws EntityNotFoundException             if the question is not found
     * @throws InvalidTransitionException          if the question is not in REVIEW state
     * @throws FourEyesPrincipleViolationException if the reviewer is also the question author
     */
    public QuestionResponse approve(UUID questionId, UUID reviewerId, String tenantId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException(QUESTION_NOT_FOUND + questionId));

        if (!QuestionStateConstants.REVIEW.equals(question.getState())) {
            throw new InvalidTransitionException(question.getState(), QuestionStateConstants.APPROVED);
        }

        // Four-eyes principle: reviewer cannot be the author
        if (question.getAuthorId() != null && question.getAuthorId().equals(reviewerId)) {
            throw new FourEyesPrincipleViolationException();
        }

        question.setReviewerId(reviewerId);
        question.setState(QuestionStateConstants.APPROVED);
        Question saved = questionRepository.save(question);

        log.info("Question approved: id={}, reviewer={}, tenant={}", questionId, reviewerId, tenantId);

        publishAuditEvent(questionId, reviewerId, tenantId, QuestionStateConstants.REVIEW,
            QuestionStateConstants.APPROVED);
        reviewWorkflowService.processTransition(saved, QuestionStateConstants.REVIEW, QuestionStateConstants.APPROVED, reviewerId, null, tenantId);

        return toResponse(saved);
    }

    /**
     * Publishes an APPROVED question — transitions to PUBLISHED.
     * Enforces the Four-Eyes Principle: the publisher cannot be the reviewer who approved it.
     *
     * @param questionId  the question UUID
     * @param publisherId UUID of the publisher
     * @param tenantId    tenant identifier for access scoping
     * @return the updated question response
     * @throws EntityNotFoundException             if the question is not found
     * @throws InvalidTransitionException          if the question is not in APPROVED state
     * @throws FourEyesPrincipleViolationException if the publisher is also the reviewer
     */
    public QuestionResponse publish(UUID questionId, UUID publisherId, String tenantId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException(QUESTION_NOT_FOUND + questionId));

        if (!QuestionStateConstants.APPROVED.equals(question.getState())) {
            throw new InvalidTransitionException(question.getState(), QuestionStateConstants.PUBLISHED);
        }

        // Four-eyes principle: publisher cannot be the reviewer who approved it
        if (question.getReviewerId() != null && question.getReviewerId().equals(publisherId)) {
            throw new FourEyesPrincipleViolationException();
        }

        question.setState(QuestionStateConstants.PUBLISHED);
        Question saved = questionRepository.save(question);

        log.info("Question published: id={}, publisher={}, tenant={}", questionId, publisherId, tenantId);

        publishAuditEvent(questionId, publisherId, tenantId, QuestionStateConstants.APPROVED,
            QuestionStateConstants.PUBLISHED);
        reviewWorkflowService.processTransition(saved, QuestionStateConstants.APPROVED,
            QuestionStateConstants.PUBLISHED, publisherId, null, tenantId);

        return toResponse(saved);
    }

    /**
     * Rejects a question in REVIEW state — transitions back to DRAFT for revision.
     * Reviewer comments are persisted on the Question entity so authors can see them.
     *
     * @param questionId the question UUID
     * @param reviewerId UUID of the reviewer performing rejection
     * @param comments   reviewer comments explaining the rejection (required)
     * @param tenantId   tenant identifier for access scoping
     * @return the updated question response
     * @throws EntityNotFoundException    if the question is not found
     * @throws InvalidTransitionException if the question is not in REVIEW state
     */
    public QuestionResponse reject(UUID questionId, UUID reviewerId, String comments, String tenantId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException(QUESTION_NOT_FOUND + questionId));

        if (!QuestionStateConstants.REVIEW.equals(question.getState())) {
            throw new InvalidTransitionException(question.getState(), QuestionStateConstants.DRAFT);
        }

        question.setReviewerId(reviewerId);
        question.setReviewComments(comments);
        question.setState(QuestionStateConstants.DRAFT);
        Question saved = questionRepository.save(question);

        log.info("Question rejected: id={}, reviewer={}, tenant={}, comments={}", questionId, reviewerId, tenantId, comments);

        publishAuditEvent(questionId, reviewerId, tenantId, QuestionStateConstants.REVIEW, QuestionStateConstants.DRAFT);
        reviewWorkflowService.processTransition(saved, QuestionStateConstants.REVIEW, QuestionStateConstants.DRAFT, reviewerId, comments, tenantId);

        return toResponse(saved);
    }

    /**
     * Retrieve analytics and exposure metrics for a question.
     *
     * @param questionId the question UUID
     * @return question analytics data
     */
    public QuestionAnalytics getAnalytics(UUID questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException(QUESTION_NOT_FOUND + questionId));

        return QuestionAnalytics.builder()
                .usageCount(question.getUsageCount())
                .difficultyIndex(0.0)
                .discriminationIndex(0.0)
                .build();
    }

    private QuestionResponse toResponse(Question question) {
        return QuestionResponse.fromEntity(question);
    }
}
