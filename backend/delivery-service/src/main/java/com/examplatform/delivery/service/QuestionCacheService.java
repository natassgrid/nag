/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.delivery.service;

import com.examplatform.questionbank.grpc.PaperQuestionsGrpcRequest;
import com.examplatform.questionbank.grpc.PaperQuestionsGrpcResponse;
import com.examplatform.questionbank.grpc.QuestionBankGrpcServiceGrpc;
import com.examplatform.questionbank.grpc.QuestionSummaryGrpc;
import com.examplatform.shared.grpc.GrpcChannelFactory;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for fetching questions from the Question Bank via gRPC with circuit breaker protection.
 * Falls back to pre-cached Redis data when the Question Bank service is unavailable.
 *
 * Validates: design error-handling
 */
@Slf4j
@Service
public class QuestionCacheService {

    private static final String CACHE_KEY_PREFIX = "question:cache:";
    private static final long CACHE_TTL_HOURS = 24;

    private final RedisTemplate<String, Object> redisTemplate;
    private final String grpcHost;
    private final int grpcPort;
    private final long timeoutMs;

    public QuestionCacheService(
            RedisTemplate<String, Object> redisTemplate,
            @Value("${grpc.client.questionbank.host:localhost}") String grpcHost,
            @Value("${grpc.client.questionbank.port:9083}") int grpcPort,
            @Value("${grpc.client.questionbank.timeout-ms:5000}") long timeoutMs) {
        this.redisTemplate = redisTemplate;
        this.grpcHost = grpcHost;
        this.grpcPort = grpcPort;
        this.timeoutMs = timeoutMs;
    }

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
        log.debug("Fetching questions from Question Bank via gRPC: paperId={}, tenant={}", paperId, tenantId);

        List<Map<String, Object>> questions = fetchFromQuestionBank(paperId, tenantId);

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
     * Fetches questions from the Question Bank service via gRPC.
     */
    private List<Map<String, Object>> fetchFromQuestionBank(UUID paperId, String tenantId) {
        try {
            ManagedChannel channel = GrpcChannelFactory.getChannel(grpcHost, grpcPort);
            QuestionBankGrpcServiceGrpc.QuestionBankGrpcServiceBlockingStub stub =
                    QuestionBankGrpcServiceGrpc.newBlockingStub(channel)
                            .withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS);

            PaperQuestionsGrpcRequest request = PaperQuestionsGrpcRequest.newBuilder()
                    .setPaperId(paperId != null ? paperId.toString() : "")
                    .setTenantId(tenantId != null ? tenantId : "")
                    .build();

            PaperQuestionsGrpcResponse response = stub.getQuestionsForPaper(request);
            List<Map<String, Object>> result = new ArrayList<>();

            for (QuestionSummaryGrpc q : response.getQuestionsList()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", q.getId());
                map.put("content", q.getContent());
                map.put("type", q.getQuestionType());
                map.put("difficulty", q.getDifficulty());
                map.put("marks", q.getMarks());
                map.put("negativeMarks", q.getNegativeMarks());
                map.put("optionsJson", q.getOptionsJson());
                map.put("answerKey", q.getAnswerKey());
                result.add(map);
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch questions via gRPC from {}:{} ({}). Triggering circuit breaker fallback.",
                    grpcHost, grpcPort, e.getMessage());
            throw new RuntimeException("gRPC call to question-bank-service failed: " + e.getMessage(), e);
        }
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
