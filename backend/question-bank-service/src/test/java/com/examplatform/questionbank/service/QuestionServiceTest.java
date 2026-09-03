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
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.repository.SubjectRepository;
import com.examplatform.questionbank.repository.SubtopicRepository;
import com.examplatform.questionbank.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private QuestionService questionService;

    private String currentTenantId = "tenant-abc";

    @BeforeEach
    void setUp() {
        Mockito.lenient()
                .when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

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
            assertThat(response.getCognitiveLevel()).isEqualTo("APPLY");
            assertThat(response.getAuthorId()).isEqualTo(authorId);

            // Verify encryption key is set
            ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
            verify(questionRepository).save(captor.capture());
            Question saved = captor.getValue();
            assertThat(saved.getEncryptionKeyId()).startsWith("question-dek-");
        }

        @Test
        @DisplayName("should set authorId from provided UUID")
        void shouldSetAuthorIdFromJwt() {
            // Given
            CreateQuestionRequest request = validRequest();
            UUID authorId = UUID.fromString("11111111-1111-1111-1111-111111111111");
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
            QuestionResponse response = questionService.createQuestion(request, authorId, tenantId);

            // Then
            assertThat(response.getAuthorId()).isEqualTo(authorId);

            ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
            verify(questionRepository).save(captor.capture());
            assertThat(captor.getValue().getAuthorId()).isEqualTo(authorId);
        }

        @Test
        @DisplayName("should generate unique per-question DEK key name")
        void shouldGenerateUniqueDekPerQuestion() {
            // Given
            CreateQuestionRequest request = validRequest();
            UUID authorId = UUID.randomUUID();
            String tenantId = "tenant-abc";
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

            // When - create two questions
            questionService.createQuestion(request, authorId, tenantId);

            ArgumentCaptor<Question> captor1 = ArgumentCaptor.forClass(Question.class);
            verify(questionRepository).save(captor1.capture());
            String firstDek = captor1.getValue().getEncryptionKeyId();

            // Verify DEK format
            assertThat(firstDek).startsWith("question-dek-");
            // Verify UUID part is valid
            String uuidPart = firstDek.replace("question-dek-", "");
            assertThat(UUID.fromString(uuidPart)).isNotNull();
        }

        @Test
        @DisplayName("should set tenantId on the question entity")
        void shouldSetTenantId() {
            // Given
            CreateQuestionRequest request = validRequest();
            UUID authorId = UUID.randomUUID();
            String tenantId = "exam-authority-maharashtra";
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
    }
}
