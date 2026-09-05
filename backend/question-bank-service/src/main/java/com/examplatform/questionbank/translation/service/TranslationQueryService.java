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
import com.examplatform.questionbank.translation.domain.TranslatedQuestionPayload;
import com.examplatform.questionbank.translation.dto.TranslatedOptionDto;
import com.examplatform.questionbank.translation.dto.TranslationResponse;
import com.examplatform.questionbank.translation.repository.TranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only service for fetching translations.
 *
 * <p>Used by the delivery service to retrieve approved localized content
 * for candidates, and by the admin/translator UI to list all translations
 * for a question.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TranslationQueryService {

    private final TranslationRepository translationRepository;
    private final TranslationPayloadService payloadService;

    /**
     * Fetch the approved translation for a specific question and language.
     * Returns empty if no approved translation exists.
     *
     * @param questionId   source question UUID
     * @param languageCode BCP-47 / ISO 639 language code (e.g. "hi", "ta")
     * @param tenantId     examination authority identifier
     */
    public Optional<TranslationResponse> getApprovedTranslation(
            UUID questionId, String languageCode, String tenantId) {

        return translationRepository
                .findByQuestionIdAndLanguageCodeAndStatusAndTenantId(
                        questionId, languageCode, Translation.TranslationStatus.APPROVED, tenantId)
                .map(this::toResponse);
    }

    /**
     * List all translations for a question (all languages, all statuses).
     * Used for admin / translator dashboard views.
     *
     * @param questionId source question UUID
     * @param tenantId   examination authority identifier
     */
    public List<TranslationResponse> listTranslationsForQuestion(UUID questionId, String tenantId) {
        return translationRepository
                .findByQuestionIdAndTenantId(questionId, tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private TranslationResponse toResponse(Translation t) {
        TranslatedQuestionPayload payload =
                payloadService.deserialize(t.getTranslatedPayload(), t.isPayloadEncrypted());

        List<TranslatedOptionDto> options = Collections.emptyList();
        String content = null;
        String explanation = null;

        if (payload != null) {
            content = payload.content();
            explanation = payload.explanation();
            if (payload.options() != null) {
                options = payload.options().stream()
                        .map(o -> new TranslatedOptionDto(o.id(), o.text()))
                        .toList();
            }
        }

        return TranslationResponse.builder()
                .translationId(t.getId())
                .questionId(t.getQuestionId())
                .languageCode(t.getLanguageCode())
                .translatedContent(content)
                .translatedOptions(options)
                .translatedExplanation(explanation)
                .sourceVersion(t.getSourceVersion())
                .status(t.getStatus())
                .translatorId(t.getTranslatorId())
                .reviewerId(t.getReviewerId())
                .reviewComments(t.getReviewComments())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
