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
import com.examplatform.questionbank.ai.similarity.SimilarityCheckResult;
import com.examplatform.questionbank.ai.similarity.SimilarityCheckResult.Status;
import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.domain.enums.CognitiveLevel;
import com.examplatform.questionbank.domain.enums.DifficultyLevel;
import com.examplatform.questionbank.domain.enums.QuestionType;
import com.examplatform.questionbank.dto.CreateQuestionRequest;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.exception.SimilarQuestionException;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.repository.SimilarityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for the full duplicate detection flow during question creation.
 * Tests the interaction between {@link QuestionService} and {@link SimilarityDetectionService},
 * covering the end-to-end behavior from question submission through similarity checking
 * to final response (including warnings and rejections).
 *
 * <p>Since pgvector is not available in unit tests, we mock {@link EmbeddingService} and
 * {@link QuestionRepository} while testing the real service-layer logic of
 * {@link SimilarityDetectionService} integrated with {@link QuestionService}.
 *
 * <p>Validates: Requirements FR-2 (Duplicate Detection), NFR-2 (Resilience)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Duplicate Detection Integration Test — Full Flow (FR-2, NFR-2)")
class DuplicateDetectionIntegrationTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private com.examplatform.questionbank.repository.SubjectRepository subjectRepository;

    @Mock
    private com.examplatform.questionbank.repository.TopicRepository topicRepository;

    @Mock
    private com.examplatform.questionbank.repository.SubtopicRepository subtopicRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    // Real SimilarityDetectionService with mocked dependencies
    private SimilarityDetectionService similarityDetectionService;

    // QuestionService under test — wired with the real SimilarityDetectionService
    private QuestionService questionService;

    private static final String SUBJECT = "Mathematics";
    private static final String TENANT_ID = "tenant-integration-test";
    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final float[] DUMMY_EMBEDDING = new float[384];

    @BeforeEach
    void setUp() {
        // Initialize a 384-dim embedding with sample values
        for (int i = 0; i < DUMMY_EMBEDDING.length; i++) {
            DUMMY_EMBEDDING[i] = (float) Math.sin(i * 0.1);
        }

        // Wire real SimilarityDetectionService with mocked dependencies
        similarityDetectionService = new SimilarityDetectionService(embeddingService, questionRepository);

        // Wire QuestionService with real SimilarityDetectionService and mocked dependencies
        questionService = new QuestionService(
                questionRepository,
                subjectRepository,
                topicRepository,
                subtopicRepository,
                similarityDetectionService,
                embeddingService,
                kafkaTemplate
        );

        // Set encryptionEnabled = false to avoid Vault dependency in tests
        try {
            var field = QuestionService.class.getDeclaredField("encryptionEnabled");
            field.setAccessible(true);
            field.set(questionService, false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set encryptionEnabled field", e);
        }

        // Lenient stub for Kafka — fire-and-forget audit events
        lenient().when(kafkaTemplate.send(anyString(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Lenient stub for the Subject -> Topic -> Subtopic hierarchy resolution
        stubHierarchy();
    }

    private static final Long SUBJECT_ID = 1L;
    private static final Long TOPIC_ID = 2L;
    private static final Long SUBTOPIC_ID = 3L;

    private CreateQuestionRequest createMcqRequest(String content) {
        return CreateQuestionRequest.builder()
                .subjectId(SUBJECT_ID)
                .topicId(TOPIC_ID)
                .subtopicId(SUBTOPIC_ID)
                .subject(SUBJECT)
                .topic("Calculus")
                .subtopic("Differentiation")
                .chapter("Chapter 1")
                .difficulty(DifficultyLevel.MEDIUM)
                .cognitiveLevel(CognitiveLevel.APPLY)
                .questionType(QuestionType.SINGLE_MCQ)
                .content(content)
                .answerKey("A")
                .build();
    }

    /**
     * Stubs the Subject -> Topic -> Subtopic lookups so
     * {@link QuestionService#resolveHierarchy} resolves successfully to the
     * fixed test ids/names. Lenient because rejection scenarios never reach save
     * but still resolve the hierarchy first.
     */
    private void stubHierarchy() {
        com.examplatform.questionbank.domain.Subject subject =
                com.examplatform.questionbank.domain.Subject.builder().name(SUBJECT).build();
        subject.setTenantId(TENANT_ID);
        setNumericId(subject, SUBJECT_ID);

        com.examplatform.questionbank.domain.Topic topic =
                com.examplatform.questionbank.domain.Topic.builder().subjectId(SUBJECT_ID).name("Calculus").build();
        topic.setTenantId(TENANT_ID);
        setNumericId(topic, TOPIC_ID);

        com.examplatform.questionbank.domain.Subtopic subtopic =
                com.examplatform.questionbank.domain.Subtopic.builder().topicId(TOPIC_ID).name("Differentiation").build();
        subtopic.setTenantId(TENANT_ID);
        setNumericId(subtopic, SUBTOPIC_ID);

        lenient().when(subjectRepository.findById(SUBJECT_ID)).thenReturn(java.util.Optional.of(subject));
        lenient().when(topicRepository.findById(TOPIC_ID)).thenReturn(java.util.Optional.of(topic));
        lenient().when(subtopicRepository.findById(SUBTOPIC_ID)).thenReturn(java.util.Optional.of(subtopic));
    }

    private static void setNumericId(Object entity, Long id) {
        try {
            var f = entity.getClass().getSuperclass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set numeric id in test", e);
        }
    }

    private void stubRepositorySave() {
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question q = invocation.getArgument(0);
            try {
                var idField = q.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                if (idField.get(q) == null) {
                    idField.set(q, UUID.randomUUID());
                }
                var createdAtField = q.getClass().getSuperclass().getDeclaredField("createdAt");
                createdAtField.setAccessible(true);
                if (createdAtField.get(q) == null) {
                    createdAtField.set(q, Instant.now());
                }
            } catch (Exception e) {
                // fall through
            }
            return q;
        });
    }

    private SimilarityResult mockSimilarityResult(UUID id, double similarity, String content) {
        return new SimilarityResult() {
            @Override
            public UUID getId() { return id; }

            @Override
            public String getSubject() { return SUBJECT; }

            @Override
            public String getContent() { return content; }

            @Override
            public Double getSimilarity() { return similarity; }
        };
    }

    @Nested
    @DisplayName("Scenario 1: Near-duplicate rejection (similarity > 0.92)")
    class NearDuplicateRejection {

        @Test
        @DisplayName("should reject question creation when existing question has similarity > 0.92")
        void rejectsCreation_whenNearDuplicateDetected() {
            // Given: embedding service returns a valid embedding
            when(embeddingService.embed(anyString())).thenReturn(DUMMY_EMBEDDING);

            // Given: repository finds a question with 0.95 similarity (above REJECT threshold of 0.92)
            UUID existingQuestionId = UUID.randomUUID();
            when(questionRepository.findTopSimilarQuestions(anyString(), eq(SUBJECT), eq(TENANT_ID), anyInt()))
                    .thenReturn(List.of(mockSimilarityResult(existingQuestionId, 0.95,
                            "What is the derivative of x^2?")));

            CreateQuestionRequest request = createMcqRequest("What is the derivative of x squared?");

            // When/Then: creation should throw SimilarQuestionException (HTTP 422)
            assertThatThrownBy(() -> questionService.createQuestion(request, AUTHOR_ID, TENANT_ID))
                    .isInstanceOf(SimilarQuestionException.class)
                    .hasMessageContaining(existingQuestionId.toString());

            // Verify: question was NOT persisted (rejected before save)
            verify(questionRepository, never()).save(any(Question.class));
        }

        @Test
        @DisplayName("should reject at exact threshold boundary (0.921)")
        void rejectsCreation_atBoundaryAboveThreshold() {
            when(embeddingService.embed(anyString())).thenReturn(DUMMY_EMBEDDING);

            UUID existingId = UUID.randomUUID();
            when(questionRepository.findTopSimilarQuestions(anyString(), eq(SUBJECT), eq(TENANT_ID), anyInt()))
                    .thenReturn(List.of(mockSimilarityResult(existingId, 0.921, "Near-duplicate content")));

            CreateQuestionRequest request = createMcqRequest("Near-duplicate content rephrased");

            assertThatThrownBy(() -> questionService.createQuestion(request, AUTHOR_ID, TENANT_ID))
                    .isInstanceOf(SimilarQuestionException.class);

            verify(questionRepository, never()).save(any(Question.class));
        }
    }

    @Nested
    @DisplayName("Scenario 2: Warning flag for human review (similarity 0.85–0.92)")
    class WarningFlagForReview {

        @Test
        @DisplayName("should create question with warnings when similarity is between 0.85 and 0.92")
        void createsQuestion_withWarnings_whenModeratelySimilar() {
            // Given: embedding service works normally
            when(embeddingService.embed(anyString())).thenReturn(DUMMY_EMBEDDING);
            stubRepositorySave();

            // Given: repository finds a question with 0.88 similarity (in WARN range)
            UUID similarQuestionId = UUID.randomUUID();
            when(questionRepository.findTopSimilarQuestions(anyString(), eq(SUBJECT), eq(TENANT_ID), anyInt()))
                    .thenReturn(List.of(mockSimilarityResult(similarQuestionId, 0.88,
                            "Explain the concept of limits in calculus")));

            CreateQuestionRequest request = createMcqRequest("Describe the fundamental concept of limits");

            // When
            QuestionResponse response = questionService.createQuestion(request, AUTHOR_ID, TENANT_ID);

            // Then: question was created successfully
            assertThat(response).isNotNull();
            assertThat(response.getState()).isEqualTo("DRAFT");

            // Then: response contains similarity warnings for human review (FR-2)
            assertThat(response.getWarnings()).isNotNull();
            assertThat(response.getWarnings()).hasSize(1);
            assertThat(response.getWarnings().getFirst().getQuestionId()).isEqualTo(similarQuestionId);
            assertThat(response.getWarnings().getFirst().getSimilarity()).isEqualTo(0.88);

            // Verify: question was persisted
            verify(questionRepository, atLeastOnce()).save(any(Question.class));
        }

        @Test
        @DisplayName("should include multiple warnings when several questions are in warn range")
        void createsQuestion_withMultipleWarnings() {
            when(embeddingService.embed(anyString())).thenReturn(DUMMY_EMBEDDING);
            stubRepositorySave();

            UUID similar1 = UUID.randomUUID();
            UUID similar2 = UUID.randomUUID();
            when(questionRepository.findTopSimilarQuestions(anyString(), eq(SUBJECT), eq(TENANT_ID), anyInt()))
                    .thenReturn(List.of(
                            mockSimilarityResult(similar1, 0.90, "Question about derivatives"),
                            mockSimilarityResult(similar2, 0.86, "Another calculus question")
                    ));

            CreateQuestionRequest request = createMcqRequest("A question about derivatives and calculus");

            QuestionResponse response = questionService.createQuestion(request, AUTHOR_ID, TENANT_ID);

            assertThat(response.getWarnings()).hasSize(2);
            assertThat(response.getWarnings())
                    .extracting(QuestionResponse.SimilarQuestionWarning::getSimilarity)
                    .containsExactlyInAnyOrder(0.90, 0.86);
        }
    }

    @Nested
    @DisplayName("Scenario 3: Unique question passes without warnings")
    class UniqueQuestionPasses {

        @Test
        @DisplayName("should create question without warnings when content is unique (no similar questions)")
        void createsQuestion_noWarnings_whenNoSimilarQuestions() {
            when(embeddingService.embed(anyString())).thenReturn(DUMMY_EMBEDDING);
            stubRepositorySave();

            // No similar questions found
            when(questionRepository.findTopSimilarQuestions(anyString(), eq(SUBJECT), eq(TENANT_ID), anyInt()))
                    .thenReturn(Collections.emptyList());

            CreateQuestionRequest request = createMcqRequest("A completely unique question about topology");

            QuestionResponse response = questionService.createQuestion(request, AUTHOR_ID, TENANT_ID);

            assertThat(response).isNotNull();
            assertThat(response.getState()).isEqualTo("DRAFT");
            assertThat(response.getWarnings()).isNull();

            verify(questionRepository, atLeastOnce()).save(any(Question.class));
        }

        @Test
        @DisplayName("should create question without warnings when all similarities are below 0.85")
        void createsQuestion_noWarnings_whenAllBelowWarnThreshold() {
            when(embeddingService.embed(anyString())).thenReturn(DUMMY_EMBEDDING);
            stubRepositorySave();

            // All results below WARN threshold
            when(questionRepository.findTopSimilarQuestions(anyString(), eq(SUBJECT), eq(TENANT_ID), anyInt()))
                    .thenReturn(List.of(
                            mockSimilarityResult(UUID.randomUUID(), 0.60, "Unrelated question"),
                            mockSimilarityResult(UUID.randomUUID(), 0.72, "Somewhat related")
                    ));

            CreateQuestionRequest request = createMcqRequest("Explain Newton's third law of motion");

            QuestionResponse response = questionService.createQuestion(request, AUTHOR_ID, TENANT_ID);

            assertThat(response).isNotNull();
            assertThat(response.getState()).isEqualTo("DRAFT");
            assertThat(response.getWarnings()).isNull();
        }
    }

    @Nested
    @DisplayName("Scenario 4: Resilience — embedding service unavailable (NFR-2)")
    class ResilienceWhenServiceUnavailable {

        @Test
        @DisplayName("should create question successfully when embedding service throws exception")
        void createsQuestion_whenEmbeddingServiceFails() {
            // Given: embedding service throws an exception (service unavailable)
            when(embeddingService.embed(anyString()))
                    .thenThrow(new RuntimeException("Connection refused: LiteLLM unavailable"));
            stubRepositorySave();

            CreateQuestionRequest request = createMcqRequest("Question when LLM is down");

            // When: question creation should still succeed (NFR-2)
            QuestionResponse response = questionService.createQuestion(request, AUTHOR_ID, TENANT_ID);

            // Then: question was created without warnings (duplicate detection skipped gracefully)
            assertThat(response).isNotNull();
            assertThat(response.getState()).isEqualTo("DRAFT");
            assertThat(response.getWarnings()).isNull();

            // Verify: question was still persisted despite embedding service failure
            verify(questionRepository, atLeastOnce()).save(any(Question.class));
        }

        @Test
        @DisplayName("should create question successfully when repository similarity query fails")
        void createsQuestion_whenRepositorySimilarityQueryFails() {
            // Given: embedding works but the pgvector query fails
            when(embeddingService.embed(anyString())).thenReturn(DUMMY_EMBEDDING);
            when(questionRepository.findTopSimilarQuestions(anyString(), eq(SUBJECT), eq(TENANT_ID), anyInt()))
                    .thenThrow(new RuntimeException("PostgreSQL pgvector extension not available"));
            stubRepositorySave();

            CreateQuestionRequest request = createMcqRequest("Question when pgvector is down");

            // When: question creation should still succeed (NFR-2)
            QuestionResponse response = questionService.createQuestion(request, AUTHOR_ID, TENANT_ID);

            // Then: question was created without warnings
            assertThat(response).isNotNull();
            assertThat(response.getState()).isEqualTo("DRAFT");

            // Verify: question was persisted
            verify(questionRepository, atLeastOnce()).save(any(Question.class));
        }
    }

    @Nested
    @DisplayName("SimilarityDetectionService — threshold boundary tests")
    class ThresholdBoundaryTests {

        @Test
        @DisplayName("similarity at exactly 0.92 should be classified as WARN, not REJECT")
        void exactlyAtRejectThreshold_shouldBeWarn() {
            when(embeddingService.embed(anyString())).thenReturn(DUMMY_EMBEDDING);
            when(questionRepository.findTopSimilarQuestions(anyString(), eq(SUBJECT), eq(TENANT_ID), anyInt()))
                    .thenReturn(List.of(mockSimilarityResult(UUID.randomUUID(), 0.92, "Boundary question")));

            SimilarityCheckResult result = similarityDetectionService.checkSimilarity(
                    "Test content", SUBJECT, TENANT_ID);

            // 0.92 is NOT > 0.92, so it should be WARN (it IS > 0.85)
            assertThat(result.status()).isEqualTo(Status.WARN);
        }

        @Test
        @DisplayName("similarity at exactly 0.85 should be classified as PASS, not WARN")
        void exactlyAtWarnThreshold_shouldBePass() {
            when(embeddingService.embed(anyString())).thenReturn(DUMMY_EMBEDDING);
            when(questionRepository.findTopSimilarQuestions(anyString(), eq(SUBJECT), eq(TENANT_ID), anyInt()))
                    .thenReturn(List.of(mockSimilarityResult(UUID.randomUUID(), 0.85, "Boundary question")));

            SimilarityCheckResult result = similarityDetectionService.checkSimilarity(
                    "Test content", SUBJECT, TENANT_ID);

            // 0.85 is NOT > 0.85, so it should be PASS
            assertThat(result.status()).isEqualTo(Status.PASS);
        }

        @Test
        @DisplayName("similarity at 0.8501 should be classified as WARN")
        void slightlyAboveWarnThreshold_shouldBeWarn() {
            when(embeddingService.embed(anyString())).thenReturn(DUMMY_EMBEDDING);
            when(questionRepository.findTopSimilarQuestions(anyString(), eq(SUBJECT), eq(TENANT_ID), anyInt()))
                    .thenReturn(List.of(mockSimilarityResult(UUID.randomUUID(), 0.8501, "Boundary question")));

            SimilarityCheckResult result = similarityDetectionService.checkSimilarity(
                    "Test content", SUBJECT, TENANT_ID);

            assertThat(result.status()).isEqualTo(Status.WARN);
        }

        @Test
        @DisplayName("similarity at 0.9201 should be classified as REJECT")
        void slightlyAboveRejectThreshold_shouldBeReject() {
            when(embeddingService.embed(anyString())).thenReturn(DUMMY_EMBEDDING);
            when(questionRepository.findTopSimilarQuestions(anyString(), eq(SUBJECT), eq(TENANT_ID), anyInt()))
                    .thenReturn(List.of(mockSimilarityResult(UUID.randomUUID(), 0.9201, "Near-duplicate")));

            SimilarityCheckResult result = similarityDetectionService.checkSimilarity(
                    "Test content", SUBJECT, TENANT_ID);

            assertThat(result.status()).isEqualTo(Status.REJECT);
        }
    }
}
