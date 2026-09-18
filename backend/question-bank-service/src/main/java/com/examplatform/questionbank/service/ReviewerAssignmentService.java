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

package com.examplatform.questionbank.service;

import com.examplatform.questionbank.client.ReviewerPoolClient;
import com.examplatform.questionbank.dto.ReviewerAssignment;
import com.examplatform.questionbank.dto.ReviewerDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service responsible for resolving, load-balancing, and caching reviewer assignments.
 *
 * Features:
 * - Querying actual reviewer pool with Subject Matter Expert / Reviewer roles
 * - 10-minute Redis caching for reviewer pool lookups
 * - Author Conflict of Interest exclusion
 * - Least-loaded and round-robin assignment strategy
 * - Dual-review support (2 distinct reviewers)
 * - Fallback escalation to Exam Controller / general reviewers
 *
 * Validates: Requirements 5.1, 5.2, 5.3
 */
@Slf4j
@Service
public class ReviewerAssignmentService {

    private static final Duration POOL_CACHE_TTL = Duration.ofMinutes(10);
    private static final String REDIS_KEY_POOL_PREFIX = "reviewer:pool:";
    private static final String REDIS_KEY_LOAD_PREFIX = "reviewer:load:";

    private final ReviewerPoolClient reviewerPoolClient;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;

    private final Map<String, LocalCacheEntry> localPoolCache = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> localLoadTracker = new ConcurrentHashMap<>();
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

    @Autowired
    public ReviewerAssignmentService(
            ReviewerPoolClient reviewerPoolClient,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            ObjectMapper objectMapper) {
        this.reviewerPoolClient = reviewerPoolClient;
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * Resolves and assigns eligible reviewer(s) for a question.
     *
     * @param subject            question subject domain
     * @param authorId           author user ID (excluded for conflict of interest)
     * @param tenantId           tenant identifier
     * @param dualReviewRequired whether two distinct reviewers must be assigned
     * @return populated ReviewerAssignment
     */
    public ReviewerAssignment assignReviewers(String subject, UUID authorId, String tenantId, boolean dualReviewRequired) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        List<ReviewerDto> pool = getCachedReviewerPool(subject, effectiveTenant);

        if (pool.isEmpty()) {
            log.warn("No active reviewers found for subject='{}', tenant='{}'", subject, effectiveTenant);
            return ReviewerAssignment.builder()
                    .assignedReviewerIds(Collections.emptyList())
                    .build();
        }

        // 1. Filter out author (Conflict of Interest check)
        List<ReviewerDto> eligible = pool.stream()
                .filter(r -> r.getId() != null && !r.getId().equals(authorId))
                .toList();

        if (eligible.isEmpty()) {
            log.warn("All available reviewers for subject='{}' in tenant='{}' were excluded due to conflict of interest (author={})",
                    subject, effectiveTenant, authorId);
            return ReviewerAssignment.builder()
                    .assignedReviewerIds(Collections.emptyList())
                    .build();
        }

        // 2. Separate specialist vs general/controller candidates
        List<ReviewerDto> specialists = eligible.stream()
                .filter(r -> r.getSpecialization() != null && matchesSubject(r.getSpecialization(), subject))
                .toList();

        boolean escalated = false;
        List<ReviewerDto> candidates;
        if (!specialists.isEmpty()) {
            candidates = new ArrayList<>(specialists);
        } else {
            // Fallback escalation to general reviewers / controllers
            candidates = new ArrayList<>(eligible);
            escalated = true;
            log.info("No subject specialist found for '{}', escalating to general reviewer / controller pool (size={})",
                    subject, candidates.size());
        }

        // 3. Score and sort candidates by current load (least-loaded) and round-robin index
        int rrSeed = Math.abs(roundRobinCounter.getAndIncrement());
        List<ScoredReviewer> scored = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            ReviewerDto dto = candidates.get(i);
            int load = getReviewerLoad(dto.getId(), effectiveTenant);
            int tieBreaker = (i + rrSeed) % candidates.size();
            scored.add(new ScoredReviewer(dto, load, tieBreaker));
        }

        scored.sort(Comparator.comparingInt(ScoredReviewer::load)
                .thenComparingInt(ScoredReviewer::tieBreaker));

        ReviewerDto primary = scored.get(0).reviewer();
        incrementReviewerLoad(primary.getId(), effectiveTenant);

        UUID secondaryId = null;
        List<UUID> assignedIds = new ArrayList<>();
        assignedIds.add(primary.getId());

        if (dualReviewRequired && scored.size() > 1) {
            ReviewerDto secondary = scored.get(1).reviewer();
            secondaryId = secondary.getId();
            assignedIds.add(secondaryId);
            incrementReviewerLoad(secondaryId, effectiveTenant);
            log.info("Dual review assigned: primary={}, secondary={} for subject='{}'",
                    primary.getId(), secondaryId, subject);
        } else {
            log.info("Single review assigned: primary={} for subject='{}'", primary.getId(), subject);
        }

