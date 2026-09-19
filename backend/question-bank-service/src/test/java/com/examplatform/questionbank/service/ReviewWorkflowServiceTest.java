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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ReviewWorkflowService.
 *
 * Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.6
 */
@ExtendWith(MockitoExtension.class)
class ReviewWorkflowServiceTest {

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private DynamicConfigService dynamicConfigService;

    @Mock
    private ReviewerAssignmentService reviewerAssignmentService;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<Object> valueCaptor;

    private ReviewWorkflowService reviewWorkflowService;

    private Question testQuestion;
    private UUID questionId;
    private UUID authorId;
    private UUID actorId;
    private UUID reviewerId;
    private UUID secondaryReviewerId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        reviewWorkflowService = new ReviewWorkflowService(
                eventPublisher, dynamicConfigService, reviewerAssignmentService);

        questionId = UUID.randomUUID();
        authorId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        reviewerId = UUID.randomUUID();
        secondaryReviewerId = UUID.randomUUID();
        tenantId = "tenant-exam-board";

        Mockito.lenient().when(dynamicConfigService.getBoolean(anyString(), anyString(), anyBoolean()))
                .thenAnswer(inv -> inv.getArgument(2));

        testQuestion = Question.builder()
                .subject("Mathematics")
                .topic("Algebra")
                .authorId(authorId)
                .state("REVIEW")
                .build();
        testQuestion.setTenantId(tenantId);
        try {
            var idField = testQuestion.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testQuestion, questionId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Transition to REVIEW assigns reviewer(s), publishes lifecycle event and reviewer notifications")
    void transitionToReview_publishesLifecycleEventAndNotifications() {
        ReviewerAssignment assignment = ReviewerAssignment.builder()
                .primaryReviewerId(reviewerId)
                .secondaryReviewerId(secondaryReviewerId)
                .assignedReviewerIds(List.of(reviewerId, secondaryReviewerId))
                .escalatedToController(false)
                .build();

        when(reviewerAssignmentService.assignReviewers("Mathematics", authorId, tenantId, true))
                .thenReturn(assignment);

        reviewWorkflowService.processTransition(testQuestion, "DRAFT", "REVIEW", actorId, null, tenantId);

        // 1 lifecycle event + 2 reviewer notifications = 3 eventPublisher.publish calls
        verify(eventPublisher, times(3)).publish(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        var topics = topicCaptor.getAllValues();
        var keys = keyCaptor.getAllValues();
        var values = valueCaptor.getAllValues();

        assertThat(topics.get(0)).isEqualTo("exam.question.lifecycle");
        assertThat(keys.get(0)).isEqualTo(questionId.toString());

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) values.get(0);
        assertThat(payload)
                .containsEntry("eventType", "SUBMITTED_FOR_REVIEW")
                .containsEntry("questionId", questionId)
                .containsEntry("subject", "Mathematics")
                .containsEntry("authorId", authorId)
                .containsEntry("actorId", actorId)
                .containsEntry("tenantId", tenantId)
                .containsEntry("dualReviewRequired", true)
                .containsEntry("assignedReviewer", reviewerId.toString())
                .containsEntry("assignedReviewer2", secondaryReviewerId.toString())
                .containsKey("timestamp");

        // Reviewer notifications
        assertThat(topics.get(1)).isEqualTo("exam.notifications.outbound");
        assertThat(keys.get(1)).isEqualTo(reviewerId.toString());

        assertThat(topics.get(2)).isEqualTo("exam.notifications.outbound");
        assertThat(keys.get(2)).isEqualTo(secondaryReviewerId.toString());
    }

    @Test
    @DisplayName("Transition to APPROVED releases reviewer load, publishes REVIEWER_APPROVED event and sends notification")
    void transitionToApproved_publishesEventAndNotification() {
        reviewWorkflowService.processTransition(testQuestion, "REVIEW", "APPROVED", actorId, null, tenantId);

        verify(reviewerAssignmentService).releaseReviewerLoad(actorId, tenantId);
        verify(eventPublisher, times(2)).publish(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        var topics = topicCaptor.getAllValues();
        var keys = keyCaptor.getAllValues();
        var values = valueCaptor.getAllValues();

        // First call: lifecycle topic
        assertThat(topics.get(0)).isEqualTo("exam.question.lifecycle");
        assertThat(keys.get(0)).isEqualTo(questionId.toString());

        @SuppressWarnings("unchecked")
        Map<String, Object> lifecyclePayload = (Map<String, Object>) values.get(0);
        assertThat(lifecyclePayload)
                .containsEntry("eventType", "REVIEWER_APPROVED")
                .containsEntry("questionId", questionId)
                .containsEntry("reviewerId", actorId)
                .containsEntry("authorId", authorId);

        // Second call: notifications topic
        assertThat(topics.get(1)).isEqualTo("exam.notifications.outbound");
        assertThat(keys.get(1)).isEqualTo(authorId.toString());

        @SuppressWarnings("unchecked")
        Map<String, Object> notifPayload = (Map<String, Object>) values.get(1);
        assertThat(notifPayload)
                .containsEntry("type", "QUESTION_APPROVED_BY_REVIEWER")
                .containsEntry("recipientId", authorId)
                .containsEntry("questionId", questionId)
                .containsEntry("reviewerId", actorId)
                .containsEntry("subject", "Mathematics");
    }

    @Test
    @DisplayName("Transition to DRAFT releases reviewer load, publishes RETURNED_TO_DRAFT event with comments and notifies author")
    void transitionToDraft_publishesEventAndNotificationWithComments() {
        String comments = "Please provide more detailed explanation in the answer key.";
        reviewWorkflowService.processTransition(testQuestion, "REVIEW", "DRAFT", actorId, comments, tenantId);

        verify(reviewerAssignmentService).releaseReviewerLoad(actorId, tenantId);
        verify(eventPublisher, times(2)).publish(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        var topics = topicCaptor.getAllValues();
        var values = valueCaptor.getAllValues();

        assertThat(topics.get(0)).isEqualTo("exam.question.lifecycle");
        @SuppressWarnings("unchecked")
        Map<String, Object> lifecyclePayload = (Map<String, Object>) values.get(0);
        assertThat(lifecyclePayload)
                .containsEntry("eventType", "RETURNED_TO_DRAFT")
                .containsEntry("comments", comments);

        assertThat(topics.get(1)).isEqualTo("exam.notifications.outbound");
        @SuppressWarnings("unchecked")
        Map<String, Object> notifPayload = (Map<String, Object>) values.get(1);
        assertThat(notifPayload)
                .containsEntry("type", "QUESTION_RETURNED_FOR_REVISION")
                .containsEntry("comments", comments);
    }

    @Test
    @DisplayName("Transition to PUBLISHED publishes QUESTION_PUBLISHED event and notifies author")
    void transitionToPublished_publishesEventAndNotification() {
        reviewWorkflowService.processTransition(testQuestion, "APPROVED", "PUBLISHED", actorId, null, tenantId);

        verify(eventPublisher, times(2)).publish(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        var topics = topicCaptor.getAllValues();
        var values = valueCaptor.getAllValues();

        assertThat(topics.get(0)).isEqualTo("exam.question.lifecycle");
        @SuppressWarnings("unchecked")
        Map<String, Object> lifecyclePayload = (Map<String, Object>) values.get(0);
        assertThat(lifecyclePayload)
                .containsEntry("eventType", "QUESTION_PUBLISHED")
                .containsEntry("publisherId", actorId)
                .containsEntry("authorId", authorId);

        assertThat(topics.get(1)).isEqualTo("exam.notifications.outbound");
        @SuppressWarnings("unchecked")
        Map<String, Object> notifPayload = (Map<String, Object>) values.get(1);
        assertThat(notifPayload)
                .containsEntry("type", "QUESTION_PUBLISHED")
                .containsEntry("recipientId", authorId);
    }
}
