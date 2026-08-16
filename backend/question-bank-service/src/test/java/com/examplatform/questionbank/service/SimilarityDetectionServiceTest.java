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
import com.examplatform.questionbank.exception.SimilarQuestionException;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.repository.SimilarityResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SimilarityDetectionService.
 * Validates: Requirements FR-2 (Duplicate Detection)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SimilarityDetectionService")
class SimilarityDetectionServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private SimilarityDetectionService similarityDetectionService;

    private static final String SUBJECT = "Mathematics";
    private static final String TENANT_ID = "tenant-abc";
    private static final float[] DUMMY_EMBEDDING = new float[]{0.1f, 0.2f, 0.3f};

    @Nested
    @DisplayName("checkSimilarity(content, subject, tenantId)")
    class CheckSimilarityFullTests {

        @Test
        @DisplayName("returns PASS when no similar questions found")
        void returnsPass_whenNoSimilarQuestions() {
            when(embeddingService.embed(anyString())).thenReturn(DUMMY_EMBEDDING);
            when(questionRepository.findTopSimilarQuestions(anyString(), eq(SUBJECT), eq(TENANT_ID), anyInt()))
                    .thenReturn(Collections.emptyList());

            SimilarityCheckResult result = similarityDetectionService.checkSimilarity(
                    "What is calculus?", SUBJECT, TENANT_ID);

            assertThat(result.status()).isEqualTo(Status.PASS);
            assertThat(result.similarQuestions()).isEmpty();
        }

        @Test
        @DisplayName("returns PASS when all similarities are below 0.85")
        void returnsPass_whenAllBelowWarnThreshold() {
            when(embeddingService.embed(anyString())).thenReturn(DUMMY_EMBEDDING);
            when(questionRepository.findTopSimilarQuestions(anyString(), eq(SUBJECT), eq(TENANT_ID), anyInt()))
                    .thenReturn(List.of(mockSimilarityResult(UUID.randomUUID(), 0.70, "Some question")));

            SimilarityCheckResult result = similarityDetectionService.checkSimilarity(
                    "What is calculus?", SUBJECT, TENANT_ID);

            assertThat(result.status()).isEqualTo(Status.PASS);
            assertThat(result.similarQuestions()).isEmpty();
        }

        @Test
        @DisplayName("returns WARN when similarity is between 0.85 and 0.92")
        void returnsWarn_whenSimilarityInWarnRange() {
            UUID similarId = UUID.randomUUID();
            when(embeddingService.embed(anyString())).thenReturn(DUMMY_EMBEDDING);
            when(questionRepository.findTopSimilarQuestions(anyString(), eq(SUBJECT), eq(TENANT_ID), anyInt()))
                    .thenReturn(List.of(mockSimilarityResult(similarId, 0.88, "Similar question")));

            SimilarityCheckResult result = similarityDetectionService.checkSimilarity(
                    "What is differentiation?", SUBJECT, TENANT_ID);

            assertThat(result.status()).isEqualTo(Status.WARN);
            assertThat(result.similarQuestions()).hasSize(1);
            assertThat(result.similarQuestions().getFirst().questionId()).isEqualTo(similarId);
            assertThat(result.similarQuestions().getFirst().similarity()).isEqualTo(0.88);
        }

        @Test
        @DisplayName("returns REJECT when similarity exceeds 0.92")
        void returnsReject_whenSimilarityAboveRejectThreshold() {
            UUID similarId = UUID.randomUUID();
            when(embeddingService.embed(anyString())).thenReturn(DUMMY_EMBEDDING);
            when(questionRepository.findTopSimilarQuestions(anyString(), eq(SUBJECT), eq(TENANT_ID), anyInt()))
                    .thenReturn(List.of(mockSimilarityResult(similarId, 0.95, "Near-duplicate")));

            SimilarityCheckResult result = similarityDetectionService.checkSimilarity(
                    "What is integration?", SUBJECT, TENANT_ID);

            assertThat(result.status()).isEqualTo(Status.REJECT);
            assertThat(result.similarQuestions()).hasSize(1);
            assertThat(result.similarQuestions().getFirst().questionId()).isEqualTo(similarId);
            assertThat(result.similarQuestions().getFirst().similarity()).isEqualTo(0.95);
        }

        @Test
        @DisplayName("REJECT takes precedence over WARN when both are present")
        void rejectTakesPrecedenceOverWarn() {
            UUID warnId = UUID.randomUUID();
            UUID rejectId = UUID.randomUUID();
            when(embeddingService.embed(anyString())).thenReturn(DUMMY_EMBEDDING);
            when(questionRepository.findTopSimilarQuestions(anyString(), eq(SUBJECT), eq(TENANT_ID), anyInt()))
                    .thenReturn(List.of(
                            mockSimilarityResult(rejectId, 0.95, "Near-duplicate"),
                            mockSimilarityResult(warnId, 0.87, "Similar question")
                    ));

            SimilarityCheckResult result = similarityDetectionService.checkSimilarity(
                    "Test content", SUBJECT, TENANT_ID);

            assertThat(result.status()).isEqualTo(Status.REJECT);
            assertThat(result.similarQuestions()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("enforceNoDuplicate")
    class EnforceNoDuplicateTests {

        @Test
        @DisplayName("throws SimilarQuestionException when REJECT threshold exceeded")
        void throwsException_whenRejectThresholdExceeded() {
            UUID similarId = UUID.randomUUID();
            when(embeddingService.embed(anyString())).thenReturn(DUMMY_EMBEDDING);
            when(questionRepository.findTopSimilarQuestions(anyString(), eq(SUBJECT), eq(TENANT_ID), anyInt()))
                    .thenReturn(List.of(mockSimilarityResult(similarId, 0.95, "Duplicate")));

            assertThatThrownBy(() -> similarityDetectionService.enforceNoDuplicate(
                    "Duplicate content", SUBJECT, TENANT_ID))
                    .isInstanceOf(SimilarQuestionException.class)
                    .hasMessageContaining(similarId.toString());
        }

        @Test
        @DisplayName("returns WARN result without throwing when in warn range")
        void returnsWarn_withoutThrowing() {
            UUID warnId = UUID.randomUUID();
            when(embeddingService.embed(anyString())).thenReturn(DUMMY_EMBEDDING);
            when(questionRepository.findTopSimilarQuestions(anyString(), eq(SUBJECT), eq(TENANT_ID), anyInt()))
                    .thenReturn(List.of(mockSimilarityResult(warnId, 0.88, "Similar")));

            SimilarityCheckResult result = similarityDetectionService.enforceNoDuplicate(
                    "Some question", SUBJECT, TENANT_ID);

            assertThat(result.status()).isEqualTo(Status.WARN);
            assertThat(result.similarQuestions()).hasSize(1);
        }

        @Test
        @DisplayName("returns PASS result without throwing when no similar questions")
        void returnsPass_whenNoSimilarQuestions() {
            when(embeddingService.embed(anyString())).thenReturn(DUMMY_EMBEDDING);
            when(questionRepository.findTopSimilarQuestions(anyString(), eq(SUBJECT), eq(TENANT_ID), anyInt()))
                    .thenReturn(Collections.emptyList());

            SimilarityCheckResult result = similarityDetectionService.enforceNoDuplicate(
                    "Unique question", SUBJECT, TENANT_ID);

            assertThat(result.status()).isEqualTo(Status.PASS);
            assertThat(result.similarQuestions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("computeCosineSimilarity")
    class CosineSimilarityTests {

        @Test
        @DisplayName("identical vectors return 1.0")
        void identicalVectors_returnsOne() {
            float[] vector = {1.0f, 2.0f, 3.0f};
            double similarity = similarityDetectionService.computeCosineSimilarity(vector, vector);
            assertThat(similarity).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("orthogonal vectors return 0.0")
        void orthogonalVectors_returnsZero() {
            float[] a = {1.0f, 0.0f, 0.0f};
            float[] b = {0.0f, 1.0f, 0.0f};
            double similarity = similarityDetectionService.computeCosineSimilarity(a, b);
            assertThat(similarity).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("opposite vectors return -1.0")
        void oppositeVectors_returnsNegativeOne() {
            float[] a = {1.0f, 2.0f, 3.0f};
            float[] b = {-1.0f, -2.0f, -3.0f};
            double similarity = similarityDetectionService.computeCosineSimilarity(a, b);
            assertThat(similarity).isCloseTo(-1.0, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("zero vector returns 0.0")
        void zeroVector_returnsZero() {
            float[] a = {0.0f, 0.0f, 0.0f};
            float[] b = {1.0f, 2.0f, 3.0f};
            double similarity = similarityDetectionService.computeCosineSimilarity(a, b);
            assertThat(similarity).isEqualTo(0.0);
        }

        @Test
        @DisplayName("mismatched dimensions throw IllegalArgumentException")
        void mismatchedDimensions_throwsException() {
            float[] a = {1.0f, 2.0f};
            float[] b = {1.0f, 2.0f, 3.0f};
            assertThatThrownBy(() -> similarityDetectionService.computeCosineSimilarity(a, b))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Vectors must have the same dimension");
        }
    }

    @Nested
    @DisplayName("embeddingToString")
    class EmbeddingToStringTests {

        @Test
        @DisplayName("converts float array to pgvector format")
        void convertsToCorrectFormat() {
            float[] embedding = {0.1f, 0.2f, 0.3f};
            String result = similarityDetectionService.embeddingToString(embedding);
            assertThat(result).startsWith("[").endsWith("]");
            assertThat(result).contains("0.1").contains("0.2").contains("0.3");
        }
    }

    /**
     * Creates a mock SimilarityResult projection for testing.
     */
    private SimilarityResult mockSimilarityResult(UUID id, double similarity, String content) {
        return new SimilarityResult() {
            @Override
            public UUID getId() {
                return id;
            }

            @Override
            public String getSubject() {
                return SUBJECT;
            }

            @Override
            public String getContent() {
                return content;
            }

            @Override
            public Double getSimilarity() {
                return similarity;
            }
        };
    }
}
