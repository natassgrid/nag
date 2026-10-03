/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.questionbank.service;

import com.examplatform.questionbank.ai.embedding.EmbeddingService;
import com.examplatform.questionbank.ai.similarity.SimilarityCheckResult;
import com.examplatform.questionbank.ai.similarity.SimilarityCheckResult.Status;
import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.exception.SimilarQuestionException;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.repository.SimilarityResult;
import com.examplatform.questionbank.support.AbstractIntegrationTest;
import com.examplatform.questionbank.util.EmbeddingUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * End-to-end integration test suite verifying pgvector 384-dimensional
 * cosine similarity search, partition routing, multi-scope querying,
 * threshold boundary enforcement, and multi-tenant isolation.
 *
 * Validates: Requirements FR-2, FR-3, FR-9, NFR-5 (Issue #129)
 */
@DisplayName("VectorSearchIntegrationTest — pgvector E2E Integration Suite")
@Transactional
class VectorSearchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private QuestionRepository questionRepository;

    @MockitoBean
    private EmbeddingService embeddingService;

    private SimilarityDetectionService similarityDetectionService;

    private static final String TENANT_A = "tenant-alpha";
    private static final String TENANT_B = "tenant-beta";
    private static final UUID AUTHOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        similarityDetectionService = new SimilarityDetectionService(embeddingService, questionRepository);
    }

    /**
     * Helper to create normalized unit vector in 384 dimensions.
     */
    private float[] createNormalizedVector(int primaryDim, double angleOffset) {
        float[] vec = new float[384];
        vec[primaryDim] = (float) Math.cos(angleOffset);
        vec[(primaryDim + 1) % 384] = (float) Math.sin(angleOffset);
        return vec;
    }

    /**
     * Helper to persist a question entity directly into the database.
     */
    private Question insertQuestion(
            UUID id,
            Long subjectId,
            Long topicId,
            String subject,
            String topic,
            String content,
            String state,
            String difficulty,
            String cognitiveLevel,
            String tenantId,
            float[] embedding) {

        Question question = Question.builder()
                .subjectId(subjectId)
                .topicId(topicId)
                .subject(subject)
                .topic(topic)
                .content(content)
                .difficulty(difficulty)
                .cognitiveLevel(cognitiveLevel)
                .questionType("SINGLE_MCQ")
                .state(state)
                .authorId(AUTHOR_ID)
                .usageCount(0)
                .build();
        ReflectionTestUtils.setField(question, "id", id);
        question.setTenantId(tenantId);

        Question saved = questionRepository.save(question);

        if (embedding != null) {
            String embeddingStr = EmbeddingUtils.embeddingToString(embedding);
            questionRepository.updateEmbedding(saved.getId(), embeddingStr);
        }

        return saved;
    }

    @Nested
    @DisplayName("1. Cosine Distance (<=>) & Direct Vector Insertion")
    class CosineDistanceCalculationTests {

        @Test
        @DisplayName("Identical vector has cosine similarity of 1.0 (distance 0.0)")
        void identicalVectorCosineSimilarity() {
            if (!testcontainersAvailable) return;

            UUID qId = UUID.randomUUID();
            float[] vec = createNormalizedVector(0, 0.0); // Unit vector along dim 0

            insertQuestion(qId, 1L, 10L, "Physics", "Mechanics",
                    "What is Newton's second law?", "PUBLISHED", "MEDIUM", "REMEMBER", TENANT_A, vec);

            String queryVecStr = EmbeddingUtils.embeddingToString(vec);
            List<SimilarityResult> results = questionRepository.findTopSimilarQuestions(
                    queryVecStr, "Physics", TENANT_A, 5);

            assertThat(results).isNotEmpty();
            assertThat(results.getFirst().getId()).isEqualTo(qId);
            assertThat(results.getFirst().getSimilarity()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        @DisplayName("Orthogonal vector has cosine similarity of 0.0")
        void orthogonalVectorCosineSimilarity() {
            if (!testcontainersAvailable) return;

            UUID qId = UUID.randomUUID();
            float[] vecA = createNormalizedVector(0, 0.0); // Dim 0
            float[] vecB = createNormalizedVector(10, 0.0); // Dim 10 (orthogonal)

            insertQuestion(qId, 1L, 10L, "Physics", "Mechanics",
                    "Question on dynamics", "PUBLISHED", "MEDIUM", "REMEMBER", TENANT_A, vecA);

            String queryVecStr = EmbeddingUtils.embeddingToString(vecB);
            List<SimilarityResult> results = questionRepository.findTopSimilarQuestions(
                    queryVecStr, "Physics", TENANT_A, 5);

            assertThat(results).isNotEmpty();
            assertThat(results.getFirst().getSimilarity()).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.001));
        }
    }

    @Nested
    @DisplayName("2. Threshold Boundary Enforcement (REJECT > 0.92, WARN 0.85–0.92, PASS < 0.85)")
    class ThresholdEnforcementTests {

        @Test
        @DisplayName("Near-duplicate vector (similarity 0.96 > 0.92) triggers REJECT and throws SimilarQuestionException")
        void nearDuplicateTriggersReject() {
            if (!testcontainersAvailable) return;

            UUID existingId = UUID.randomUUID();
            float[] baseVec = createNormalizedVector(0, 0.0);
            // Angle with cos(theta) approx 0.96 -> theta approx 0.284 rad
            float[] queryVec = createNormalizedVector(0, 0.284);

            insertQuestion(existingId, 2L, 20L, "Chemistry", "Atomic Structure",
                    "What is Bohr's atomic model?", "PUBLISHED", "EASY", "UNDERSTAND", TENANT_A, baseVec);

            when(embeddingService.embed(anyString())).thenReturn(queryVec);

            SimilarityCheckResult result = similarityDetectionService.checkSimilarity(
                    "Explain Bohr's model of hydrogen atom", "Chemistry", TENANT_A);

            assertThat(result.status()).isEqualTo(Status.REJECT);
            assertThat(result.similarQuestions()).isNotEmpty();
            assertThat(result.similarQuestions().getFirst().questionId()).isEqualTo(existingId);
            assertThat(result.similarQuestions().getFirst().similarity()).isGreaterThan(0.92);

            assertThatThrownBy(() -> similarityDetectionService.enforceNoDuplicate(
                    "Explain Bohr's model of hydrogen atom", "Chemistry", TENANT_A))
                    .isInstanceOf(SimilarQuestionException.class);
        }

        @Test
        @DisplayName("Moderate similarity vector (similarity 0.88 in [0.85, 0.92)) triggers WARN status")
        void moderateSimilarityTriggersWarn() {
            if (!testcontainersAvailable) return;

            UUID existingId = UUID.randomUUID();
            float[] baseVec = createNormalizedVector(0, 0.0);
            // Angle with cos(theta) approx 0.88 -> theta approx 0.495 rad
            float[] queryVec = createNormalizedVector(0, 0.495);

            insertQuestion(existingId, 2L, 20L, "Chemistry", "Atomic Structure",
                    "Discuss atomic orbitals and quantum numbers", "PUBLISHED", "MEDIUM", "UNDERSTAND", TENANT_A, baseVec);

            when(embeddingService.embed(anyString())).thenReturn(queryVec);

            SimilarityCheckResult result = similarityDetectionService.checkSimilarity(
                    "Overview of quantum numbers in atoms", "Chemistry", TENANT_A);

            assertThat(result.status()).isEqualTo(Status.WARN);
            assertThat(result.similarQuestions().getFirst().questionId()).isEqualTo(existingId);
            assertThat(result.similarQuestions().getFirst().similarity())
                    .isGreaterThanOrEqualTo(0.85)
                    .isLessThanOrEqualTo(0.92);
        }

        @Test
        @DisplayName("Dissimilar vector (similarity 0.70 < 0.85) returns PASS status")
        void dissimilarVectorPasses() {
            if (!testcontainersAvailable) return;

            UUID existingId = UUID.randomUUID();
            float[] baseVec = createNormalizedVector(0, 0.0);
            // Angle with cos(theta) approx 0.70 -> theta approx 0.795 rad
            float[] queryVec = createNormalizedVector(0, 0.795);

            insertQuestion(existingId, 2L, 20L, "Chemistry", "Atomic Structure",
                    "Chemical bonding principles", "PUBLISHED", "HARD", "ANALYZE", TENANT_A, baseVec);

            when(embeddingService.embed(anyString())).thenReturn(queryVec);

            SimilarityCheckResult result = similarityDetectionService.checkSimilarity(
                    "Organic synthesis mechanisms", "Chemistry", TENANT_A);

            assertThat(result.status()).isEqualTo(Status.PASS);
        }
    }

    @Nested
    @DisplayName("3. Multi-Tenant Segregation")
    class MultiTenantIsolationTests {

        @Test
        @DisplayName("Identical vector in Tenant B is never returned when querying Tenant A")
        void tenantIsolationEnforced() {
            if (!testcontainersAvailable) return;

            UUID qTenantB = UUID.randomUUID();
            float[] vec = createNormalizedVector(5, 0.0);

            // Persist question in Tenant B
            insertQuestion(qTenantB, 3L, 30L, "Mathematics", "Calculus",
                    "Find integral of sin(x)", "PUBLISHED", "MEDIUM", "APPLY", TENANT_B, vec);

            String queryVecStr = EmbeddingUtils.embeddingToString(vec);

            // Query in Tenant A -> should NOT find Tenant B's question
            List<SimilarityResult> resultsTenantA = questionRepository.findTopSimilarQuestions(
                    queryVecStr, "Mathematics", TENANT_A, 5);

            assertThat(resultsTenantA).isEmpty();

            // Query in Tenant B -> finds it with similarity 1.0
            List<SimilarityResult> resultsTenantB = questionRepository.findTopSimilarQuestions(
                    queryVecStr, "Mathematics", TENANT_B, 5);

            assertThat(resultsTenantB).hasSize(1);
            assertThat(resultsTenantB.getFirst().getId()).isEqualTo(qTenantB);
        }
    }

    @Nested
    @DisplayName("4. Multi-Scope & Filtered Vector Search")
    class MultiScopeVectorSearchTests {

        @Test
        @DisplayName("Topic-scoped vector search filters questions by topic accurately")
        void topicScopedVectorSearch() {
            if (!testcontainersAvailable) return;

            UUID qDiff = UUID.randomUUID();
            UUID qInteg = UUID.randomUUID();
            float[] vec = createNormalizedVector(1, 0.0);

            insertQuestion(qDiff, 3L, 31L, "Mathematics", "Differentiation",
                    "Derivative of cos(x)", "PUBLISHED", "EASY", "REMEMBER", TENANT_A, vec);
            insertQuestion(qInteg, 3L, 32L, "Mathematics", "Integration",
                    "Integral of cos(x)", "PUBLISHED", "EASY", "REMEMBER", TENANT_A, vec);

            String queryVecStr = EmbeddingUtils.embeddingToString(vec);

            List<SimilarityResult> topicResults = questionRepository.findTopSimilarQuestionsByTopic(
                    queryVecStr, "Mathematics", "Integration", TENANT_A, 5);

            assertThat(topicResults).hasSize(1);
            assertThat(topicResults.getFirst().getId()).isEqualTo(qInteg);
        }

        @Test
        @DisplayName("Global cross-subject vector search finds matching vectors across different subjects")
        void globalVectorSearch() {
            if (!testcontainersAvailable) return;

            UUID qPhysics = UUID.randomUUID();
            UUID qMath = UUID.randomUUID();
            float[] vec = createNormalizedVector(2, 0.0);

            insertQuestion(qPhysics, 1L, 10L, "Physics", "Vectors",
                    "Vector cross product", "PUBLISHED", "MEDIUM", "APPLY", TENANT_A, vec);
            insertQuestion(qMath, 3L, 33L, "Mathematics", "Linear Algebra",
                    "Determinant of 3x3 matrix", "PUBLISHED", "MEDIUM", "APPLY", TENANT_A, vec);

            String queryVecStr = EmbeddingUtils.embeddingToString(vec);

            List<SimilarityResult> globalResults = questionRepository.findTopSimilarQuestionsGlobal(
                    queryVecStr, TENANT_A, 5);

            assertThat(globalResults).hasSize(2);
        }

        @Test
        @DisplayName("Multi-attribute filtered vector search matches difficulty and cognitive level")
        void filteredVectorSearch() {
            if (!testcontainersAvailable) return;

            UUID qEasy = UUID.randomUUID();
            UUID qHard = UUID.randomUUID();
            float[] vec = createNormalizedVector(3, 0.0);

            insertQuestion(qEasy, 4L, 40L, "Economics", "Fiscal Policy",
                    "What is repo rate?", "APPROVED", "EASY", "REMEMBER", TENANT_A, vec);
            insertQuestion(qHard, 4L, 40L, "Economics", "Fiscal Policy",
                    "Derive IS-LM curve equilibrium under fiscal expansion", "APPROVED", "HARD", "ANALYZE", TENANT_A, vec);

            String queryVecStr = EmbeddingUtils.embeddingToString(vec);

            List<SimilarityResult> filtered = questionRepository.findTopSimilarQuestionsFiltered(
                    queryVecStr, "Economics", "Fiscal Policy", "HARD", "ANALYZE", TENANT_A, 5);

            assertThat(filtered).hasSize(1);
            assertThat(filtered.getFirst().getId()).isEqualTo(qHard);
        }
    }

    @Nested
    @DisplayName("5. findSimilarPublishedQuestion — Real Native Cosine Distance Query")
    class FindSimilarPublishedQuestionTests {

        @Test
        @DisplayName("findSimilarPublishedQuestion returns matching published question ID when similarity >= threshold")
        void findsPublishedQuestionAboveThreshold() {
            if (!testcontainersAvailable) return;

            UUID pubId = UUID.randomUUID();
            UUID draftId = UUID.randomUUID();
            float[] vec = createNormalizedVector(4, 0.0);

            // 1. PUBLISHED question with matching vector
            insertQuestion(pubId, 1L, 10L, "General Science", "Optics",
                    "Snell's law of refraction", "PUBLISHED", "MEDIUM", "APPLY", TENANT_A, vec);

            // 2. DRAFT question with matching vector (should NOT be returned)
            insertQuestion(draftId, 1L, 10L, "General Science", "Optics",
                    "Draft refraction question", "DRAFT", "MEDIUM", "APPLY", TENANT_A, vec);

            String queryVecStr = EmbeddingUtils.embeddingToString(vec);

            Optional<UUID> found = questionRepository.findSimilarPublishedQuestion(queryVecStr, 0.90);

            assertThat(found).isPresent();
            assertThat(found.get()).isEqualTo(pubId);
        }

        @Test
        @DisplayName("findSimilarPublishedQuestion returns empty when best similarity is below threshold")
        void returnsEmptyWhenBelowThreshold() {
            if (!testcontainersAvailable) return;

            UUID pubId = UUID.randomUUID();
            float[] vecA = createNormalizedVector(4, 0.0);
            float[] vecOrthogonal = createNormalizedVector(14, 0.0);

            insertQuestion(pubId, 1L, 10L, "General Science", "Optics",
                    "Snell's law of refraction", "PUBLISHED", "MEDIUM", "APPLY", TENANT_A, vecA);

            String queryVecStr = EmbeddingUtils.embeddingToString(vecOrthogonal);

            Optional<UUID> found = questionRepository.findSimilarPublishedQuestion(queryVecStr, 0.85);

            assertThat(found).isEmpty();
        }
    }
}
