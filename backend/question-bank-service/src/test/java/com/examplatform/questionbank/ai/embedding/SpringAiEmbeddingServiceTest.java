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

package com.examplatform.questionbank.ai.embedding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SpringAiEmbeddingService}.
 * Validates that the service correctly delegates to Spring AI's EmbeddingModel.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiEmbeddingService")
class SpringAiEmbeddingServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @InjectMocks
    private SpringAiEmbeddingService springAiEmbeddingService;

    @Test
    @DisplayName("embed() returns 384-dimensional float array from model")
    void embed_returnsCorrectDimensionVector() {
        float[] expected = new float[384];
        for (int i = 0; i < 384; i++) {
            expected[i] = i * 0.01f;
        }
        when(embeddingModel.embed("What is Java?")).thenReturn(expected);

        float[] result = springAiEmbeddingService.embed("What is Java?");

        assertThat(result).hasSize(384);
        assertThat(result).isEqualTo(expected);
        verify(embeddingModel).embed("What is Java?");
    }

    @Test
    @DisplayName("embed() delegates to EmbeddingModel with exact input text")
    void embed_delegatesToModel() {
        String text = "Solve: $$x^2 - 5x + 6 = 0$$";
        float[] expected = new float[384];
        when(embeddingModel.embed(text)).thenReturn(expected);

        float[] result = springAiEmbeddingService.embed(text);

        assertThat(result).isSameAs(expected);
        verify(embeddingModel).embed(text);
    }

    @Test
    @DisplayName("embed() handles empty text input")
    void embed_emptyText_delegatesToModel() {
        float[] expected = new float[384];
        when(embeddingModel.embed("")).thenReturn(expected);

        float[] result = springAiEmbeddingService.embed("");

        assertThat(result).hasSize(384);
        verify(embeddingModel).embed("");
    }

    @Test
    @DisplayName("embedBatch() returns correct number of embeddings")
    void embedBatch_returnsCorrectCount() {
        List<String> texts = List.of("Question 1", "Question 2", "Question 3");
        List<float[]> expected = List.of(
                new float[384],
                new float[384],
                new float[384]
        );
        when(embeddingModel.embed(texts)).thenReturn(expected);

        List<float[]> result = springAiEmbeddingService.embedBatch(texts);

        assertThat(result).hasSize(3);
        assertThat(result).isEqualTo(expected);
        verify(embeddingModel).embed(texts);
    }

    @Test
    @DisplayName("embedBatch() handles single text in batch")
    void embedBatch_singleText_returnsSingleEmbedding() {
        List<String> texts = List.of("Single question");
        List<float[]> expected = List.of(new float[384]);
        when(embeddingModel.embed(texts)).thenReturn(expected);

        List<float[]> result = springAiEmbeddingService.embedBatch(texts);

        assertThat(result).hasSize(1);
        verify(embeddingModel).embed(texts);
    }

    @Test
    @DisplayName("embedBatch() handles empty list input")
    void embedBatch_emptyList_returnsEmptyList() {
        List<String> texts = List.of();
        List<float[]> expected = List.of();
        when(embeddingModel.embed(texts)).thenReturn(expected);

        List<float[]> result = springAiEmbeddingService.embedBatch(texts);

        assertThat(result).isEmpty();
        verify(embeddingModel).embed(texts);
    }

    @Test
    @DisplayName("embedBatch() delegates to EmbeddingModel with exact input list")
    void embedBatch_delegatesToModel() {
        List<String> texts = List.of(
                "What is $$\\pi$$?",
                "Define photosynthesis",
                "Name the capital of India"
        );
        List<float[]> expected = List.of(
                new float[384],
                new float[384],
                new float[384]
        );
        when(embeddingModel.embed(texts)).thenReturn(expected);

        List<float[]> result = springAiEmbeddingService.embedBatch(texts);

        assertThat(result).isSameAs(expected);
        verify(embeddingModel).embed(texts);
    }
}
