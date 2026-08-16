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
package com.examplatform.questionbank.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI configuration for the Question Bank service.
 *
 * <p>This configuration creates a {@link ChatClient} bean from the auto-configured
 * {@link ChatClient.Builder}. The OpenAI-compatible starter auto-configures both
 * the {@code OpenAiChatModel} and {@code OpenAiEmbeddingModel} beans based on
 * application properties pointing to the LiteLLM gateway.</p>
 *
 * <p>The LLM provider is fully swappable by changing only configuration properties
 * (base-url, api-key, model names) — no code changes required (FR-7).</p>
 *
 * <h3>Auto-configured beans (via spring-ai-starter-model-openai):</h3>
 * <ul>
 *   <li>{@code OpenAiChatModel} — chat completions via LiteLLM → Ollama models</li>
 *   <li>{@code OpenAiEmbeddingModel} — embeddings via LiteLLM → all-minilm</li>
 *   <li>{@code ChatClient.Builder} — prototype-scoped builder for ChatClient instances</li>
 * </ul>
 *
 * @see org.springframework.ai.openai.OpenAiChatModel
 * @see org.springframework.ai.openai.OpenAiEmbeddingModel
 */
@Configuration
public class SpringAiConfig {

    /**
     * Creates a default {@link ChatClient} bean from the auto-configured builder.
     *
     * <p>The builder is pre-configured by Spring AI auto-configuration with the
     * LiteLLM base URL, API key, default model, and temperature from properties.</p>
     *
     * @param builder the auto-configured ChatClient.Builder (prototype-scoped)
     * @return a ready-to-use ChatClient instance
     */
    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
