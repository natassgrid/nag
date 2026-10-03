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

import com.examplatform.delivery.domain.ExamSession;
import com.examplatform.delivery.dto.QuestionDeliveryDto;
import com.examplatform.delivery.repository.ExamSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrator service for delivering examination questions to candidates during CBT test sessions.
 * Coordinates question resolution from decrypted exam packages, paper generator definitions, Redis cache,
 * and question repository, as well as multi-language translation enrichment and deterministic option randomization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamQuestionDeliveryService {

    private static final String REDIS_QUESTIONS_PREFIX = "delivery:questions:";
    private static final Duration CACHE_DURATION = Duration.ofHours(12);

    private final QuestionDeliveryRepository questionDeliveryRepository;
    private final QuestionDeliveryParser questionDeliveryParser;
    private final OptionRandomizer optionRandomizer;
    private final TranslationEnricher translationEnricher;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ExamSessionRepository examSessionRepository;

    /**
     * Resolves question delivery payloads for an examination session.
     *
     * @param examId         the examination UUID
     * @param paperId        the paper UUID (if assigned)
     * @param decryptedPaper decrypted paper JSON payload (if decrypted via Vault)
     * @param tenantId       the tenant identifier
     * @return list of questions formatted for delivery interface
     */
    public List<QuestionDeliveryDto> getDeliveryQuestions(UUID examId, UUID paperId, String decryptedPaper, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";

        // 1. First priority: parse questions embedded in the decrypted paper package
        if (decryptedPaper != null && !decryptedPaper.isBlank()) {
            List<QuestionDeliveryDto> parsedQuestions = questionDeliveryParser.parseFromPaperJson(decryptedPaper);
            if (!parsedQuestions.isEmpty()) {
                translationEnricher.enrichWithTranslations(parsedQuestions, effectiveTenant);
                return parsedQuestions;
            }
        }

        // 2. Second priority: if paperId is provided, check paper_generator.paper table directly
        if (paperId != null) {
            List<QuestionDeliveryDto> paperQuestions = questionDeliveryRepository.fetchQuestionsForPaper(paperId, effectiveTenant);
            if (!paperQuestions.isEmpty()) {
                translationEnricher.enrichWithTranslations(paperQuestions, effectiveTenant);
                return paperQuestions;
            }
        }

        // 3. Third priority: check Redis cache for this exam questions package
        String cacheKey = REDIS_QUESTIONS_PREFIX + effectiveTenant + ":" + (examId != null ? examId : "default");
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List<?> list && !list.isEmpty()) {
                log.debug("Cache hit for delivery questions examId={}", examId);
                return questionDeliveryParser.convertCachedList(list);
            }
        } catch (Exception e) {
            log.warn("Redis lookup failed for questions cache: {}", e.getMessage());
        }

        // 4. Fourth priority: query approved questions from the database matching the exam specification
        List<QuestionDeliveryDto> dbQuestions = questionDeliveryRepository.fetchQuestionsForExam(examId, effectiveTenant);
        if (!dbQuestions.isEmpty()) {
            translationEnricher.enrichWithTranslations(dbQuestions, effectiveTenant);
            try {
                redisTemplate.opsForValue().set(cacheKey, dbQuestions, CACHE_DURATION);
            } catch (Exception e) {
                log.warn("Failed to cache questions in Redis: {}", e.getMessage());
            }
            return dbQuestions;
        }

        return Collections.emptyList();
    }

    /**
     * Retrieves questions for an ongoing exam session.
     */
    public List<QuestionDeliveryDto> getQuestionsForSession(UUID sessionId, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        ExamSession session = examSessionRepository.findBySessionIdAndTenantId(sessionId, effectiveTenant)
                .orElse(null);

        if (session == null) {
            return Collections.emptyList();
        }

        List<QuestionDeliveryDto> baseQuestions = getDeliveryQuestions(session.getExamId(), session.getPaperId(), null, effectiveTenant);
        return optionRandomizer.randomizeOptions(baseQuestions, sessionId);
    }

    /**
     * Randomizes option order for each question deterministically using a session seed.
     */
    public List<QuestionDeliveryDto> randomizeOptions(List<QuestionDeliveryDto> questions, UUID seedId) {
        return optionRandomizer.randomizeOptions(questions, seedId);
    }

    /**
     * Enriches delivered questions with published and approved regional language translations.
     */
    public void enrichWithTranslations(List<QuestionDeliveryDto> questions, String tenantId) {
        translationEnricher.enrichWithTranslations(questions, tenantId);
    }

    /**
     * Retrieves questions for a specific paper.
     */
    public List<QuestionDeliveryDto> getQuestionsForPaper(UUID paperId, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        List<QuestionDeliveryDto> questions = questionDeliveryRepository.fetchQuestionsForPaper(paperId, effectiveTenant);
        if (!questions.isEmpty()) {
            translationEnricher.enrichWithTranslations(questions, effectiveTenant);
        }
        return questions;
    }

    public List<UUID> extractQuestionUuidsFromJson(String json) {
        return questionDeliveryParser.extractQuestionUuidsFromJson(json);
    }

    public List<UUID> extractQuestionUuidsFromJsonOrString(String raw) {
        return questionDeliveryParser.extractQuestionUuidsFromJsonOrString(raw);
    }
}
