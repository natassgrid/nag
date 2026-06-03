package com.examplatform.questionbank.service;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.dto.TransitionRequest;
import com.examplatform.questionbank.exception.FourEyesPrincipleViolationException;
import com.examplatform.questionbank.exception.InvalidTransitionException;
import com.examplatform.questionbank.repository.QuestionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for QuestionLifecycleService — validates FSM transitions
 * and four-eyes principle enforcement.
 *
 * Validates: Requirements 4.6, 5.5
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionLifecycleService")
class QuestionLifecycleServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private QuestionLifecycleService questionLifecycleService;

    private static final UUID QUESTION_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final String TENANT_ID = "tenant-1";

    private Question buildQuestion(String state) {
        Question question = Question.builder()
                .subject("Mathematics")
                .topic("Algebra")
                .difficulty("MEDIUM")
                .cognitiveLevel("UNDERSTAND")
                .questionType("SINGLE_MCQ")
                .content("What is 2+2?")
                .answerKey("4")
                .state(state)
                .authorId(UUID.randomUUID())
                .build();
        // Use reflection-like approach through the protected setter via BaseEntity
        try {
            var setIdMethod = question.getClass().getSuperclass().getDeclaredMethod("setId", UUID.class);
            setIdMethod.setAccessible(true);
            setIdMethod.invoke(question, QUESTION_ID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return question;
    }

    @Nested
    @DisplayName("Valid transitions")
    class ValidTransitions {

        @Test
        @DisplayName("DRAFT → REVIEW succeeds")
        void draftToReviewSucceeds() {
            Question question = buildQuestion("DRAFT");
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
            when(questionRepository.save(any(Question.class))).thenAnswer(i -> i.getArgument(0));

            TransitionRequest request = TransitionRequest.builder()
                    .targetState("REVIEW")
                    .build();

            QuestionResponse response = questionLifecycleService.transition(QUESTION_ID, request, ACTOR_ID, TENANT_ID);

            assertThat(response.getState()).isEqualTo("REVIEW");
        }

        @Test
        @DisplayName("REVIEW → APPROVED succeeds and sets reviewerId")
        void reviewToApprovedSucceedsAndSetsReviewerId() {
            Question question = buildQuestion("REVIEW");
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
            when(questionRepository.save(any(Question.class))).thenAnswer(i -> i.getArgument(0));

            TransitionRequest request = TransitionRequest.builder()
                    .targetState("APPROVED")
                    .build();

            QuestionResponse response = questionLifecycleService.transition(QUESTION_ID, request, ACTOR_ID, TENANT_ID);

            assertThat(response.getState()).isEqualTo("APPROVED");
            assertThat(question.getReviewerId()).isEqualTo(ACTOR_ID);
        }

        @Test
        @DisplayName("REVIEW → DRAFT succeeds")
        void reviewToDraftSucceeds() {
            Question question = buildQuestion("REVIEW");
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
            when(questionRepository.save(any(Question.class))).thenAnswer(i -> i.getArgument(0));

            TransitionRequest request = TransitionRequest.builder()
                    .targetState("DRAFT")
                    .build();

            QuestionResponse response = questionLifecycleService.transition(QUESTION_ID, request, ACTOR_ID, TENANT_ID);

            assertThat(response.getState()).isEqualTo("DRAFT");
        }

        @Test
        @DisplayName("APPROVED → PUBLISHED succeeds with different actor than reviewer")
        void approvedToPublishedSucceedsWithDifferentActor() {
            UUID reviewerId = UUID.randomUUID();
            UUID approverId = UUID.randomUUID();

            Question question = buildQuestion("APPROVED");
            question.setReviewerId(reviewerId);
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
            when(questionRepository.save(any(Question.class))).thenAnswer(i -> i.getArgument(0));

            TransitionRequest request = TransitionRequest.builder()
                    .targetState("PUBLISHED")
                    .build();

            QuestionResponse response = questionLifecycleService.transition(QUESTION_ID, request, approverId, TENANT_ID);

            assertThat(response.getState()).isEqualTo("PUBLISHED");
        }

        @Test
        @DisplayName("PUBLISHED → ARCHIVED succeeds")
        void publishedToArchivedSucceeds() {
            Question question = buildQuestion("PUBLISHED");
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
            when(questionRepository.save(any(Question.class))).thenAnswer(i -> i.getArgument(0));

            TransitionRequest request = TransitionRequest.builder()
                    .targetState("ARCHIVED")
                    .build();

            QuestionResponse response = questionLifecycleService.transition(QUESTION_ID, request, ACTOR_ID, TENANT_ID);

            assertThat(response.getState()).isEqualTo("ARCHIVED");
        }
    }

    @Nested
    @DisplayName("Invalid transitions")
    class InvalidTransitions {

        @Test
        @DisplayName("DRAFT → PUBLISHED throws InvalidTransitionException")
        void draftToPublishedThrows() {
            Question question = buildQuestion("DRAFT");
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));

            TransitionRequest request = TransitionRequest.builder()
                    .targetState("PUBLISHED")
                    .build();

            assertThatThrownBy(() -> questionLifecycleService.transition(QUESTION_ID, request, ACTOR_ID, TENANT_ID))
                    .isInstanceOf(InvalidTransitionException.class)
                    .hasMessageContaining("DRAFT")
                    .hasMessageContaining("PUBLISHED");
        }

        @Test
        @DisplayName("ARCHIVED → DRAFT throws InvalidTransitionException")
        void archivedToDraftThrows() {
            Question question = buildQuestion("ARCHIVED");
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));

            TransitionRequest request = TransitionRequest.builder()
                    .targetState("DRAFT")
                    .build();

            assertThatThrownBy(() -> questionLifecycleService.transition(QUESTION_ID, request, ACTOR_ID, TENANT_ID))
                    .isInstanceOf(InvalidTransitionException.class)
                    .hasMessageContaining("ARCHIVED")
                    .hasMessageContaining("DRAFT");
        }
    }

    @Nested
    @DisplayName("Four-eyes principle")
    class FourEyesPrinciple {

        @Test
        @DisplayName("APPROVED → PUBLISHED fails when actor == reviewer")
        void approvedToPublishedFailsWhenActorIsReviewer() {
            UUID sameActorId = UUID.randomUUID();

            Question question = buildQuestion("APPROVED");
            question.setReviewerId(sameActorId);
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));

            TransitionRequest request = TransitionRequest.builder()
                    .targetState("PUBLISHED")
                    .build();

            assertThatThrownBy(() -> questionLifecycleService.transition(QUESTION_ID, request, sameActorId, TENANT_ID))
                    .isInstanceOf(FourEyesPrincipleViolationException.class)
                    .hasMessageContaining("Four-eyes principle violation");
        }
    }

    @Nested
    @DisplayName("Question not found")
    class QuestionNotFound {

        @Test
        @DisplayName("throws EntityNotFoundException when question does not exist")
        void throwsWhenQuestionNotFound() {
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.empty());

            TransitionRequest request = TransitionRequest.builder()
                    .targetState("REVIEW")
                    .build();

            assertThatThrownBy(() -> questionLifecycleService.transition(QUESTION_ID, request, ACTOR_ID, TENANT_ID))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }
}
