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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        reviewWorkflowService = new ReviewWorkflowService(kafkaTemplate);

        questionId = UUID.randomUUID();
        authorId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        tenantId = "tenant-exam-board";

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
        Map<String, Object> event = (Map<String, Object>) valueCaptor.getValue();
        assertThat(event.get("eventType")).isEqualTo("SUBMITTED_FOR_REVIEW");
        assertThat(event.get("questionId")).isEqualTo(questionId);
        assertThat(event.get("subject")).isEqualTo("Mathematics");
        assertThat(event.get("authorId")).isEqualTo(authorId);
        assertThat(event.get("tenantId")).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("Approval (REVIEW→APPROVED) notifies author via notifications topic")
    void approvalNotifiesAuthor() {
        reviewWorkflowService.processTransition(testQuestion, "REVIEW", "APPROVED", actorId, null, tenantId);

        // Should publish to both lifecycle and notifications topics
        verify(kafkaTemplate, times(2)).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        var topics = topicCaptor.getAllValues();
        assertThat(topics).containsExactly("exam.question.lifecycle", "exam.notifications.outbound");

        // Check notification event
        @SuppressWarnings("unchecked")
        Map<String, Object> notification = (Map<String, Object>) valueCaptor.getAllValues().get(1);
        assertThat(notification.get("type")).isEqualTo("QUESTION_APPROVED_BY_REVIEWER");
        assertThat(notification.get("recipientId")).isEqualTo(authorId);
        assertThat(notification.get("questionId")).isEqualTo(questionId);
        assertThat(notification.get("reviewerId")).isEqualTo(actorId);
    }

    @Test
    @DisplayName("Return to DRAFT (REVIEW→DRAFT) notifies author with comments")
    void returnToDraftNotifiesAuthorWithComments() {
        String comments = "Please clarify option C";

        reviewWorkflowService.processTransition(testQuestion, "REVIEW", "DRAFT", actorId, comments, tenantId);

        verify(kafkaTemplate, times(2)).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        var topics = topicCaptor.getAllValues();
        assertThat(topics).containsExactly("exam.question.lifecycle", "exam.notifications.outbound");

        @SuppressWarnings("unchecked")
        Map<String, Object> notification = (Map<String, Object>) valueCaptor.getAllValues().get(1);
        assertThat(notification.get("type")).isEqualTo("QUESTION_RETURNED_BY_REVIEWER");
        assertThat(notification.get("recipientId")).isEqualTo(authorId);
        assertThat(notification.get("comments")).isEqualTo(comments);
    }

    @Test
    @DisplayName("Publishing (APPROVED→PUBLISHED) notifies author via notifications topic")
    void publishingNotifiesAuthor() {
        testQuestion.setState("PUBLISHED");

        reviewWorkflowService.processTransition(testQuestion, "APPROVED", "PUBLISHED", actorId, null, tenantId);

        verify(kafkaTemplate, times(2)).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        var topics = topicCaptor.getAllValues();
        assertThat(topics).containsExactly("exam.question.lifecycle", "exam.notifications.outbound");

        @SuppressWarnings("unchecked")
        Map<String, Object> notification = (Map<String, Object>) valueCaptor.getAllValues().get(1);
        assertThat(notification.get("type")).isEqualTo("QUESTION_PUBLISHED");
        assertThat(notification.get("recipientId")).isEqualTo(authorId);
        assertThat(notification.get("approverId")).isEqualTo(actorId);
    }
}
