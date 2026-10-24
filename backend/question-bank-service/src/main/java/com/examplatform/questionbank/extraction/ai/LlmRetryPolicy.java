/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 Open Digital Public Infrastructure (DPI) Platform Contributors
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
package com.examplatform.questionbank.extraction.ai;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Executes LLM operations with exponential backoff and full jitter when rate limits
 * or quota limits (HTTP 429, RESOURCE_EXHAUSTED) are encountered.
 * Non-quota errors fail fast without exhausting retry quota.
 * When quota retries are exhausted, throws {@link LlmQuotaExhaustedException}.
 */
@Slf4j
@Component
public class LlmRetryPolicy {

    @FunctionalInterface
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    @Getter
    @Setter
    private int maxAttempts;

    @Getter
    @Setter
    private long initialBackoffMs;

    @Getter
    @Setter
    private long maxBackoffMs;

    @Getter
    @Setter
    private Sleeper sleeper = Thread::sleep;

    public LlmRetryPolicy(
            @Value("${app.extraction.retry.max-attempts:4}") int maxAttempts,
            @Value("${app.extraction.retry.initial-backoff-ms:2000}") long initialBackoffMs,
            @Value("${app.extraction.retry.max-backoff-ms:30000}") long maxBackoffMs) {
        this.maxAttempts = maxAttempts > 0 ? maxAttempts : 4;
        this.initialBackoffMs = initialBackoffMs > 0 ? initialBackoffMs : 2000;
        this.maxBackoffMs = maxBackoffMs > 0 ? maxBackoffMs : 30000;
    }

    /**
     * Executes the given callable with exponential backoff + full jitter on quota/rate-limit errors.
     *
     * @param pageNumber page number being processed (for exception context)
     * @param action callable task
     * @param <T> result type
     * @return result from action
     * @throws LlmQuotaExhaustedException if quota/rate-limit errors persist through all attempts
     * @throws Exception if non-quota error occurs or thread is interrupted
     */
    public <T> T executeWithRetry(int pageNumber, Callable<T> action) throws Exception {
        int attempt = 0;
        Throwable lastQuotaCause = null;

        while (attempt < maxAttempts) {
            attempt++;
            try {
                return action.call();
            } catch (Exception ex) {
                if (!isQuotaOrRateLimitException(ex)) {
                    // Non-quota error: do not retry, propagate immediately to allow fallback
                    log.debug("Non-quota error on page {} attempt {}: {}", pageNumber, attempt, ex.getMessage());
                    throw ex;
                }

                lastQuotaCause = ex;
                if (attempt >= maxAttempts) {
                    log.error("Exhausted all {} retry attempts for page {} due to quota exhaustion",
                            maxAttempts, pageNumber);
                    break;
                }

                long backoffCap = Math.min(
                        maxBackoffMs,
                        (long) (initialBackoffMs * Math.pow(2, attempt - 1))
                );
                long sleepTimeMs = backoffCap > 0
                        ? ThreadLocalRandom.current().nextLong(1, backoffCap + 1)
                        : initialBackoffMs;

                log.warn("Rate limit / quota error on page {} (attempt {}/{}): {}. Backing off for {} ms with full jitter",
                        pageNumber, attempt, maxAttempts, ex.getMessage(), sleepTimeMs);

                try {
                    sleeper.sleep(sleepTimeMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during backoff sleep on page " + pageNumber, ie);
                }
            }
        }

        throw new LlmQuotaExhaustedException(
                pageNumber,
                "LLM rate limit or quota exceeded after " + maxAttempts + " attempts on page " + pageNumber,
                lastQuotaCause
        );
    }

    /**
     * Inspects the exception and its cause chain for HTTP 429, rate-limiting, or quota exhaustion indicators.
     *
     * @param throwable candidate exception
     * @return true if exception indicates rate-limiting or quota exhaustion
     */
    public boolean isQuotaOrRateLimitException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String className = current.getClass().getName();
            String message = current.getMessage() != null ? current.getMessage().toLowerCase() : "";

            if (className.contains("TooManyRequests")
                    || className.contains("NonTransientAiException")
                    || className.contains("TransientAiException")) {
                if (message.contains("429")
                        || message.contains("rate limit")
                        || message.contains("quota")
                        || message.contains("resource_exhausted")
                        || message.contains("too many requests")
                        || className.contains("TooManyRequests")) {
                    return true;
                }
            }

            // Check HTTP status code methods reflectively or via standard names if present
            if (message.contains("429")
                    || message.contains("rate limit")
                    || message.contains("quota")
                    || message.contains("resource_exhausted")
                    || message.contains("too many requests")) {
                return true;
            }

            // Check Spring WebClientResponseException / HttpClientErrorException status
            if (current instanceof org.springframework.web.client.HttpStatusCodeException hse
                    && hse.getStatusCode().value() == 429) {
                return true;
            }

            current = current.getCause();
        }
        return false;
    }
}
