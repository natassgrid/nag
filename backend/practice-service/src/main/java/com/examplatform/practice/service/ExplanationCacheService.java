// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.service;

import com.examplatform.practice.dto.explanation.ExplanationIrDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis L2 Cache Service for Anvaya-Prajna Explanations (Issue #348).
 * Provides 24-hour TTL caching for computed multi-step explanation representations.
 */
@Slf4j
@Service
public class ExplanationCacheService {

    private static final String CACHE_PREFIX = "explain:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ExplanationCacheService(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
    }

    public Optional<ExplanationIrDto> getCachedExplanation(String questionId) {
        if (redisTemplate == null || questionId == null) {
            return Optional.empty();
        }
        try {
            String key = CACHE_PREFIX + questionId;
            String json = redisTemplate.opsForValue().get(key);
            if (json != null && !json.isBlank()) {
                log.debug("Cache hit for question explanation: {}", questionId);
                return Optional.of(objectMapper.readValue(json, ExplanationIrDto.class));
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve cached explanation for question {}: {}", questionId, e.getMessage());
        }
        return Optional.empty();
    }

    public void cacheExplanation(String questionId, ExplanationIrDto explanation) {
        if (redisTemplate == null || questionId == null || explanation == null) {
            return;
        }
        try {
            String key = CACHE_PREFIX + questionId;
            String json = objectMapper.writeValueAsString(explanation);
            redisTemplate.opsForValue().set(key, json, TTL);
            log.debug("Cached explanation for question: {} with TTL 24h", questionId);
        } catch (Exception e) {
            log.warn("Failed to cache explanation for question {}: {}", questionId, e.getMessage());
        }
    }
}
