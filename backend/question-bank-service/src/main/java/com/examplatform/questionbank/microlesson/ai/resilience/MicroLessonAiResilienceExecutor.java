/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

package com.examplatform.questionbank.microlesson.ai.resilience;

import com.examplatform.questionbank.exception.AiSecurityException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Resilient execution wrapper for upstream AI LLM inference calls using Resilience4j
 * Circuit Breaker, Retry, and timeout guards (OWASP Top 10 for AI: LLM04, LLM06).
 */
@Component
public class MicroLessonAiResilienceExecutor {

    private static final Logger log = LoggerFactory.getLogger(MicroLessonAiResilienceExecutor.class);
    public static final String SERVICE_NAME = "microLessonAiService";

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    @Value("${app.ai.resilience.call-timeout-seconds:${AI_CALL_TIMEOUT_SECONDS:45}}")
    private int callTimeoutSeconds = 45;

    public MicroLessonAiResilienceExecutor() {
        this(null, null);
    }

    @Autowired
    public MicroLessonAiResilienceExecutor(
            @Autowired(required = false) CircuitBreakerRegistry circuitBreakerRegistry,
            @Autowired(required = false) RetryRegistry retryRegistry) {
        CircuitBreakerRegistry cbRegistry = circuitBreakerRegistry != null
                ? circuitBreakerRegistry
                : CircuitBreakerRegistry.ofDefaults();
        RetryRegistry rRegistry = retryRegistry != null
                ? retryRegistry
                : RetryRegistry.ofDefaults();

        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(4)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofMillis(500))
                .permittedNumberOfCallsInHalfOpenState(2)
                .ignoreExceptions(AiSecurityException.class)
                .build();

        RetryConfig defaultRetryConfig = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(50))
                .ignoreExceptions(AiSecurityException.class, CallNotPermittedException.class)
                .build();

        this.circuitBreaker = circuitBreakerRegistry != null
                ? circuitBreakerRegistry.circuitBreaker(SERVICE_NAME)
                : cbRegistry.circuitBreaker(SERVICE_NAME, () -> defaultConfig);
        this.retry = retryRegistry != null
                ? retryRegistry.retry(SERVICE_NAME)
                : rRegistry.retry(SERVICE_NAME, () -> defaultRetryConfig);
    }

    public MicroLessonAiResilienceExecutor(
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            int callTimeoutSeconds) {
        this(circuitBreakerRegistry, retryRegistry);
        this.callTimeoutSeconds = callTimeoutSeconds;
    }

    public MicroLessonAiResilienceExecutor(CircuitBreaker circuitBreaker, Retry retry, int callTimeoutSeconds) {
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.callTimeoutSeconds = callTimeoutSeconds;
    }

    /**
     * Executes an AI LLM supplier call through Retry and Circuit Breaker with timeout enforcement.
     *
     * @param llmSupplier the supplier performing the remote AI generation call
     * @return raw response string
     * @throws Exception if downstream failure, timeout, or circuit is open
     */
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = SERVICE_NAME, fallbackMethod = "handleAiFallback")
    public String executeChatCall(Callable<String> llmSupplier) throws Exception {
        return retry.executeCallable(() -> circuitBreaker.executeCallable(() -> {
            try {
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        return llmSupplier.call();
                    } catch (RuntimeException re) {
                        throw re;
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }).get(callTimeoutSeconds, TimeUnit.SECONDS);
            } catch (ExecutionException ee) {
                Throwable cause = ee.getCause();
                if (cause instanceof Exception ex) {
                    throw ex;
                }
                throw new RuntimeException(cause);
            } catch (TimeoutException te) {
                throw new TimeoutException("AI call timed out after " + callTimeoutSeconds + " seconds");
            }
        }));
    }

    /**
     * Fallback handler invoked when circuit breaker trips or downstream failure propagates.
     */
    public String handleAiFallback(Callable<String> llmSupplier, Throwable t) throws Exception {
        log.warn("Circuit breaker fallback triggered for AI call: {}", t.getMessage());
        if (t instanceof Exception ex) {
            throw ex;
        }
        throw new RuntimeException(t);
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public CircuitBreaker.State getCircuitBreakerState() {
        return circuitBreaker.getState();
    }

    public Retry getRetry() {
        return retry;
    }

    public int getCallTimeoutSeconds() {
        return callTimeoutSeconds;
    }

    public void setCallTimeoutSeconds(int callTimeoutSeconds) {
        this.callTimeoutSeconds = callTimeoutSeconds;
    }
}
