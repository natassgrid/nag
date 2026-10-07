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

import com.examplatform.questionbank.ai.embedding.EmbeddingService;
import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.domain.Subject;
import com.examplatform.questionbank.domain.Topic;
import com.examplatform.questionbank.domain.enums.CognitiveLevel;
import com.examplatform.questionbank.domain.enums.DifficultyLevel;
import com.examplatform.questionbank.domain.enums.QuestionType;
import com.examplatform.questionbank.dto.CreateQuestionRequest;
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.repository.SubjectRepository;
import com.examplatform.questionbank.repository.SubtopicRepository;
import com.examplatform.questionbank.repository.TopicRepository;
import com.examplatform.questionbank.translation.domain.Translation;
import com.examplatform.questionbank.translation.repository.TranslationRepository;
import com.examplatform.questionbank.repository.QuestionVersionRepository;
import com.examplatform.questionbank.domain.QuestionVersion;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.examplatform.shared.messaging.EventPublisher;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link QuestionService}.
 *
 * Validates: Requirements 4.1, 4.2, 4.3, 4.5
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionService")
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private SubtopicRepository subtopicRepository;

    @Mock
    private SimilarityDetectionService similarityDetectionService;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private TranslationRepository translationRepository;

    @Mock
    private QuestionVersionRepository questionVersionRepository;

    @InjectMocks
    private QuestionService questionService;

    private String currentTenantId = "tenant-abc";

    @BeforeEach
    void setUp() {

        Mockito.lenient()
                .when(subjectRepository.findById(any()))
                .thenAnswer(inv -> {
                    Long id = inv.getArgument(0);
                    Subject s = Subject.builder().name("Mathematics").code("MATH").build();
                    s.setTenantId(currentTenantId);
                    try {
                        var idField = s.getClass().getSuperclass().getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(s, id != null ? id : 1L);
                    } catch (Exception e) {}
                    return Optional.of(s);
                });

        Mockito.lenient()
                .when(topicRepository.findById(any()))
                .thenAnswer(inv -> {
                    Long id = inv.getArgument(0);
                    Topic t = Topic.builder().name("Calculus").subjectId(1L).build();
                    t.setTenantId(currentTenantId);
                    try {
                        var idField = t.getClass().getSuperclass().getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(t, id != null ? id : 10L);
                    } catch (Exception e) {}
                    return Optional.of(t);
                });

        // Set @Value field that isn't injected by @InjectMocks
        try {
            var field = questionService.getClass().getDeclaredField("encryptionEnabled");
            field.setAccessible(true);
            field.set(questionService, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CreateQuestionRequest validRequest() {
        return CreateQuestionRequest.builder()
                .subjectId(1L)
                .topicId(10L)
                .subject("Mathematics")
                .topic("Calculus")
                .subtopic("Differentiation")
                .chapter("Chapter 5")
                .difficulty(DifficultyLevel.MEDIUM)
                .cognitiveLevel(CognitiveLevel.APPLY)
                .questionType(QuestionType.SINGLE_MCQ)
                .content("<p>Find the derivative of x^2</p>")
                .answerKey("{\"correct\": \"2x\"}")
                .contentType("HTML5")
                .build();
    }

    @Nested
    @DisplayName("createQuestion")
    class CreateQuestion {

        @Test
        @DisplayName("should persist question in DRAFT state with encryption key")
        void shouldPersistInDraftStateWithEncryptionKey() {
            // Given
            CreateQuestionRequest request = validRequest();
            UUID authorId = UUID.randomUUID();
            String tenantId = "tenant-abc";
            currentTenantId = tenantId;

            when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
                Question q = invocation.getArgument(0);
                // Simulate BaseEntity prePersist behavior
                try {
                    var idField = q.getClass().getSuperclass().getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(q, UUID.randomUUID());
                    var createdAtField = q.getClass().getSuperclass().getDeclaredField("createdAt");
                    createdAtField.setAccessible(true);
                    createdAtField.set(q, Instant.now());
                } catch (Exception e) {
                    // fall through
                }
                return q;
            });

            // When
            QuestionResponse response = questionService.createQuestion(request, authorId, tenantId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getState()).isEqualTo("DRAFT");
            assertThat(response.getSubject()).isEqualTo("Mathematics");
            assertThat(response.getTopic()).isEqualTo("Calculus");
            assertThat(response.getQuestionType()).isEqualTo("SINGLE_MCQ");
            assertThat(response.getDifficulty()).isEqualTo("MEDIUM");
            assertThat(response.getContent()).isEqualTo("<p>Find the derivative of x^2</p>");
            assertThat(response.getEncryptionKeyId()).startsWith("question-dek-");

            ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
            verify(questionRepository).save(captor.capture());
            assertThat(captor.getValue().getTenantId()).isEqualTo(tenantId);
            assertThat(captor.getValue().getState()).isEqualTo("DRAFT");
        }
        @Test
        @DisplayName("should persist question in APPROVED state when state is explicitly provided")
        void shouldPersistInApprovedStateWhenStateIsExplicitlyProvided() {
            // Given
            CreateQuestionRequest request = validRequest();
            request.setState("APPROVED");
            UUID authorId = UUID.randomUUID();
            String tenantId = "tenant-approved";
            currentTenantId = tenantId;

            when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
                Question q = invocation.getArgument(0);
                return q;
            });

            // When
            QuestionResponse response = questionService.createQuestion(request, authorId, tenantId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getState()).isEqualTo("APPROVED");

            ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
            verify(questionRepository, Mockito.atLeastOnce()).save(captor.capture());
            assertThat(captor.getValue().getState()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("should deserialize JSON payload with type alias and state into CreateQuestionRequest")
        void shouldDeserializeWithTypeAliasAndState() throws Exception {
            String json = """
                {
                    "subjectId": 1,
                    "topicId": 10,
                    "type": "SINGLE_MCQ",
                    "difficulty": "MEDIUM",
                    "cognitiveLevel": "APPLY",
                    "content": "Test content",
                    "state": "APPROVED"
                }
                """;
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            CreateQuestionRequest req = mapper.readValue(json, CreateQuestionRequest.class);
            assertThat(req.getQuestionType()).isEqualTo(QuestionType.SINGLE_MCQ);
            assertThat(req.getState()).isEqualTo("APPROVED");
        }
        @Test
        @DisplayName("should default to DRAFT state when not specified in CreateQuestionRequest")
        void shouldDefaultToDraftStateWhenNotSpecified() throws Exception {
            String json = """
                {
                    "subjectId": 1,
                    "topicId": 10,
                    "type": "SINGLE_MCQ",
                    "difficulty": "MEDIUM",
                    "cognitiveLevel": "APPLY",
                    "content": "Test content"
                }
                """;
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            CreateQuestionRequest req = mapper.readValue(json, CreateQuestionRequest.class);
            assertThat(req.getState()).isEqualTo("DRAFT");
        }

        @Test
        @DisplayName("should associate question with tenantId from context")
        void shouldAssociateWithTenantId() {
            // Given
            CreateQuestionRequest request = validRequest();
            UUID authorId = UUID.randomUUID();
            String tenantId = "tenant-xyz";
            currentTenantId = tenantId;

            when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
                Question q = invocation.getArgument(0);
                try {
                    var idField = q.getClass().getSuperclass().getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(q, UUID.randomUUID());
                    var createdAtField = q.getClass().getSuperclass().getDeclaredField("createdAt");
                    createdAtField.setAccessible(true);
                    createdAtField.set(q, Instant.now());
                } catch (Exception e) {
                    // fall through
                }
                return q;
            });

            // When
            questionService.createQuestion(request, authorId, tenantId);

            // Then
            ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
            verify(questionRepository).save(captor.capture());
            assertThat(captor.getValue().getTenantId()).isEqualTo(tenantId);
        }

        @Test
        @DisplayName("should throw when question type is null")
        void shouldThrowWhenQuestionTypeIsNull() {
            // Given
            CreateQuestionRequest request = validRequest();
            request.setQuestionType(null);
            UUID authorId = UUID.randomUUID();
            String tenantId = "tenant-abc";

            // When / Then
            assertThatThrownBy(() -> questionService.createQuestion(request, authorId, tenantId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("supported types");
        }

        @Test
        @DisplayName("should set hasImages to false when content and options have no images")
        void shouldSetHasImagesFalseWhenNoImages() {
            CreateQuestionRequest request = validRequest();
            when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

            QuestionResponse response = questionService.createQuestion(request, UUID.randomUUID(), "tenant-abc");

            assertThat(response.isHasImages()).isFalse();
            ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
            verify(questionRepository).save(captor.capture());
            assertThat(captor.getValue().isHasImages()).isFalse();
        }

        @Test
        @DisplayName("should set hasImages to true when content contains markdown image syntax")
        void shouldSetHasImagesTrueWhenContentHasMarkdownImage() {
            CreateQuestionRequest request = validRequest();
            request.setContent("<p>Look at diagram: ![circuit](https://cdn.example.com/c1.svg)</p>");
            when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

            QuestionResponse response = questionService.createQuestion(request, UUID.randomUUID(), "tenant-abc");

            assertThat(response.isHasImages()).isTrue();
            ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
            verify(questionRepository).save(captor.capture());
            assertThat(captor.getValue().isHasImages()).isTrue();
        }

        @Test
        @DisplayName("should set hasImages to true when content contains img tag")
        void shouldSetHasImagesTrueWhenContentHasImgTag() {
            CreateQuestionRequest request = validRequest();
            request.setContent("<p><img src=\"https://cdn.example.com/photo.png\" alt=\"cell\" /></p>");
            when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

            QuestionResponse response = questionService.createQuestion(request, UUID.randomUUID(), "tenant-abc");

            assertThat(response.isHasImages()).isTrue();
        }

        @Test
        @DisplayName("should set hasImages to true when an option has imageUrl")
        void shouldSetHasImagesTrueWhenOptionHasImageUrl() {
            CreateQuestionRequest request = validRequest();
            request.setOptions(List.of(
                    QuestionOption.builder().id("A").text("Option A").correct(true).build(),
                    QuestionOption.builder().id("B").imageUrl("https://cdn.example.com/opt-b.svg").correct(false).build()
            ));
            when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

            QuestionResponse response = questionService.createQuestion(request, UUID.randomUUID(), "tenant-abc");

            assertThat(response.isHasImages()).isTrue();
        }
    }

    @Nested
    @DisplayName("listQuestions with translation filters and metadata")
    class ListQuestionsTranslationTests {

        @Test
        @DisplayName("should attach translation status and language metadata to questions")
        void shouldAttachTranslationMetadata() {
            UUID q1Id = UUID.randomUUID();
            UUID q2Id = UUID.randomUUID();

            Question q1 = Question.builder()
                    .subject("Physics")
                    .topic("Mechanics")
                    .difficulty("MEDIUM")
                    .state("APPROVED")
                    .content("What is Newton's second law?")
                    .build();
            try {
                var idField = q1.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(q1, q1Id);
            } catch (Exception ignored) {}

            Question q2 = Question.builder()
                    .subject("Physics")
                    .topic("Thermodynamics")
                    .difficulty("EASY")
                    .state("APPROVED")
                    .content("Define isothermal process.")
                    .build();
            try {
                var idField = q2.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(q2, q2Id);
            } catch (Exception ignored) {}

            Page<Question> questionPage = new PageImpl<>(List.of(q1, q2));
            when(questionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(questionPage);

            Translation t1 = Translation.builder()
                    .questionId(q1Id)
                    .languageCode("hi")
                    .status(Translation.TranslationStatus.APPROVED)
                    .build();

            Translation t2 = Translation.builder()
                    .questionId(q1Id)
                    .languageCode("ta")
                    .status(Translation.TranslationStatus.DRAFT)
                    .build();

            when(translationRepository.findByQuestionIdsAndTenantId(eq(List.of(q1Id, q2Id)), eq("tenant-abc")))
                    .thenReturn(List.of(t1, t2));

            Page<QuestionResponse> result = questionService.listQuestions(
                    "Physics", null, null, null, null, null, "Newton law",
                    "hi", "APPROVED", 0, 20, "tenant-abc");

            assertThat(result.getContent()).hasSize(2);

            QuestionResponse resp1 = result.getContent().get(0);
            assertThat(resp1.getId()).isEqualTo(q1Id);
            assertThat(resp1.getTranslatedLanguages()).containsExactlyInAnyOrder("hi", "ta");
            assertThat(resp1.getTranslationStatusMap()).containsEntry("hi", "APPROVED");
            assertThat(resp1.getTranslationStatusMap()).containsEntry("ta", "DRAFT");
            assertThat(resp1.getTranslationStatus()).isEqualTo("APPROVED");

            QuestionResponse resp2 = result.getContent().get(1);
            assertThat(resp2.getId()).isEqualTo(q2Id);
            assertThat(resp2.getTranslatedLanguages()).isEmpty();
            assertThat(resp2.getTranslationStatusMap()).isEmpty();
            assertThat(resp2.getTranslationStatus()).isEqualTo("MISSING");
        }

        @Test
        @DisplayName("searchLike specification builds multi-field predicates")
        void searchLikeBuildsSpecification() {
            Specification<Question> singleTokenSpec = questionService.searchLike("Physics");
            assertThat(singleTokenSpec).isNotNull();

            Specification<Question> multiTokenSpec = questionService.searchLike("Quantitative Aptitude EASY");
            assertThat(multiTokenSpec).isNotNull();

            Specification<Question> nullSpec = questionService.searchLike("   ");
            assertThat(nullSpec).isNull();
        }
    }

    @Nested
    @DisplayName("deleteQuestion")
    class DeleteQuestionTests {

        @Test
        @DisplayName("should delete draft question and clean up versions and translations")
        void deleteDraftQuestionSuccessfully() {
            UUID qId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            Question question = Question.builder()
                    .subject("Physics")
                    .topic("Mechanics")
                    .difficulty("MEDIUM")
                    .questionType("SINGLE_MCQ")
                    .state("DRAFT")
                    .authorId(authorId)
                    .usageCount(0)
                    .build();
            question.setTenantId("tenant-abc");

            when(questionRepository.findById(qId)).thenReturn(Optional.of(question));
            when(translationRepository.findByQuestionIdAndTenantId(qId, "tenant-abc")).thenReturn(List.of(
                    Translation.builder().questionId(qId).languageCode("hi").status(Translation.TranslationStatus.DRAFT).build()
            ));
            when(questionVersionRepository.findByQuestionIdOrderByVersionNumberDesc(qId)).thenReturn(List.of(
                    QuestionVersion.builder().questionId(qId).versionNumber(1).build()
            ));

            questionService.deleteQuestion(qId, authorId, "tenant-abc");

            verify(translationRepository).deleteAll(any());
            verify(questionVersionRepository).deleteAll(any());
            verify(questionRepository).delete(question);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException if question does not exist")
        void deleteQuestionNotFound() {
            UUID qId = UUID.randomUUID();
            when(questionRepository.findById(qId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> questionService.deleteQuestion(qId, UUID.randomUUID(), "tenant-abc"))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException if tenant does not match")
        void deleteQuestionTenantMismatch() {
            UUID qId = UUID.randomUUID();
            Question question = Question.builder().state("DRAFT").usageCount(0).build();
            question.setTenantId("other-tenant");
            when(questionRepository.findById(qId)).thenReturn(Optional.of(question));

            assertThatThrownBy(() -> questionService.deleteQuestion(qId, UUID.randomUUID(), "tenant-abc"))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("should throw IllegalStateException if question is in PUBLISHED state")
        void deletePublishedQuestionFails() {
            UUID qId = UUID.randomUUID();
            Question question = Question.builder().state("PUBLISHED").usageCount(0).build();
            question.setTenantId("tenant-abc");
            when(questionRepository.findById(qId)).thenReturn(Optional.of(question));

            assertThatThrownBy(() -> questionService.deleteQuestion(qId, UUID.randomUUID(), "tenant-abc"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PUBLISHED");
        }

        @Test
        @DisplayName("should throw IllegalStateException if question has active exam usage")
        void deleteUsedQuestionFails() {
            UUID qId = UUID.randomUUID();
            Question question = Question.builder().state("DRAFT").usageCount(3).build();
            question.setTenantId("tenant-abc");
            when(questionRepository.findById(qId)).thenReturn(Optional.of(question));

            assertThatThrownBy(() -> questionService.deleteQuestion(qId, UUID.randomUUID(), "tenant-abc"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("usage");
        }
    }

    @Nested
    @DisplayName("createQuestionFromGenerated")
    class CreateQuestionFromGeneratedTests {

        @Test
        @DisplayName("should create question from generated request with default DRAFT state")
        void createFromGenerated() {
            UUID authorId = UUID.randomUUID();
            Question savedQuestion = Question.builder()
                    .subjectId(1L)
                    .topicId(10L)
                    .subject("Mathematics")
                    .topic("Calculus")
                    .difficulty("MEDIUM")
                    .cognitiveLevel("APPLY")
                    .questionType("SINGLE_MCQ")
                    .content("What is the derivative of x^2?")
                    .state("DRAFT")
                    .build();
            try { var idField = savedQuestion.getClass().getSuperclass().getDeclaredField("id"); idField.setAccessible(true); idField.set(savedQuestion, UUID.randomUUID()); } catch (Exception ignored) {}
            when(questionRepository.save(any(Question.class))).thenReturn(savedQuestion);

            CreateQuestionRequest request = CreateQuestionRequest.builder()
                    .subjectId(1L)
                    .topicId(10L)
                    .difficulty(DifficultyLevel.MEDIUM)
                    .cognitiveLevel(CognitiveLevel.APPLY)
                    .questionType(QuestionType.SINGLE_MCQ)
                    .content("What is the derivative of x^2?")
                    .build();

            QuestionResponse response = questionService.createQuestionFromGenerated(request, authorId, currentTenantId);

            assertThat(response).isNotNull();
            assertThat(response.getState()).isEqualTo("DRAFT");
            assertThat(response.getContent()).isEqualTo("What is the derivative of x^2?");
        }

        @Test
        @DisplayName("detectHasImages should identify data URI images")
        void detectHasDataUriImages() {
            boolean hasImage = QuestionService.detectHasImages(
                    "Refer to diagram: data:image/jpeg;base64,/9j/4AAQSkZJRg==",
                    null,
                    null);
            assertThat(hasImage).isTrue();
        }
    }
}
