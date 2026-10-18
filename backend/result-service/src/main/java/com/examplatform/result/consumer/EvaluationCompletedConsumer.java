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

package com.examplatform.result.consumer;

import com.examplatform.result.domain.Result;
import com.examplatform.result.dto.CandidateScoreInput;
import com.examplatform.result.dto.SectionBreakdownDto;
import com.examplatform.result.dto.TaxonomyBreakdownDto;
import com.examplatform.result.repository.ResultRepository;
import com.examplatform.result.service.QuestionAnalyticsService;
import com.examplatform.result.service.ResultComputationService;
import com.examplatform.shared.constants.MessagingConstants;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.messaging.AbstractEvaluationCompletedConsumer;
import com.examplatform.shared.util.DataConversionUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Computes and persists diagnostic scorecards and invalidates question analytics cache.
 */
@Slf4j
@Component
public class EvaluationCompletedConsumer extends AbstractEvaluationCompletedConsumer {

    private final ResultComputationService resultComputationService;
    private final ResultRepository resultRepository;
    private final QuestionAnalyticsService questionAnalyticsService;

    public EvaluationCompletedConsumer(
            ResultComputationService resultComputationService,
            ResultRepository resultRepository,
            QuestionAnalyticsService questionAnalyticsService,
            ObjectMapper objectMapper) {
        super(objectMapper);
        this.resultComputationService = resultComputationService;
        this.resultRepository = resultRepository;
        this.questionAnalyticsService = questionAnalyticsService;
    }

    @KafkaListener(
            topics = EVALUATION_COMPLETED_TOPIC,
            groupId = "result-service-evaluation-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onEvaluationCompleted(
            @Payload String payload,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        log.info("Computing result scorecard for evaluation completed: key={}", key);
        processEvaluationCompleted(payload, key);
    }

    @Override
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "result.evaluation.events.queue", durable = "true"),
                    exchange = @Exchange(value = MessagingConstants.EXAM_EVENTS_TOPIC, type = ExchangeTypes.TOPIC),
                    key = EVALUATION_COMPLETED_TOPIC
            )
    )
    public void onRabbitEvaluationCompleted(Object message) {
        super.onRabbitEvaluationCompleted(message);
    }

    @Override
    public void processEvaluationCompleted(String payload, String key) {
        handleEvaluationCompleted(payload, event -> {
            UUID candidateId = UUID.fromString((String) event.get("candidateId"));
            UUID examId      = DataConversionUtils.parseUUID(event.get("examId"));
            String tenantId  = (String) event.getOrDefault(SharedConstants.TENANT_ID, SharedConstants.DEFAULT_TENANT);
            double totalRawScore = DataConversionUtils.toDouble(event.get("totalRawScore"));

            // Idempotency: skip if result already exists
            if (examId != null) {
                Optional<Result> existing = resultRepository
                        .findByCandidateIdAndExamIdAndTenantId(candidateId, examId, tenantId);
                if (existing.isPresent()) {
                    log.info("Result already exists for candidate={}, exam={} — skipping", candidateId, examId);
                    return;
                }
            }

            // Extract section scores
            @SuppressWarnings("unchecked")
            Map<String, Object> rawSectionScores = (Map<String, Object>) event.getOrDefault("sectionScores", Map.of());
            Map<String, Double> sectionScores = new HashMap<>();
            rawSectionScores.forEach((k, v) -> sectionScores.put(k, DataConversionUtils.toDouble(v)));

            List<SectionBreakdownDto> sectionBreakdowns = parseSectionBreakdowns(event.get("sectionBreakdowns"));
            List<TaxonomyBreakdownDto> topicBreakdowns = parseTaxonomyBreakdowns(event.get("topicBreakdowns"));
            List<TaxonomyBreakdownDto> subtopicBreakdowns = parseTaxonomyBreakdowns(event.get("subtopicBreakdowns"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> questionLevelScores =
                    (List<Map<String, Object>>) event.getOrDefault("questionLevelScores", List.of());

            // Build score input
            CandidateScoreInput scoreInput = CandidateScoreInput.builder()
                    .candidateId(candidateId)
                    .totalRawScore(totalRawScore)
                    .sectionScores(sectionScores)
                    .sectionBreakdowns(sectionBreakdowns)
                    .topicBreakdowns(topicBreakdowns)
                    .subtopicBreakdowns(subtopicBreakdowns)
                    .questionLevelScores(questionLevelScores)
                    .shiftMean(0)
                    .shiftStdDev(0)
                    .build();

            // Compute and save result (single-candidate batch)
            if (examId != null) {
                List<Result> results = resultComputationService.computeResults(
                        examId, List.of(scoreInput), false, tenantId);

                // Enrich with diagnostic data from event
                enrichWithDiagnostics(results, event, tenantId);

                // Invalidate analytics cache for this exam
                questionAnalyticsService.invalidateCache(examId);

                log.info("Result computed for candidate={}, exam={}, score={}",
                        candidateId, examId, totalRawScore);
            }
        });
    }

    /**
     * Enriches saved results with diagnostic data from the evaluation event.
     * Computes accuracy rate and time analysis from question-level scores if not already set.
     */
    private void enrichWithDiagnostics(List<Result> results, Map<String, Object> event, String tenantId) {
        if (results.isEmpty()) return;

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> questionLevelScores =
                    (List<Map<String, Object>>) event.getOrDefault("questionLevelScores", List.of());

            BigDecimal accuracyRate = null;
            if (!questionLevelScores.isEmpty()) {
                long totalAttempted = questionLevelScores.size();
                long correct = questionLevelScores.stream()
                        .filter(q -> DataConversionUtils.toDouble(q.get("score")) > 0)
                        .count();
                accuracyRate = BigDecimal.valueOf(correct * 100.0 / totalAttempted)
                        .setScale(2, RoundingMode.HALF_UP);
            }

            boolean updated = false;
            for (Result r : results) {
                if (accuracyRate != null && r.getAccuracyRate() == null) {
                    r.setAccuracyRate(accuracyRate);
                    updated = true;
                }
                r.setTenantId(tenantId);
            }
            if (updated) {
                resultRepository.saveAll(results);
            }

        } catch (Exception e) {
            log.warn("Failed to enrich results with diagnostic data: {}", e.getMessage());
        }
    }

    private List<SectionBreakdownDto> parseSectionBreakdowns(Object raw) {
        if (raw == null) return null;
        try {
            if (raw instanceof List<?> list) {
                return objectMapper.convertValue(list, new TypeReference<List<SectionBreakdownDto>>() {});
            } else if (raw instanceof String str && !str.isBlank()) {
                return objectMapper.readValue(str, new TypeReference<List<SectionBreakdownDto>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to parse section breakdowns from event: {}", e.getMessage());
        }
        return null;
    }

    private List<TaxonomyBreakdownDto> parseTaxonomyBreakdowns(Object raw) {
        if (raw == null) return null;
        try {
            if (raw instanceof List<?> list) {
                return objectMapper.convertValue(list, new TypeReference<List<TaxonomyBreakdownDto>>() {});
            } else if (raw instanceof String str && !str.isBlank()) {
                return objectMapper.readValue(str, new TypeReference<List<TaxonomyBreakdownDto>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to parse taxonomy breakdowns from event: {}", e.getMessage());
        }
        return null;
    }
}
