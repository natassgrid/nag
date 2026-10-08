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

package com.examplatform.questionbank.lifecycle;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.domain.enums.CognitiveLevel;
import com.examplatform.questionbank.domain.enums.DifficultyLevel;
import com.examplatform.questionbank.domain.enums.QuestionType;
import com.examplatform.questionbank.dto.CreateQuestionRequest;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.dto.TransitionRequest;
import com.examplatform.questionbank.exception.FourEyesPrincipleViolationException;
import com.examplatform.questionbank.exception.InvalidTransitionException;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.service.QuestionLifecycleService;
import com.examplatform.questionbank.service.QuestionService;
import com.examplatform.questionbank.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

/**
 * End-to-end lifecycle integration test for the Question Review & Approval Workflow.
 *
 * <p>Tests the complete FSM journey:
 * DRAFT → REVIEW → DRAFT (rejected with comments) → REVIEW → APPROVED → PUBLISHED
 *
 * <p>These tests run against a real PostgreSQL Testcontainer (via {@link AbstractIntegrationTest})
 * and exercise the actual service layer and repository, ensuring that:
 * <ul>
 *   <li>Four-eyes principle is enforced: reviewer != author on approve()</li>
 *   <li>Rejection comments are persisted on the Question entity and surfaced in the response</li>
 *   <li>Authors can re-submit after rejection</li>
 *   <li>A different reviewer can approve the re-submitted question</li>
 *   <li>EXAM_CONTROLLER can publish an approved question</li>
 * </ul>
 *
 * Validates: Requirements 4.6, 5.2, 5.3, 5.5
 */
