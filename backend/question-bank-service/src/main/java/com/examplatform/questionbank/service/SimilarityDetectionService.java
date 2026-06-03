package com.examplatform.questionbank.service;

import com.examplatform.questionbank.exception.SimilarQuestionException;
import com.examplatform.questionbank.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Detects similarity between new questions and existing PUBLISHED questions
 * using cosine similarity on embedding vectors stored in pgvector.
 *
 * Validates: Requirement 4.7
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarityDetectionService {

    private static final double SIMILARITY_THRESHOLD = 0.80;
    private static final int EMBEDDING_DIMENSION = 1536;

    private final QuestionRepository questionRepository;

    /**
     * Checks if the given question content is too similar to any PUBLISHED question.
     * Throws SimilarQuestionException (HTTP 422) if similarity exceeds the threshold.
     *
     * @param content the question content text to check
     */
    public void checkSimilarity(String content) {
        float[] embedding = computeEmbedding(content);
        String embeddingStr = embeddingToString(embedding);

        Optional<UUID> similarId = questionRepository.findSimilarPublishedQuestion(
                embeddingStr, SIMILARITY_THRESHOLD);

        if (similarId.isPresent()) {
            log.warn("Similar question detected: newContent hash={}, similarId={}",
                    content.hashCode(), similarId.get());
            throw new SimilarQuestionException(similarId.get());
        }
    }

    /**
     * Computes cosine similarity between two embedding vectors.
     *
     * @param a first embedding vector
     * @param b second embedding vector
     * @return cosine similarity value between -1.0 and 1.0
     */
    public double computeCosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0.0) {
            return 0.0;
        }

        return dotProduct / denominator;
    }

    /**
     * Computes a dummy embedding vector for the given content.
     * TODO: Replace with real embedding API call (e.g., OpenAI text-embedding-ada-002)
     *
     * @param content the text content to embed
     * @return a 1536-dimensional float vector
     */
    float[] computeEmbedding(String content) {
        // TODO: Integrate with real embedding API (OpenAI, Cohere, etc.)
        // For now, generate a deterministic dummy vector based on content hash
        float[] embedding = new float[EMBEDDING_DIMENSION];
        Random random = new Random(content.hashCode());
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            embedding[i] = random.nextFloat() * 2 - 1; // range [-1, 1]
        }
        return embedding;
    }

    /**
     * Converts a float array embedding to a pgvector-compatible string format.
     * e.g., "[0.1,0.2,0.3,...]"
     */
    private String embeddingToString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
