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

package com.examplatform.delivery.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for fetching questions from the Question Bank with circuit breaker protection.
 * Falls back to pre-cached Redis data when the Question Bank service is unavailable.
 *
 * Validates: design error-handling
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionCacheService {

    private static final String CACHE_KEY_PREFIX = "question:cache:";
    private static final long CACHE_TTL_HOURS = 24;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Fetches questions for an exam paper from the Question Bank service.
     * Protected by the "questionBank" circuit breaker — if the service is down,
     * falls back to returning pre-cached questions from Redis.
     *
     * @param paperId  the exam paper identifier
     * @param tenantId the tenant identifier
     * @return list of question data maps
     */
    @CircuitBreaker(name = "questionBank", fallbackMethod = "getFromCache")
    public List<Map<String, Object>> getQuestionsForPaper(UUID paperId, String tenantId) {
        log.debug("Fetching questions from Question Bank: paperId={}, tenant={}", paperId, tenantId);

        // In production, this would call the Question Bank service via REST/gRPC
        List<Map<String, Object>> questions = fetchFromQuestionBank(paperId, tenantId);

        // Cache the fetched questions in Redis for fallback
        cacheQuestions(paperId, tenantId, questions);

        return questions;
    }

    /**
     * Fallback method — returns pre-cached questions from Redis when Question Bank is unavailable.
     * This is invoked automatically by the circuit breaker when the main method fails.
     *
     * @param paperId  the exam paper identifier
     * @param tenantId the tenant identifier
     * @param ex       the exception that triggered the fallback
     * @return cached question data or empty list if cache miss
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getFromCache(UUID paperId, String tenantId, Throwable ex) {
        log.warn("Question Bank unavailable, falling back to cache: paperId={}, tenant={}, error={}",
                paperId, tenantId, ex.getMessage());

        String cacheKey = buildCacheKey(paperId, tenantId);
        Object cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached instanceof List<?> cachedList) {
            log.info("Cache hit for paperId={}, returning {} cached questions", paperId, cachedList.size());
            return (List<Map<String, Object>>) cachedList;
        }

        log.error("Cache miss for paperId={} — no fallback data available", paperId);
        return Collections.emptyList();
    }

    /**
     * Fetches questions from the Question Bank service.
     * In production, this would use WebClient or RestClient for inter-service communication.
     */
    private List<Map<String, Object>> fetchFromQuestionBank(UUID paperId, String tenantId) {
        // Placeholder for actual inter-service call
        // Would be replaced with WebClient call to question-bank-service
        throw new UnsupportedOperationException(
                "Question Bank client not yet wired — replace with actual inter-service call");
    }

    /**
     * Caches fetched questions in Redis for circuit breaker fallback.
     */
    private void cacheQuestions(UUID paperId, String tenantId, List<Map<String, Object>> questions) {
        String cacheKey = buildCacheKey(paperId, tenantId);
        redisTemplate.opsForValue().set(cacheKey, questions, CACHE_TTL_HOURS, TimeUnit.HOURS);
        log.debug("Cached {} questions for paperId={}", questions.size(), paperId);
    }

    private String buildCacheKey(UUID paperId, String tenantId) {
        return CACHE_KEY_PREFIX + tenantId + ":" + paperId;
    }
}
