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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates the review/approval workflow around question lifecycle transitions.
 * Publishes lifecycle events and notifications via Kafka topics.
 *
 * Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.6
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewWorkflowService {

    private static final String TOPIC_LIFECYCLE = "exam.question.lifecycle";
    private static final String TOPIC_NOTIFICATIONS = "exam.notifications.outbound";

    private final KafkaTemplate<String, Object> kafkaTemplate;

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
     * DRAFT → REVIEW: Assign to available reviewer by subject specialization,
     * publish lifecycle event.
     */
    private void handleSubmittedForReview(Question question, UUID actorId, String tenantId) {
        // Assign reviewer by subject specialization (simplified — production would query reviewer pool)
        UUID assignedReviewer = resolveReviewerBySubject(question.getSubject(), tenantId);

        Map<String, Object> lifecycleEvent = Map.of(
                "eventType", "SUBMITTED_FOR_REVIEW",
                "questionId", question.getId(),
                "subject", question.getSubject(),
                "authorId", question.getAuthorId(),
                "assignedReviewer", assignedReviewer != null ? assignedReviewer.toString() : "UNASSIGNED",
                "actorId", actorId,
                "tenantId", tenantId,
                "timestamp", Instant.now().toString()
        );

        kafkaTemplate.send(TOPIC_LIFECYCLE, question.getId().toString(), lifecycleEvent);

        log.info("Question submitted for review: questionId={}, subject={}, assignedReviewer={}, tenant={}",
                question.getId(), question.getSubject(), assignedReviewer, tenantId);
    }

    /**
     * REVIEW → APPROVED: Reviewer approved the question. Notify the author.
     */
    private void handleApproved(Question question, String fromState, UUID actorId, String tenantId) {
        if (!"REVIEW".equals(fromState)) {
            return;
        }

        Map<String, Object> lifecycleEvent = Map.of(
                "eventType", "REVIEWER_APPROVED",
                "questionId", question.getId(),
                "reviewerId", actorId,
                "authorId", question.getAuthorId(),
                "tenantId", tenantId,
                "timestamp", Instant.now().toString()
        );

        kafkaTemplate.send(TOPIC_LIFECYCLE, question.getId().toString(), lifecycleEvent);

        // Notify author that their question was approved by reviewer
        Map<String, Object> notification = Map.of(
                "type", "QUESTION_APPROVED_BY_REVIEWER",
                "recipientId", question.getAuthorId(),
                "questionId", question.getId(),
                "reviewerId", actorId,
                "subject", question.getSubject(),
                "tenantId", tenantId,
                "timestamp", Instant.now().toString()
        );

        kafkaTemplate.send(TOPIC_NOTIFICATIONS, question.getAuthorId().toString(), notification);

        log.info("Question approved by reviewer: questionId={}, reviewer={}, author notified={}",
                question.getId(), actorId, question.getAuthorId());
    }

    /**
     * REVIEW → DRAFT: Reviewer returned the question with comments. Notify the author.
     */
    private void handleReturnedToDraft(Question question, String fromState, UUID actorId,
                                       String comments, String tenantId) {
        if (!"REVIEW".equals(fromState)) {
            return;
        }

        Map<String, Object> lifecycleEvent = Map.of(
                "eventType", "RETURNED_TO_DRAFT",
                "questionId", question.getId(),
                "reviewerId", actorId,
                "authorId", question.getAuthorId(),
                "comments", comments != null ? comments : "",
                "tenantId", tenantId,
                "timestamp", Instant.now().toString()
        );

        kafkaTemplate.send(TOPIC_LIFECYCLE, question.getId().toString(), lifecycleEvent);

        // Notify author that their question was returned with comments
        Map<String, Object> notification = Map.of(
                "type", "QUESTION_RETURNED_BY_REVIEWER",
                "recipientId", question.getAuthorId(),
                "questionId", question.getId(),
                "reviewerId", actorId,
                "comments", comments != null ? comments : "",
                "subject", question.getSubject(),
                "tenantId", tenantId,
                "timestamp", Instant.now().toString()
        );

        kafkaTemplate.send(TOPIC_NOTIFICATIONS, question.getAuthorId().toString(), notification);

        log.info("Question returned to draft: questionId={}, reviewer={}, author notified={}, hasComments={}",
                question.getId(), actorId, question.getAuthorId(), comments != null);
    }

    /**
     * APPROVED → PUBLISHED: Approver published the question. Notify via notifications topic.
     */
    private void handlePublished(Question question, UUID actorId, String tenantId) {
        Map<String, Object> lifecycleEvent = Map.of(
                "eventType", "QUESTION_PUBLISHED",
                "questionId", question.getId(),
                "approverId", actorId,
                "authorId", question.getAuthorId(),
                "tenantId", tenantId,
                "timestamp", Instant.now().toString()
        );

        kafkaTemplate.send(TOPIC_LIFECYCLE, question.getId().toString(), lifecycleEvent);

        // Notify author that their question was published
        Map<String, Object> notification = Map.of(
                "type", "QUESTION_PUBLISHED",
                "recipientId", question.getAuthorId(),
                "questionId", question.getId(),
                "approverId", actorId,
                "subject", question.getSubject(),
                "tenantId", tenantId,
                "timestamp", Instant.now().toString()
        );

        kafkaTemplate.send(TOPIC_NOTIFICATIONS, question.getAuthorId().toString(), notification);

        log.info("Question published: questionId={}, approver={}, author notified={}",
                question.getId(), actorId, question.getAuthorId());
    }

    /**
     * Resolves a reviewer by subject specialization.
     * In production, this would query a reviewer pool/assignment service.
     * Returns null if no reviewer is available (will be marked as UNASSIGNED).
     */
    private UUID resolveReviewerBySubject(String subject, String tenantId) {
        // Placeholder: production implementation would query a reviewer registry
        // filtered by subject specialization and tenant
        log.debug("Resolving reviewer for subject={}, tenant={}", subject, tenantId);
        return null;
    }
}
