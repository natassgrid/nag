/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 Open Digital Public Infrastructure (DPI) Platform Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */
package com.examplatform.questionbank.ai.embedding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.util.EmbeddingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring AI implementation of {@link EmbeddingService} that delegates to an
 * {@link EmbeddingModel} configured to call the all-minilm model (384-dim)
 * via the LiteLLM OpenAI-compatible gateway.
 *
 * <p>This service produces 384-dimensional float vectors suitable for storage
 * in pgvector's {@code halfvec(384)} column and cosine similarity search.</p>
 *
 * @see EmbeddingService
 * @see com.examplatform.questionbank.ai.config.SpringAiConfig
 */
@Slf4j
@Service
public class SpringAiEmbeddingService implements EmbeddingService {

    private static final int BATCH_SIZE = 50;

    private final EmbeddingModel embeddingModel;
    private final QuestionRepository questionRepository;

    public SpringAiEmbeddingService(EmbeddingModel embeddingModel) {
        this(embeddingModel, null);
    }

    @Autowired
    public SpringAiEmbeddingService(EmbeddingModel embeddingModel, @Autowired(required = false) QuestionRepository questionRepository) {
        this.embeddingModel = embeddingModel;
        this.questionRepository = questionRepository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates a 384-dimensional embedding for the given text using the
     * all-minilm model via LiteLLM. Target latency is under 200ms per call.</p>
     */
    @Override
    public float[] embed(String text) {
        log.debug("Generating embedding for text of length {}", text.length());
        long start = System.nanoTime();

        float[] embedding = embeddingModel.embed(text);

        long durationMs = (System.nanoTime() - start) / 1_000_000;
        log.debug("Embedding generated in {}ms, dimensions: {}", durationMs, embedding.length);

        return embedding;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates embeddings for a batch of texts in a single call to the
     * embedding model, reducing HTTP round-trips for bulk operations such as
     * the backfill endpoint.</p>
     */
    @Override
    public List<float[]> embedBatch(List<String> texts) {
        log.debug("Generating embeddings for batch of {} texts", texts.size());
        long start = System.nanoTime();

        List<float[]> embeddings = embeddingModel.embed(texts);

        long durationMs = (System.nanoTime() - start) / 1_000_000;
        log.debug("Batch embedding completed in {}ms for {} texts", durationMs, texts.size());

        return embeddings;
    }

    @Override
    @Transactional
    public Map<String, Object> backfillEmbeddings(String tenantId) {
        log.info("Starting embedding backfill for tenant={}", tenantId);

        if (questionRepository == null) {
            log.warn("QuestionRepository not available for embedding backfill");
            return Map.of("totalProcessed", 0, "totalFailed", 0, "failures", List.of());
        }

        int totalProcessed = 0;
        int totalFailed = 0;
        List<String> failures = new ArrayList<>();

        Page<Question> batch;
        do {
            batch = questionRepository.findQuestionsWithNullEmbedding(
                    tenantId, PageRequest.of(0, BATCH_SIZE));

            if (batch.isEmpty()) {
                break;
            }

            try {
                List<String> contents = batch.getContent().stream()
                        .map(Question::getContent)
                        .map(content -> content != null ? content : "")
                        .toList();

                List<float[]> embeddings = embedBatch(contents);

                List<Question> questions = batch.getContent();
                for (int i = 0; i < questions.size(); i++) {
                    questionRepository.updateEmbedding(
                            questions.get(i).getId(), EmbeddingUtils.embeddingToString(embeddings.get(i)));
                }

                totalProcessed += questions.size();

                log.info("Backfill batch completed: processed={}, remaining={}",
                        questions.size(), batch.getTotalElements() - questions.size());

            } catch (Exception e) {
                totalFailed += batch.getContent().size();
                String failureMessage = String.format(
                        "Batch failed (%d questions): %s", batch.getContent().size(), e.getMessage());
                failures.add(failureMessage);
                log.error("Embedding backfill batch failed for tenant={}: {}", tenantId, e.getMessage(), e);
                break;
            }

        } while (batch.hasNext() || !batch.isEmpty());

        log.info("Embedding backfill finished for tenant={}: processed={}, failed={}",
                tenantId, totalProcessed, totalFailed);

        return Map.of(
                "totalProcessed", totalProcessed,
                "totalFailed", totalFailed,
                "failures", failures
        );
    }
}
