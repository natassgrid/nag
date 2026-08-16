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

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

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
@RequiredArgsConstructor
public class SpringAiEmbeddingService implements EmbeddingService {

    private final EmbeddingModel embeddingModel;

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
}
