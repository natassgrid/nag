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

package com.examplatform.questionbank.translation.service;

import com.examplatform.questionbank.translation.domain.Translation;
import com.examplatform.questionbank.translation.repository.TranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages translation review workflow: approve, reject, and mark stale.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TranslationReviewService {

    private static final String TRANSLATION_EVENTS_TOPIC = "exam.translation.events";

    private final TranslationRepository translationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Approve a translation, transitioning it from DRAFT -> APPROVED.
     *
     * @param translationId the translation to approve
     * @param reviewerId    the reviewer performing approval
     * @param tenantId      examination authority identifier
     * @return the approved Translation entity
     */
    public Translation approve(UUID translationId, UUID reviewerId, String tenantId) {
        Translation translation = translationRepository.findById(translationId)
                .orElseThrow(() -> new IllegalArgumentException("Translation not found: " + translationId));

        if (translation.getStatus() != Translation.TranslationStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT translations can be approved. Current status: " + translation.getStatus());
        }

        translation.setStatus(Translation.TranslationStatus.APPROVED);
        translation.setReviewerId(reviewerId);

        log.info("Translation {} approved by reviewer {}", translationId, reviewerId);
        return translationRepository.save(translation);
    }

    /**
     * Reject a translation and notify the translator via Kafka.
     *
     * @param translationId the translation to reject
     * @param reviewerId    the reviewer performing rejection
     * @param comments      rejection reason/feedback
     * @param tenantId      examination authority identifier
     */
    public void reject(UUID translationId, UUID reviewerId, String comments, String tenantId) {
        Translation translation = translationRepository.findById(translationId)
                .orElseThrow(() -> new IllegalArgumentException("Translation not found: " + translationId));

        if (translation.getStatus() != Translation.TranslationStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT translations can be rejected. Current status: " + translation.getStatus());
        }

        translation.setReviewerId(reviewerId);
        translation.setReviewComments(comments);
        // Keep in DRAFT for re-work by translator
        translationRepository.save(translation);

        // Notify translator via Kafka
        publishRejectionEvent(translation, reviewerId, comments, tenantId);
        log.info("Translation {} rejected by reviewer {}. Translator {} notified.",
                translationId, reviewerId, translation.getTranslatorId());
    }

    /**
     * Mark all APPROVED translations for a question as STALE.
     * Called when the source question is modified, invalidating existing translations.
     * Publishes a Kafka event for downstream consumers.
     *
     * @param questionId the source question that was modified
     * @param tenantId   the tenant identifier
     */
    public void markStale(UUID questionId, String tenantId) {
        List<Translation> approvedTranslations = translationRepository
                .findByQuestionIdAndStatusAndTenantId(questionId, Translation.TranslationStatus.APPROVED, tenantId);

        if (approvedTranslations.isEmpty()) {
            log.debug("No approved translations to mark stale for question {}", questionId);
            return;
        }

        for (Translation translation : approvedTranslations) {
            translation.setStatus(Translation.TranslationStatus.STALE);
        }
        translationRepository.saveAll(approvedTranslations);

        // Publish stale event
        publishStaleEvent(questionId, approvedTranslations.size());
        log.info("Marked {} translations as STALE for question {}", approvedTranslations.size(), questionId);
    }

    private void publishRejectionEvent(Translation translation, UUID reviewerId,
                                        String comments, String tenantId) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", "TRANSLATION_REJECTED",
                    "translationId", translation.getId().toString(),
                    "translatorId", translation.getTranslatorId().toString(),
                    "reviewerId", reviewerId.toString(),
                    "languageCode", translation.getLanguageCode(),
                    "comments", comments,
                    "tenantId", tenantId,
                    "occurredAt", Instant.now().toString()
            );
            kafkaTemplate.send(TRANSLATION_EVENTS_TOPIC, translation.getId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish TRANSLATION_REJECTED event: {}", e.getMessage());
        }
    }

    private void publishStaleEvent(UUID questionId, int count) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", "TRANSLATIONS_MARKED_STALE",
                    "questionId", questionId.toString(),
                    "affectedCount", count,
                    "occurredAt", Instant.now().toString()
            );
            kafkaTemplate.send(TRANSLATION_EVENTS_TOPIC, questionId.toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish TRANSLATIONS_MARKED_STALE event: {}", e.getMessage());
        }
    }
}
