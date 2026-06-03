package com.examplatform.questionbank.service;

import com.examplatform.questionbank.exception.SimilarQuestionException;
import com.examplatform.questionbank.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SimilarityDetectionService.
 * Validates: Requirements 4.7
 */
@ExtendWith(MockitoExtension.class)
class SimilarityDetectionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private SimilarityDetectionService similarityDetectionService;

    @Test
    @DisplayName("Similarity above threshold rejects with SimilarQuestionException")
    void checkSimilarity_aboveThreshold_throwsException() {
        UUID similarId = UUID.randomUUID();
        when(questionRepository.findSimilarPublishedQuestion(anyString(), anyDouble()))
                .thenReturn(Optional.of(similarId));

        assertThatThrownBy(() -> similarityDetectionService.checkSimilarity("What is Java?"))
                .isInstanceOf(SimilarQuestionException.class)
                .hasMessageContaining(similarId.toString());
    }

    @Test
    @DisplayName("Similarity below threshold passes without exception")
    void checkSimilarity_belowThreshold_passes() {
        when(questionRepository.findSimilarPublishedQuestion(anyString(), anyDouble()))
                .thenReturn(Optional.empty());

        // Should not throw
        similarityDetectionService.checkSimilarity("What is the capital of France?");
    }

    @Test
    @DisplayName("Cosine similarity of identical vectors equals 1.0")
    void computeCosineSimilarity_identicalVectors_returnsOne() {
        float[] vector = {1.0f, 2.0f, 3.0f};
        double similarity = similarityDetectionService.computeCosineSimilarity(vector, vector);
        assertThat(similarity).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    @DisplayName("Cosine similarity of orthogonal vectors equals 0.0")
    void computeCosineSimilarity_orthogonalVectors_returnsZero() {
        float[] a = {1.0f, 0.0f, 0.0f};
        float[] b = {0.0f, 1.0f, 0.0f};
        double similarity = similarityDetectionService.computeCosineSimilarity(a, b);
        assertThat(similarity).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    @DisplayName("Cosine similarity of opposite vectors equals -1.0")
    void computeCosineSimilarity_oppositeVectors_returnsNegativeOne() {
        float[] a = {1.0f, 2.0f, 3.0f};
        float[] b = {-1.0f, -2.0f, -3.0f};
        double similarity = similarityDetectionService.computeCosineSimilarity(a, b);
        assertThat(similarity).isCloseTo(-1.0, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    @DisplayName("Cosine similarity with zero vectors returns 0.0")
    void computeCosineSimilarity_zeroVector_returnsZero() {
        float[] a = {0.0f, 0.0f, 0.0f};
        float[] b = {1.0f, 2.0f, 3.0f};
        double similarity = similarityDetectionService.computeCosineSimilarity(a, b);
        assertThat(similarity).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Cosine similarity throws on mismatched dimensions")
    void computeCosineSimilarity_mismatchedDimensions_throwsException() {
        float[] a = {1.0f, 2.0f};
        float[] b = {1.0f, 2.0f, 3.0f};
        assertThatThrownBy(() -> similarityDetectionService.computeCosineSimilarity(a, b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Vectors must have the same dimension");
    }

    @Test
    @DisplayName("computeEmbedding returns vector of correct dimension")
    void computeEmbedding_returnsCorrectDimension() {
        float[] embedding = similarityDetectionService.computeEmbedding("test content");
        assertThat(embedding).hasSize(1536);
    }

    @Test
    @DisplayName("computeEmbedding is deterministic for same input")
    void computeEmbedding_sameInput_sameOutput() {
        float[] first = similarityDetectionService.computeEmbedding("same content");
        float[] second = similarityDetectionService.computeEmbedding("same content");
        assertThat(first).isEqualTo(second);
    }
}