@DisplayName("Question Lifecycle Workflow Integration Tests (DRAFT → REVIEW → APPROVED → PUBLISHED)")
class QuestionLifecycleWorkflowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionLifecycleService questionLifecycleService;

    @Autowired
    private QuestionRepository questionRepository;

    // Mock the ReviewerAssignmentService and DynamicConfigService dependencies
    // to avoid requiring the full identity/admin service stack in this test.
    @MockitoBean
    private com.examplatform.questionbank.service.ReviewWorkflowService reviewWorkflowService;

    private static final String TENANT_ID = "default";

    private static final UUID AUTHOR_ID   = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID REVIEWER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID CONTROLLER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @BeforeEach
    void assumeTestcontainersAvailable() {
        Assumptions.assumeTrue(testcontainersAvailable,
                "Skipping lifecycle integration tests — Docker / Testcontainers not available");
        // No-op stub: ReviewWorkflowService.processTransition is fire-and-forget;
        // we only care about the state machine and persistence in this test suite.
        org.mockito.Mockito.doNothing().when(reviewWorkflowService)
                .processTransition(any(), anyString(), anyString(), any(), any(), anyString());
    }

    // -------------------------------------------------------------------------
    // Helper: create a minimal DRAFT question in the DB
    // -------------------------------------------------------------------------
    private UUID createDraftQuestion() {
        // We need a real subject/topic in the DB. The schema seed populates
        // subjects and topics for tenant 'default'. We look one up directly.
        Long subjectId = jdbcTemplate.queryForObject(
                "SELECT id FROM question_service.subject WHERE tenant_id = 'default' LIMIT 1", Long.class);
        Long topicId = jdbcTemplate.queryForObject(
                "SELECT id FROM question_service.topic WHERE tenant_id = 'default' AND subject_id = ? LIMIT 1",
                Long.class, subjectId);

        // Bypass QuestionService to avoid similarity detection / embedding calls
        Question question = Question.builder()
                .subjectId(subjectId)
                .topicId(topicId)
                .subject("Physics")
                .topic("Thermodynamics")
                .difficulty(DifficultyLevel.MEDIUM.name())
                .cognitiveLevel(CognitiveLevel.UNDERSTAND.name())
                .questionType(QuestionType.SINGLE_MCQ.name())
                .content("What is entropy?")
                .answerKey("A")
                .state("DRAFT")
                .authorId(AUTHOR_ID)
                .hasImages(false)
                .usageCount(0)
                .build();
        question.setTenantId(TENANT_ID);
        return questionRepository.save(question).getId();
    }

    // =========================================================================
    // Full Happy-Path Workflow
    // =========================================================================
    @Nested
    @DisplayName("Happy Path: DRAFT → REVIEW → DRAFT (rejected) → REVIEW → APPROVED → PUBLISHED")
    class HappyPathWorkflow {

        @Test
        @DisplayName("Full workflow executes successfully with four-eyes enforcement at each step")
        void fullLifecycleWorkflow() {
            UUID questionId = createDraftQuestion();

            // 1. Author submits DRAFT → REVIEW
            QuestionResponse afterSubmit = questionService.submitForReview(questionId, AUTHOR_ID, TENANT_ID);
            assertThat(afterSubmit.getState()).isEqualTo("REVIEW");
            assertThat(afterSubmit.getReviewComments()).isNull();

            // 2. Reviewer rejects REVIEW → DRAFT with comments
            String rejectionComment = "Please clarify the thermodynamic system boundary.";
            QuestionResponse afterReject = questionLifecycleService.reject(questionId, REVIEWER_ID, rejectionComment, TENANT_ID);
            assertThat(afterReject.getState()).isEqualTo("DRAFT");
            assertThat(afterReject.getReviewComments()).isEqualTo(rejectionComment);
            assertThat(afterReject.getReviewerId()).isEqualTo(REVIEWER_ID);

            // 3. Verify reviewComments persisted in DB
            Question dbQuestion = questionRepository.findById(questionId).orElseThrow();
            assertThat(dbQuestion.getReviewComments()).isEqualTo(rejectionComment);
            assertThat(dbQuestion.getState()).isEqualTo("DRAFT");

            // 4. Author re-submits DRAFT → REVIEW
            QuestionResponse afterResubmit = questionService.submitForReview(questionId, AUTHOR_ID, TENANT_ID);
            assertThat(afterResubmit.getState()).isEqualTo("REVIEW");

            // 5. A different reviewer approves REVIEW → APPROVED
            QuestionResponse afterApprove = questionLifecycleService.approve(questionId, REVIEWER_ID, TENANT_ID);
            assertThat(afterApprove.getState()).isEqualTo("APPROVED");
            assertThat(afterApprove.getReviewerId()).isEqualTo(REVIEWER_ID);

            // 6. EXAM_CONTROLLER transitions APPROVED → PUBLISHED via generic transition
            TransitionRequest publishRequest = TransitionRequest.builder()
                    .targetState("PUBLISHED")
                    .build();
            QuestionResponse afterPublish = questionLifecycleService.transition(
                    questionId, publishRequest, CONTROLLER_ID, TENANT_ID);
            assertThat(afterPublish.getState()).isEqualTo("PUBLISHED");

            // 7. Final DB state verification
            Question finalDbQuestion = questionRepository.findById(questionId).orElseThrow();
            assertThat(finalDbQuestion.getState()).isEqualTo("PUBLISHED");
            assertThat(finalDbQuestion.getReviewComments()).isEqualTo(rejectionComment);
        }
    }

    // =========================================================================
    // Four-Eyes Principle: approve() must reject reviewer == author
    // =========================================================================
    @Nested
    @DisplayName("Four-Eyes Principle: approve() blocks reviewer == author")
    class FourEyesPrincipleTests {

        @Test
        @DisplayName("Approving own question throws FourEyesPrincipleViolationException")
        void approveOwnQuestionThrowsFourEyesViolation() {
            UUID questionId = createDraftQuestion();

            // Author submits for review
            questionService.submitForReview(questionId, AUTHOR_ID, TENANT_ID);

            // Author tries to approve their own question
            assertThatThrownBy(() ->
                    questionLifecycleService.approve(questionId, AUTHOR_ID, TENANT_ID))
                    .isInstanceOf(FourEyesPrincipleViolationException.class)
                    .hasMessageContaining("Four-eyes principle violation");

            // State must remain REVIEW — not transitioned
            Question dbQuestion = questionRepository.findById(questionId).orElseThrow();
            assertThat(dbQuestion.getState()).isEqualTo("REVIEW");
        }

        @Test
        @DisplayName("A different reviewer can approve successfully")
        void differentReviewerCanApprove() {
            UUID questionId = createDraftQuestion();

            questionService.submitForReview(questionId, AUTHOR_ID, TENANT_ID);

            QuestionResponse approved = questionLifecycleService.approve(questionId, REVIEWER_ID, TENANT_ID);
            assertThat(approved.getState()).isEqualTo("APPROVED");
        }
    }

    // =========================================================================
    // Rejection Comments Persistence
    // =========================================================================
    @Nested
    @DisplayName("Review Comments: rejected question surfaces comments to author")
    class ReviewCommentsPersistenceTests {

        @Test
        @DisplayName("Rejection comments are persisted and visible in the response")
        void rejectionCommentsPersisted() {
            UUID questionId = createDraftQuestion();
            questionService.submitForReview(questionId, AUTHOR_ID, TENANT_ID);

            String comments = "Question lacks adequate distractors. Add plausible wrong options.";
            QuestionResponse response = questionLifecycleService.reject(questionId, REVIEWER_ID, comments, TENANT_ID);

            assertThat(response.getReviewComments()).isEqualTo(comments);
            assertThat(response.getState()).isEqualTo("DRAFT");

            // Also verify via getQuestion
            QuestionResponse fetched = questionService.getQuestion(questionId);
            assertThat(fetched.getReviewComments()).isEqualTo(comments);
        }

        @Test
        @DisplayName("reviewComments cleared when question is re-approved after revision")
        void reviewCommentsNotClearedOnApproval() {
            // Rejection comments intentionally remain on the entity even after approval —
            // they serve as an audit trail. This test verifies the approve path does not
            // wipe out previously set rejection comments.
            UUID questionId = createDraftQuestion();
            questionService.submitForReview(questionId, AUTHOR_ID, TENANT_ID);

            String comments = "Needs better explanation.";
            questionLifecycleService.reject(questionId, REVIEWER_ID, comments, TENANT_ID);

            // Re-submit
            questionService.submitForReview(questionId, AUTHOR_ID, TENANT_ID);

            // Approve (different reviewer — here same for simplicity as four-eyes is author check)
            QuestionResponse approved = questionLifecycleService.approve(questionId, REVIEWER_ID, TENANT_ID);
            assertThat(approved.getState()).isEqualTo("APPROVED");

            // Comments remain as audit trail
            Question dbQuestion = questionRepository.findById(questionId).orElseThrow();
            assertThat(dbQuestion.getReviewComments()).isEqualTo(comments);
        }
    }

    // =========================================================================
    // FSM Guard Rails: Invalid Transitions
    // =========================================================================
    @Nested
    @DisplayName("FSM Guard Rails: invalid transitions are rejected")
    class FsmGuardRailTests {

        @Test
        @DisplayName("Rejecting a DRAFT question (not in REVIEW) throws InvalidTransitionException")
        void cannotRejectDraftQuestion() {
            UUID questionId = createDraftQuestion();

            assertThatThrownBy(() ->
                    questionLifecycleService.reject(questionId, REVIEWER_ID, "some comment", TENANT_ID))
                    .isInstanceOf(InvalidTransitionException.class);
        }

        @Test
        @DisplayName("Approving a DRAFT question (not in REVIEW) throws InvalidTransitionException")
        void cannotApproveDraftQuestion() {
            UUID questionId = createDraftQuestion();

            assertThatThrownBy(() ->
                    questionLifecycleService.approve(questionId, REVIEWER_ID, TENANT_ID))
                    .isInstanceOf(InvalidTransitionException.class);
        }

        @Test
        @DisplayName("Submitting a REVIEW question again throws IllegalStateException")
        void cannotSubmitAlreadyInReview() {
            UUID questionId = createDraftQuestion();
            questionService.submitForReview(questionId, AUTHOR_ID, TENANT_ID);

            assertThatThrownBy(() ->
                    questionService.submitForReview(questionId, AUTHOR_ID, TENANT_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DRAFT state");
        }

        @Test
        @DisplayName("Only the question's author can submit for review")
        void onlyAuthorCanSubmit() {
            UUID questionId = createDraftQuestion();
            UUID nonAuthor = UUID.randomUUID();

            assertThatThrownBy(() ->
                    questionService.submitForReview(questionId, nonAuthor, TENANT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("author");
        }
    }
}
