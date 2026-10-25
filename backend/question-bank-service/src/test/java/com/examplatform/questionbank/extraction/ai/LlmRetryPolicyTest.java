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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LlmRetryPolicy Unit Tests")
class LlmRetryPolicyTest {

    private LlmRetryPolicy retryPolicy;
    private List<Long> sleepDurations;

    @BeforeEach
    void setUp() {
        retryPolicy = new LlmRetryPolicy(4, 50, 500);
        sleepDurations = new ArrayList<>();
        // Inject non-blocking test sleeper
        retryPolicy.setSleeper(sleepDurations::add);
    }

    @Test
    @DisplayName("429 rate limit on attempt 1 then success on attempt 2 retries and succeeds")
    void testRateLimitThenSuccess() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);

        String result = retryPolicy.executeWithRetry(3, () -> {
            int attempt = callCount.incrementAndGet();
            if (attempt == 1) {
                throw new RuntimeException("429 Too Many Requests: Rate limit exceeded");
            }
            return "SUCCESS_RESPONSE";
        });

        assertThat(result).isEqualTo("SUCCESS_RESPONSE");
        assertThat(callCount.get()).isEqualTo(2);
        assertThat(sleepDurations).hasSize(1);
        assertThat(sleepDurations.getFirst()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("429 rate limit exhausted across all 4 attempts throws LlmQuotaExhaustedException")
    void testRateLimitExhaustedThrowsQuotaExhaustedException() {
        AtomicInteger callCount = new AtomicInteger(0);

        assertThatThrownBy(() ->
                retryPolicy.executeWithRetry(7, () -> {
                    callCount.incrementAndGet();
                    throw HttpClientErrorException.create(
                            HttpStatusCode.valueOf(429),
                            "Too Many Requests",
                            HttpHeaders.EMPTY,
                            new byte[0],
                            null
                    );
                })
        )
                .isInstanceOf(LlmQuotaExhaustedException.class)
                .satisfies(ex -> {
                    LlmQuotaExhaustedException quotaEx = (LlmQuotaExhaustedException) ex;
                    assertThat(quotaEx.getPageNumber()).isEqualTo(7);
                    assertThat(quotaEx.getMessage()).contains("7");
                });

        assertThat(callCount.get()).isEqualTo(4);
        assertThat(sleepDurations).hasSize(3);
    }

    @Test
    @DisplayName("Non-quota error fails fast on attempt 1 without retrying or sleeping")
    void testNonQuotaErrorFailsFast() {
        AtomicInteger callCount = new AtomicInteger(0);

        assertThatThrownBy(() ->
                retryPolicy.executeWithRetry(5, () -> {
                    callCount.incrementAndGet();
                    throw new RuntimeException("500 Internal Server Error: Gateway bad response");
                })
        )
                .isInstanceOf(RuntimeException.class)
                .isNotInstanceOf(LlmQuotaExhaustedException.class)
                .hasMessageContaining("500 Internal Server Error");

        assertThat(callCount.get()).isEqualTo(1);
        assertThat(sleepDurations).isEmpty();
    }

    @Test
    @DisplayName("Detects quota and rate limit keywords across nested exception causes")
    void testDetectsQuotaKeywordsInCauses() {
        Throwable cause = new IllegalStateException("API error: RESOURCE_EXHAUSTED - daily quota exceeded");
        Throwable wrapped = new RuntimeException("Outer wrapper", cause);

        assertThat(retryPolicy.isQuotaOrRateLimitException(wrapped)).isTrue();
    }
}
