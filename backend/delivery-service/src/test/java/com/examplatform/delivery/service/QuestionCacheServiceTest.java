package com.examplatform.delivery.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for QuestionCacheService — circuit breaker fallback behavior.
 *
 * Validates: design error-handling
 */
@ExtendWith(MockitoExtension.class)
class QuestionCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private QuestionCacheService questionCacheService;

    private UUID paperId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        questionCacheService = new QuestionCacheService(redisTemplate);
        paperId = UUID.randomUUID();
        tenantId = "tenant-board-1";
    }

    @Test
    @DisplayName("Fallback returns cached data when Question Bank service is down")
    void fallbackReturnsCachedDataWhenServiceDown() {
        String cacheKey = "question:cache:" + tenantId + ":" + paperId;

        List<Map<String, Object>> cachedQuestions = List.of(
                Map.of("id", UUID.randomUUID().toString(), "content", "What is 2+2?", "type", "MCQ"),
                Map.of("id", UUID.randomUUID().toString(), "content", "Solve for x: 3x+1=7", "type", "NUMERICAL")
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(eq(cacheKey))).thenReturn(cachedQuestions);

        RuntimeException serviceDown = new RuntimeException("Connection refused: Question Bank service unavailable");

        List<Map<String, Object>> result = questionCacheService.getFromCache(paperId, tenantId, serviceDown);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("content")).isEqualTo("What is 2+2?");
        assertThat(result.get(1).get("type")).isEqualTo("NUMERICAL");
    }

    @Test
    @DisplayName("Fallback returns empty list on cache miss")
    void fallbackReturnsEmptyListOnCacheMiss() {
        String cacheKey = "question:cache:" + tenantId + ":" + paperId;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(eq(cacheKey))).thenReturn(null);

        RuntimeException serviceDown = new RuntimeException("Connection timeout");

        List<Map<String, Object>> result = questionCacheService.getFromCache(paperId, tenantId, serviceDown);

        assertThat(result).isEmpty();
    }
}