        return ReviewerAssignment.builder()
                .primaryReviewerId(primary.getId())
                .secondaryReviewerId(secondaryId)
                .assignedReviewerIds(assignedIds)
                .escalatedToController(escalated)
                .build();
    }

    /**
     * Decrements the in-flight review queue depth when a review completes.
     *
     * @param reviewerId reviewer user ID
     * @param tenantId   tenant context
     */
    public void releaseReviewerLoad(UUID reviewerId, String tenantId) {
        if (reviewerId == null) return;
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        String key = REDIS_KEY_LOAD_PREFIX + effectiveTenant + ":" + reviewerId;

        StringRedisTemplate redis = getRedisTemplate();
        if (redis != null) {
            try {
                Long current = redis.opsForValue().decrement(key);
                if (current != null && current < 0) {
                    redis.opsForValue().set(key, "0");
                }
            } catch (Exception e) {
                log.warn("Failed to decrement Redis reviewer load for {}: {}", reviewerId, e.getMessage());
            }
        }

        AtomicInteger counter = localLoadTracker.get(key);
        if (counter != null) {
            counter.updateAndGet(c -> Math.max(0, c - 1));
        }
    }

    /**
     * Retrieves reviewer pool for (subject, tenant) with Redis caching (10-min TTL).
     */
    public List<ReviewerDto> getCachedReviewerPool(String subject, String tenantId) {
        String cleanSubject = (subject != null) ? subject.trim().toLowerCase() : "all";
        String redisKey = REDIS_KEY_POOL_PREFIX + tenantId + ":" + cleanSubject;

        // 1. Try Redis cache
        StringRedisTemplate redis = getRedisTemplate();
        if (redis != null) {
            try {
                String cachedJson = redis.opsForValue().get(redisKey);
                if (cachedJson != null && !cachedJson.isBlank()) {
                    return objectMapper.readValue(cachedJson, new TypeReference<List<ReviewerDto>>() {});
                }
            } catch (Exception e) {
                log.warn("Redis reviewer pool cache read failed: {}", e.getMessage());
            }
        }

        // 2. Try Local in-memory cache
        LocalCacheEntry local = localPoolCache.get(redisKey);
        if (local != null && !local.isExpired()) {
            return local.pool();
        }

        // 3. Fetch from remote / DB client
        List<ReviewerDto> fetched = reviewerPoolClient.getReviewers(subject, tenantId);
        if (fetched == null) {
            fetched = Collections.emptyList();
        }

        // 4. Update caches
        if (!fetched.isEmpty()) {
            if (redis != null) {
                try {
                    String json = objectMapper.writeValueAsString(fetched);
                    redis.opsForValue().set(redisKey, json, POOL_CACHE_TTL);
                } catch (Exception e) {
                    log.warn("Redis reviewer pool cache write failed: {}", e.getMessage());
                }
            }
            localPoolCache.put(redisKey, new LocalCacheEntry(fetched, Instant.now().plus(POOL_CACHE_TTL)));
        }

        return fetched;
    }

    /**
     * Clears cached reviewer pool for (subject, tenant).
     */
    public void evictCache(String subject, String tenantId) {
        String cleanSubject = (subject != null) ? subject.trim().toLowerCase() : "all";
        String redisKey = REDIS_KEY_POOL_PREFIX + tenantId + ":" + cleanSubject;

        StringRedisTemplate redis = getRedisTemplate();
        if (redis != null) {
            try {
                redis.delete(redisKey);
            } catch (Exception ignored) {}
        }
        localPoolCache.remove(redisKey);
    }

    public int getReviewerLoad(UUID reviewerId, String tenantId) {
        if (reviewerId == null) return 0;
        String key = REDIS_KEY_LOAD_PREFIX + tenantId + ":" + reviewerId;

        StringRedisTemplate redis = getRedisTemplate();
        if (redis != null) {
            try {
                String val = redis.opsForValue().get(key);
                if (val != null) {
                    return Integer.parseInt(val.trim());
                }
            } catch (Exception e) {
                log.warn("Failed to get reviewer load from Redis for {}: {}", reviewerId, e.getMessage());
            }
        }

        AtomicInteger counter = localLoadTracker.get(key);
        return counter != null ? counter.get() : 0;
    }

    private void incrementReviewerLoad(UUID reviewerId, String tenantId) {
        if (reviewerId == null) return;
        String key = REDIS_KEY_LOAD_PREFIX + tenantId + ":" + reviewerId;

        StringRedisTemplate redis = getRedisTemplate();
        if (redis != null) {
            try {
                redis.opsForValue().increment(key);
            } catch (Exception e) {
                log.warn("Failed to increment reviewer load in Redis for {}: {}", reviewerId, e.getMessage());
            }
        }

        localLoadTracker.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
    }

    private boolean matchesSubject(String specialization, String subject) {
        if (specialization == null || subject == null) return false;
        String s1 = specialization.trim().toLowerCase();
        String s2 = subject.trim().toLowerCase();
        return s1.equals(s2) || s1.contains(s2) || s2.contains(s1);
    }

    private StringRedisTemplate getRedisTemplate() {
        return redisTemplateProvider != null ? redisTemplateProvider.getIfAvailable() : null;
    }

    private record ScoredReviewer(ReviewerDto reviewer, int load, int tieBreaker) {}

    private record LocalCacheEntry(List<ReviewerDto> pool, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
