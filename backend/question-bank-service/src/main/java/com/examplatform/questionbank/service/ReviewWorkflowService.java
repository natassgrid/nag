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

import com.examplatform.questionbank.constants.QuestionBankConstants;
import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.dto.ReviewerAssignment;
import com.examplatform.shared.config.DynamicConfigService;
import com.examplatform.shared.constants.MessagingConstants;
import com.examplatform.shared.constants.QuestionStateConstants;
import com.examplatform.shared.constants.SharedConstants;
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

    private static final String TOPIC_LIFECYCLE = QuestionBankConstants.TOPIC_QUESTION_LIFECYCLE;
    private static final String TOPIC_NOTIFICATIONS = MessagingConstants.NOTIFICATIONS_OUTBOUND_TOPIC;

    private final EventPublisher eventPublisher;
    private final DynamicConfigService dynamicConfigService;
    private final ReviewerAssignmentService reviewerAssignmentService;
    private final com.examplatform.questionbank.repository.SubjectRepository subjectRepository;

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
            case QuestionStateConstants.REVIEW -> handleSubmittedForReview(question, actorId, tenantId);
            case QuestionStateConstants.APPROVED -> handleApproved(question, fromState, actorId, tenantId);
            case QuestionStateConstants.DRAFT -> handleReturnedToDraft(question, fromState, actorId, comments, tenantId);
            case QuestionStateConstants.PUBLISHED -> handlePublished(question, actorId, tenantId);
            default -> log.debug("No workflow action for transition to state: {}", toState);
        }
    }

    private String resolveSubjectName(Question question) {
        if (question.getSubjectId() == null) {
            return "General";
        }
        return subjectRepository.findById(question.getSubjectId())
                .map(com.examplatform.questionbank.domain.Subject::getName)
                .orElse("Subject #" + question.getSubjectId());
    }

    /**
     * DRAFT -> REVIEW: Assign to available reviewer(s) by subject specialization,
     * load-balance, and publish lifecycle event and reviewer notifications.
     */
    private void handleSubmittedForReview(Question question, UUID actorId, String tenantId) {
        boolean dualReviewRequired = dynamicConfigService.getBoolean(
                QuestionBankConstants.CONFIG_DUAL_REVIEW_REQUIRED, tenantId, true);

        String subjectName = resolveSubjectName(question);
        ReviewerAssignment assignment = reviewerAssignmentService.assignReviewers(
                subjectName, question.getAuthorId(), tenantId, dualReviewRequired);

        UUID primaryReviewer = assignment.getPrimaryReviewerId();
        UUID secondaryReviewer = assignment.getSecondaryReviewerId();

        Map<String, Object> lifecycleEvent = new HashMap<>();
        lifecycleEvent.put(SharedConstants.EVENT_TYPE, QuestionBankConstants.EVENT_SUBMITTED_FOR_REVIEW);
        lifecycleEvent.put(SharedConstants.QUESTION_ID, question.getId());
        lifecycleEvent.put(SharedConstants.SUBJECT, subjectName);
        lifecycleEvent.put(SharedConstants.SUBJECT_ID, question.getSubjectId());
        lifecycleEvent.put(SharedConstants.AUTHOR_ID, question.getAuthorId());
        lifecycleEvent.put(QuestionBankConstants.KEY_ASSIGNED_REVIEWER,
                primaryReviewer != null ? primaryReviewer.toString() : QuestionBankConstants.VALUE_UNASSIGNED);
        if (secondaryReviewer != null) {
            lifecycleEvent.put(QuestionBankConstants.KEY_ASSIGNED_REVIEWER_2, secondaryReviewer.toString());
        }
        lifecycleEvent.put(QuestionBankConstants.KEY_ASSIGNED_REVIEWERS,
                assignment.getAssignedReviewerIds().stream().map(UUID::toString).toList());
        lifecycleEvent.put(QuestionBankConstants.KEY_ESCALATED_TO_CONTROLLER, assignment.isEscalatedToController());
        lifecycleEvent.put(QuestionBankConstants.KEY_DUAL_REVIEW_REQUIRED, dualReviewRequired);
        lifecycleEvent.put(SharedConstants.ACTOR_ID, actorId);
        lifecycleEvent.put(SharedConstants.TENANT_ID, tenantId);
        lifecycleEvent.put(QuestionBankConstants.KEY_TIMESTAMP, Instant.now().toString());

        publishLifecycleEvent(question.getId(), lifecycleEvent);

        // Notify assigned reviewer(s)
        for (UUID assignedReviewerId : assignment.getAssignedReviewerIds()) {
            Map<String, Object> notification = Map.of(
                    QuestionBankConstants.KEY_TYPE, QuestionBankConstants.NOTIF_QUESTION_ASSIGNED_FOR_REVIEW,
                    SharedConstants.RECIPIENT_ID, assignedReviewerId,
                    SharedConstants.QUESTION_ID, question.getId(),
                    SharedConstants.SUBJECT, subjectName,
                    SharedConstants.TENANT_ID, tenantId,
                    SharedConstants.MESSAGE, "A new question for subject '" + subjectName + "' has been assigned to you for review."
            );
            publishNotification(assignedReviewerId, notification);
        }

        log.info("Question submitted for review: questionId={}, subject={}, primaryReviewer={}, secondaryReviewer={}, dualReviewRequired={}, tenant={}",
                question.getId(), subjectName, primaryReviewer, secondaryReviewer, dualReviewRequired, tenantId);
    }

    /**
     * REVIEW -> APPROVED: Reviewer approved the question. Decrement load and notify the author.
     */
    private void handleApproved(Question question, String fromState, UUID actorId, String tenantId) {
        if (!QuestionStateConstants.REVIEW.equals(fromState)) {
            return;
        }

        reviewerAssignmentService.releaseReviewerLoad(actorId, tenantId);

        String subjectName = resolveSubjectName(question);
        Map<String, Object> lifecycleEvent = createLifecyclePayload(QuestionBankConstants.EVENT_REVIEWER_APPROVED, question.getId(), tenantId);
        lifecycleEvent.put(SharedConstants.REVIEWER_ID, actorId);
        lifecycleEvent.put(SharedConstants.AUTHOR_ID, question.getAuthorId());
        lifecycleEvent.put(SharedConstants.SUBJECT, subjectName);
        lifecycleEvent.put(SharedConstants.SUBJECT_ID, question.getSubjectId());
        publishLifecycleEvent(question.getId(), lifecycleEvent);

        // Notify author that their question was approved by reviewer
        Map<String, Object> notification = Map.of(
                QuestionBankConstants.KEY_TYPE, QuestionBankConstants.NOTIF_QUESTION_APPROVED_BY_REVIEWER,
                SharedConstants.RECIPIENT_ID, question.getAuthorId(),
                SharedConstants.QUESTION_ID, question.getId(),
                SharedConstants.REVIEWER_ID, actorId,
                SharedConstants.SUBJECT, subjectName,
                SharedConstants.TENANT_ID, tenantId,
                SharedConstants.MESSAGE, SharedConstants.YOUR_QUESTION_FOR_SUBJECT_PREFIX + subjectName + "' has been approved by the reviewer."
        );
        publishNotification(question.getAuthorId(), notification);

        log.info("Question approved by reviewer: questionId={}, reviewer={}, author={}, tenant={}",
                question.getId(), actorId, question.getAuthorId(), tenantId);
    }

    /**
     * REVIEW -> DRAFT: Reviewer returned question with comments. Decrement load and notify the author.
     */
    private void handleReturnedToDraft(Question question, String fromState, UUID actorId,
                                       String comments, String tenantId) {
        if (!QuestionStateConstants.REVIEW.equals(fromState)) {
            return;
        }

        reviewerAssignmentService.releaseReviewerLoad(actorId, tenantId);

        String subjectName = resolveSubjectName(question);
        Map<String, Object> lifecycleEvent = createLifecyclePayload(QuestionBankConstants.EVENT_RETURNED_TO_DRAFT, question.getId(), tenantId);
        lifecycleEvent.put(SharedConstants.REVIEWER_ID, actorId);
        lifecycleEvent.put(SharedConstants.AUTHOR_ID, question.getAuthorId());
        lifecycleEvent.put(QuestionBankConstants.KEY_COMMENTS, comments != null ? comments : "");
        lifecycleEvent.put(SharedConstants.SUBJECT, subjectName);
        lifecycleEvent.put(SharedConstants.SUBJECT_ID, question.getSubjectId());
        publishLifecycleEvent(question.getId(), lifecycleEvent);

        // Notify author that their question was returned for revision
        Map<String, Object> notification = Map.of(
                QuestionBankConstants.KEY_TYPE, QuestionBankConstants.NOTIF_QUESTION_RETURNED_FOR_REVISION,
                SharedConstants.RECIPIENT_ID, question.getAuthorId(),
                SharedConstants.QUESTION_ID, question.getId(),
                SharedConstants.REVIEWER_ID, actorId,
                QuestionBankConstants.KEY_COMMENTS, comments != null ? comments : "",
                SharedConstants.TENANT_ID, tenantId,
                SharedConstants.MESSAGE, SharedConstants.YOUR_QUESTION_FOR_SUBJECT_PREFIX + subjectName +
                        "' was returned with comments: " + (comments != null ? comments : QuestionBankConstants.DEFAULT_NO_COMMENTS)
        );
        publishNotification(question.getAuthorId(), notification);

        log.info("Question returned to draft: questionId={}, reviewer={}, author={}, tenant={}",
                question.getId(), actorId, question.getAuthorId(), tenantId);
    }

    /**
     * APPROVED -> PUBLISHED: Final approval given. Question available in bank.
     */
    private void handlePublished(Question question, UUID actorId, String tenantId) {
        String subjectName = resolveSubjectName(question);
        Map<String, Object> lifecycleEvent = createLifecyclePayload(QuestionBankConstants.EVENT_QUESTION_PUBLISHED, question.getId(), tenantId);
        lifecycleEvent.put(QuestionBankConstants.KEY_PUBLISHER_ID, actorId);
        lifecycleEvent.put(SharedConstants.AUTHOR_ID, question.getAuthorId());
        lifecycleEvent.put(SharedConstants.SUBJECT, subjectName);
        lifecycleEvent.put(SharedConstants.SUBJECT_ID, question.getSubjectId());
        publishLifecycleEvent(question.getId(), lifecycleEvent);

        // Notify author that their question has been published
        Map<String, Object> notification = Map.of(
                QuestionBankConstants.KEY_TYPE, QuestionBankConstants.EVENT_QUESTION_PUBLISHED,
                SharedConstants.RECIPIENT_ID, question.getAuthorId(),
                SharedConstants.QUESTION_ID, question.getId(),
                SharedConstants.SUBJECT, subjectName,
                SharedConstants.TENANT_ID, tenantId,
                SharedConstants.MESSAGE, SharedConstants.YOUR_QUESTION_FOR_SUBJECT_PREFIX + subjectName + "' has been published to the question bank."
        );
        publishNotification(question.getAuthorId(), notification);

        log.info("Question published to bank: questionId={}, publisher={}, tenant={}",
                question.getId(), actorId, tenantId);
    }

    private void publishLifecycleEvent(UUID questionId, Map<String, Object> event) {
        eventPublisher.publish(TOPIC_LIFECYCLE, questionId.toString(), event);
    }

    private void publishNotification(UUID recipientId, Map<String, Object> notification) {
        eventPublisher.publish(TOPIC_NOTIFICATIONS, recipientId.toString(), notification);
    }

    private Map<String, Object> createLifecyclePayload(String eventType, UUID questionId, String tenantId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(SharedConstants.EVENT_TYPE, eventType);
        payload.put(SharedConstants.QUESTION_ID, questionId);
        payload.put(SharedConstants.TENANT_ID, tenantId);
        payload.put(QuestionBankConstants.KEY_TIMESTAMP, Instant.now().toString());
        return payload;
    }
}
