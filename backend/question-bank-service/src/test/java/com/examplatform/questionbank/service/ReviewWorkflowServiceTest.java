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
import com.examplatform.shared.config.DynamicConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private DynamicConfigService dynamicConfigService;

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
    private String tenantId;

    @BeforeEach
    void setUp() {
        reviewWorkflowService = new ReviewWorkflowService(kafkaTemplate, dynamicConfigService);

        questionId = UUID.randomUUID();
        authorId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        tenantId = "tenant-exam-board";

        Mockito.lenient().when(dynamicConfigService.getBoolean(anyString(), anyString(), anyBoolean()))
                .thenAnswer(inv -> inv.getArgument(2));

        testQuestion = Question.builder()
                .subject("Mathematics")
                .topic("Algebra")
                .authorId(authorId)
                .state("REVIEW")
                .build();
        // Use reflection-like setter for BaseEntity id
        testQuestion.setTenantId(tenantId);
        // Set id via the protected method — use builder workaround
        try {
            var idField = testQuestion.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testQuestion, questionId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    @DisplayName("Transition to REVIEW publishes lifecycle event to exam.question.lifecycle")
    void transitionToReview_publishesLifecycleEvent() {
        reviewWorkflowService.processTransition(testQuestion, "DRAFT", "REVIEW", actorId, null, tenantId);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("exam.question.lifecycle");
        assertThat(keyCaptor.getValue()).isEqualTo(questionId.toString());

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) valueCaptor.getValue();
        assertThat(payload)
                .containsEntry("eventType", "SUBMITTED_FOR_REVIEW")
                .containsEntry("questionId", questionId)
                .containsEntry("subject", "Mathematics")
                .containsEntry("authorId", authorId)
                .containsEntry("actorId", actorId)
                .containsEntry("tenantId", tenantId)
                .containsEntry("dualReviewRequired", true)
                .containsKey("assignedReviewer")
                .containsKey("timestamp");
    }

    @Test
    @DisplayName("Transition to APPROVED publishes REVIEWER_APPROVED event and sends notification")
    void transitionToApproved_publishesEventAndNotification() {
        reviewWorkflowService.processTransition(testQuestion, "REVIEW", "APPROVED", actorId, null, tenantId);

        verify(kafkaTemplate, times(2)).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

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
    @DisplayName("Transition to DRAFT publishes RETURNED_TO_DRAFT event with comments and notifies author")
    void transitionToDraft_publishesEventAndNotificationWithComments() {
        String comments = "Please provide more detailed explanation in the answer key.";
        reviewWorkflowService.processTransition(testQuestion, "REVIEW", "DRAFT", actorId, comments, tenantId);

        verify(kafkaTemplate, times(2)).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

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

        verify(kafkaTemplate, times(2)).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

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
