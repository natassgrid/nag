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

package com.examplatform.questionbank.service;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.dto.ReviewerAssignment;
import com.examplatform.shared.config.DynamicConfigService;
import com.examplatform.shared.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates the review/approval workflow around question lifecycle transitions.
 * Publishes lifecycle events and notifications via messaging broker.
 *
 * Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.6
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewWorkflowService {

    private static final String TOPIC_LIFECYCLE = "exam.question.lifecycle";
    private static final String TOPIC_NOTIFICATIONS = "exam.notifications.outbound";

    private final EventPublisher eventPublisher;
    private final DynamicConfigService dynamicConfigService;
    private final ReviewerAssignmentService reviewerAssignmentService;

    /**
     * Processes a lifecycle transition event and triggers appropriate workflow actions.
     *
     * @param question  the question that was transitioned
     * @param fromState the previous state
     * @param toState   the new state
     * @param actorId   the user who performed the transition
     * @param comments  optional reviewer comments
     * @param tenantId  tenant identifier
     */
    public void processTransition(Question question, String fromState, String toState,
                                  UUID actorId, String comments, String tenantId) {
        switch (toState) {
            case "REVIEW" -> handleSubmittedForReview(question, actorId, tenantId);
            case "APPROVED" -> handleApproved(question, fromState, actorId, tenantId);
            case "DRAFT" -> handleReturnedToDraft(question, fromState, actorId, comments, tenantId);
            case "PUBLISHED" -> handlePublished(question, actorId, tenantId);
            default -> log.debug("No workflow action for transition to state: {}", toState);
        }
    }

    /**
     * DRAFT -> REVIEW: Assign to available reviewer(s) by subject specialization,
     * load-balance, and publish lifecycle event and reviewer notifications.
     */
    private void handleSubmittedForReview(Question question, UUID actorId, String tenantId) {
        boolean dualReviewRequired = dynamicConfigService.getBoolean(
                "question.dual.review.required", tenantId, true);

        ReviewerAssignment assignment = reviewerAssignmentService.assignReviewers(
                question.getSubject(), question.getAuthorId(), tenantId, dualReviewRequired);

        UUID primaryReviewer = assignment.getPrimaryReviewerId();
        UUID secondaryReviewer = assignment.getSecondaryReviewerId();

        Map<String, Object> lifecycleEvent = new HashMap<>();
        lifecycleEvent.put("eventType", "SUBMITTED_FOR_REVIEW");
        lifecycleEvent.put("questionId", question.getId());
        lifecycleEvent.put("subject", question.getSubject());
        lifecycleEvent.put("authorId", question.getAuthorId());
        lifecycleEvent.put("assignedReviewer", primaryReviewer != null ? primaryReviewer.toString() : "UNASSIGNED");
        if (secondaryReviewer != null) {
            lifecycleEvent.put("assignedReviewer2", secondaryReviewer.toString());
        }
        lifecycleEvent.put("assignedReviewers", assignment.getAssignedReviewerIds().stream().map(UUID::toString).toList());
        lifecycleEvent.put("escalatedToController", assignment.isEscalatedToController());
        lifecycleEvent.put("dualReviewRequired", dualReviewRequired);
        lifecycleEvent.put("actorId", actorId);
        lifecycleEvent.put("tenantId", tenantId);
        lifecycleEvent.put("timestamp", Instant.now().toString());

        eventPublisher.publish(TOPIC_LIFECYCLE, question.getId().toString(), lifecycleEvent);

        // Notify assigned reviewer(s)
        for (UUID assignedReviewerId : assignment.getAssignedReviewerIds()) {
            Map<String, Object> notification = Map.of(
                    "type", "QUESTION_ASSIGNED_FOR_REVIEW",
                    "recipientId", assignedReviewerId,
                    "questionId", question.getId(),
                    "subject", question.getSubject(),
                    "tenantId", tenantId,
                    "message", "A new question for subject '" + question.getSubject() + "' has been assigned to you for review."
            );
            eventPublisher.publish(TOPIC_NOTIFICATIONS, assignedReviewerId.toString(), notification);
        }

        log.info("Question submitted for review: questionId={}, subject={}, primaryReviewer={}, secondaryReviewer={}, dualReviewRequired={}, tenant={}",
                question.getId(), question.getSubject(), primaryReviewer, secondaryReviewer, dualReviewRequired, tenantId);
    }

    /**
     * REVIEW -> APPROVED: Reviewer approved the question. Decrement load and notify the author.
     */
    private void handleApproved(Question question, String fromState, UUID actorId, String tenantId) {
        if (!"REVIEW".equals(fromState)) {
            return;
        }

        reviewerAssignmentService.releaseReviewerLoad(actorId, tenantId);

        Map<String, Object> lifecycleEvent = Map.of(
                "eventType", "REVIEWER_APPROVED",
                "questionId", question.getId(),
                "reviewerId", actorId,
                "authorId", question.getAuthorId(),
                "tenantId", tenantId,
                "timestamp", Instant.now().toString()
        );

        eventPublisher.publish(TOPIC_LIFECYCLE, question.getId().toString(), lifecycleEvent);

        // Notify author that their question was approved by reviewer
        Map<String, Object> notification = Map.of(
                "type", "QUESTION_APPROVED_BY_REVIEWER",
                "recipientId", question.getAuthorId(),
                "questionId", question.getId(),
                "reviewerId", actorId,
                "subject", question.getSubject(),
                "tenantId", tenantId,
                "message", "Your question for subject '" + question.getSubject() + "' has been approved by the reviewer."
        );

        eventPublisher.publish(TOPIC_NOTIFICATIONS, question.getAuthorId().toString(), notification);

        log.info("Question approved by reviewer: questionId={}, reviewer={}, author={}, tenant={}",
                question.getId(), actorId, question.getAuthorId(), tenantId);
    }

    /**
     * REVIEW -> DRAFT: Reviewer returned question with comments. Decrement load and notify the author.
     */
    private void handleReturnedToDraft(Question question, String fromState, UUID actorId,
                                       String comments, String tenantId) {
        if (!"REVIEW".equals(fromState)) {
            return;
        }

        reviewerAssignmentService.releaseReviewerLoad(actorId, tenantId);

        Map<String, Object> lifecycleEvent = Map.of(
                "eventType", "RETURNED_TO_DRAFT",
                "questionId", question.getId(),
                "reviewerId", actorId,
                "authorId", question.getAuthorId(),
                "comments", comments != null ? comments : "",
                "tenantId", tenantId,
                "timestamp", Instant.now().toString()
        );

        eventPublisher.publish(TOPIC_LIFECYCLE, question.getId().toString(), lifecycleEvent);

        // Notify author that their question was returned for revision
        Map<String, Object> notification = Map.of(
                "type", "QUESTION_RETURNED_FOR_REVISION",
                "recipientId", question.getAuthorId(),
                "questionId", question.getId(),
                "reviewerId", actorId,
                "comments", comments != null ? comments : "",
                "tenantId", tenantId,
                "message", "Your question for subject '" + question.getSubject() +
                           "' was returned with comments: " + (comments != null ? comments : "No comments provided")
        );

        eventPublisher.publish(TOPIC_NOTIFICATIONS, question.getAuthorId().toString(), notification);

        log.info("Question returned to draft: questionId={}, reviewer={}, author={}, tenant={}",
                question.getId(), actorId, question.getAuthorId(), tenantId);
    }

    /**
     * APPROVED -> PUBLISHED: Final approval given. Question available in bank.
     */
    private void handlePublished(Question question, UUID actorId, String tenantId) {
        Map<String, Object> lifecycleEvent = Map.of(
                "eventType", "QUESTION_PUBLISHED",
                "questionId", question.getId(),
                "publisherId", actorId,
                "authorId", question.getAuthorId(),
                "subject", question.getSubject(),
                "tenantId", tenantId,
                "timestamp", Instant.now().toString()
        );

        eventPublisher.publish(TOPIC_LIFECYCLE, question.getId().toString(), lifecycleEvent);

        // Notify author that their question has been published
        Map<String, Object> notification = Map.of(
                "type", "QUESTION_PUBLISHED",
                "recipientId", question.getAuthorId(),
                "questionId", question.getId(),
                "subject", question.getSubject(),
                "tenantId", tenantId,
                "message", "Your question for subject '" + question.getSubject() + "' has been published to the question bank."
        );

        eventPublisher.publish(TOPIC_NOTIFICATIONS, question.getAuthorId().toString(), notification);

        log.info("Question published to bank: questionId={}, publisher={}, tenant={}",
                question.getId(), actorId, tenantId);
    }
}
