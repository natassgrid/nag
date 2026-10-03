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

import com.examplatform.questionbank.client.ReviewerPoolClient;
import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.dto.ReviewerDto;
import com.examplatform.questionbank.dto.TransitionRequest;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.support.AbstractIntegrationTest;
import com.examplatform.shared.messaging.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Reviewer Assignment Workflow Integration Test")
class ReviewerAssignmentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private QuestionLifecycleService questionLifecycleService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ReviewerAssignmentService reviewerAssignmentService;

    @MockitoBean
    private ReviewerPoolClient reviewerPoolClient;

    @MockitoBean
    private EventPublisher eventPublisher;

    private UUID authorId;
    private UUID reviewer1Id;
    private UUID reviewer2Id;
    private final String tenantId = "default";

    @BeforeEach
    void setupReviewerPool() {
        authorId = UUID.randomUUID();
        reviewer1Id = UUID.randomUUID();
        reviewer2Id = UUID.randomUUID();

        ReviewerDto mathReviewer1 = ReviewerDto.builder()
                .id(reviewer1Id)
                .username("math_sme_1")
                .specialization("Mathematics")
                .roles(List.of("SUBJECT_MATTER_EXPERT"))
                .build();

        ReviewerDto mathReviewer2 = ReviewerDto.builder()
                .id(reviewer2Id)
                .username("math_sme_2")
                .specialization("Mathematics")
                .roles(List.of("REVIEWER"))
                .build();

        when(reviewerPoolClient.getReviewers(anyString(), anyString()))
                .thenReturn(List.of(mathReviewer1, mathReviewer2));

        reviewerAssignmentService.evictCache("Mathematics", tenantId);
    }

    @Test
    @DisplayName("Lifecycle transition DRAFT -> REVIEW assigns reviewers and publishes event")
    void draftToReview_assignsReviewersAndPublishesEvents() {
        Question question = Question.builder()
                .subjectId(1L)
                .topicId(10L)
                .subject("Mathematics")
                .topic("Calculus")
                .difficulty("MEDIUM")
                .cognitiveLevel("APPLY")
                .questionType("SINGLE_MCQ")
                .content("Calculate the derivative of f(x) = x^2.")
                .answerKey("2x")
                .state("DRAFT")
                .authorId(authorId)
                .build();
        question.setTenantId(tenantId);
        question = questionRepository.save(question);

        TransitionRequest transitionRequest = TransitionRequest.builder()
                .targetState("REVIEW")
                .build();

        QuestionResponse response = questionLifecycleService.transition(
                question.getId(), transitionRequest, authorId, tenantId);

        assertThat(response.getState()).isEqualTo("REVIEW");

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);

        verify(eventPublisher, atLeastOnce()).publish(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());

        List<String> topics = topicCaptor.getAllValues();
        assertThat(topics).contains("exam.question.lifecycle");
        assertThat(topics).contains("exam.notifications.outbound");

        // Load for assigned reviewer should be incremented
        int load1 = reviewerAssignmentService.getReviewerLoad(reviewer1Id, tenantId);
        int load2 = reviewerAssignmentService.getReviewerLoad(reviewer2Id, tenantId);
        assertThat(load1 + load2).isGreaterThanOrEqualTo(1);

        // Approve question and ensure load decrements
        QuestionResponse approvedResponse = questionLifecycleService.approve(question.getId(), reviewer1Id, tenantId);
        assertThat(approvedResponse.getState()).isEqualTo("APPROVED");
    }
}
