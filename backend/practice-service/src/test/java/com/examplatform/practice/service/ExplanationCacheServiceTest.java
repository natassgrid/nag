// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.service;

import com.examplatform.practice.dto.explanation.ExplanationIrDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExplanationCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ExplanationCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new ExplanationCacheService(redisTemplate, objectMapper);
    }

    @Test
    @DisplayName("getCachedExplanation - Returns empty when redisTemplate is null")
    void testGetCachedExplanationNullRedis() {
        ExplanationCacheService noRedis = new ExplanationCacheService(null, objectMapper);
        Optional<ExplanationIrDto> result = noRedis.getCachedExplanation("q-123");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getCachedExplanation - Returns empty when key not found in Redis")
    void testGetCachedExplanationMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("explain:q-123")).thenReturn(null);

        Optional<ExplanationIrDto> result = cacheService.getCachedExplanation("q-123");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getCachedExplanation - Returns cached explanation on Redis hit")
    void testGetCachedExplanationHit() throws Exception {
        ExplanationIrDto explanation = ExplanationIrDto.builder()
                .explanationId("exp-99")
                .questionId("q-123")
                .status("APPROVED")
                .build();
        String json = objectMapper.writeValueAsString(explanation);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("explain:q-123")).thenReturn(json);

        Optional<ExplanationIrDto> result = cacheService.getCachedExplanation("q-123");
        assertThat(result).isPresent();
        assertThat(result.get().getExplanationId()).isEqualTo("exp-99");
        assertThat(result.get().getQuestionId()).isEqualTo("q-123");
    }

    @Test
    @DisplayName("cacheExplanation - Sets value in Redis with 24h TTL")
    void testCacheExplanationSuccess() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ExplanationIrDto explanation = ExplanationIrDto.builder()
                .explanationId("exp-99")
                .questionId("q-123")
                .build();

        cacheService.cacheExplanation("q-123", explanation);

        verify(valueOperations).set(eq("explain:q-123"), any(String.class), eq(Duration.ofHours(24)));
    }

    @Test
    @DisplayName("cacheExplanation - Handles Redis failure gracefully without throwing")
    void testCacheExplanationHandlesRedisError() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis connection refused"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        ExplanationIrDto explanation = ExplanationIrDto.builder().explanationId("exp-1").build();

        // Should not throw
        cacheService.cacheExplanation("q-123", explanation);
    }
}
