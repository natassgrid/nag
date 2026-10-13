// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.service;

import com.examplatform.questionbank.microlesson.dto.MicroLessonResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MicroLessonCacheService {

    private static final Logger log = LoggerFactory.getLogger(MicroLessonCacheService.class);
    private static final String KEY_PREFIX = "microlesson:";
    private static final Duration DEFAULT_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<MicroLessonResponse> getCached(Long subtopicId) {
        if (subtopicId == null || redisTemplate == null) return Optional.empty();
        try {
            String key = KEY_PREFIX + subtopicId;
            String json = redisTemplate.opsForValue().get(key);
            if (json != null && !json.isBlank()) {
                MicroLessonResponse resp = objectMapper.readValue(json, MicroLessonResponse.class);
                return Optional.of(resp);
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed for subtopic {}: {}", subtopicId, e.getMessage());
        }
        return Optional.empty();
    }

    public void put(Long subtopicId, MicroLessonResponse response) {
        if (subtopicId == null || response == null || redisTemplate == null) return;
        try {
            String key = KEY_PREFIX + subtopicId;
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, DEFAULT_TTL);
        } catch (Exception e) {
            log.warn("Redis cache write failed for subtopic {}: {}", subtopicId, e.getMessage());
        }
    }

    public void evict(Long subtopicId) {
        if (subtopicId == null || redisTemplate == null) return;
        try {
            String key = KEY_PREFIX + subtopicId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis cache eviction failed for subtopic {}: {}", subtopicId, e.getMessage());
        }
    }
}
