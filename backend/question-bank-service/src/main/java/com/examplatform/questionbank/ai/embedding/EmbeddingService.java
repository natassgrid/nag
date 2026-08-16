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

/**
 * Service interface for generating text embeddings.
 *
 * <p>Implementations use an embedding model (e.g., all-minilm via LiteLLM)
 * to produce 384-dimensional vectors suitable for similarity search
 * with pgvector's halfvec type.</p>
 *
 * @see com.examplatform.questionbank.ai.config.SpringAiConfig
 */
public interface EmbeddingService {

    /**
     * Generates a 384-dimensional embedding for a single text input.
     *
     * @param text the text to embed (question content, typically)
     * @return a 384-element float array representing the embedding vector
     */
    float[] embed(String text);

    /**
     * Generates embeddings for a batch of text inputs.
     *
     * @param texts the list of texts to embed
     * @return a list of 384-element float arrays, one per input text
     */
    List<float[]> embedBatch(List<String> texts);
}
