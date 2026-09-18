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

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.translation.domain.Translation;
import com.examplatform.questionbank.translation.domain.TranslatedQuestionPayload;
import com.examplatform.questionbank.translation.dto.TranslatedOptionDto;
import com.examplatform.questionbank.translation.dto.TranslationRequest;
import com.examplatform.questionbank.translation.repository.TranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages the translation request and resubmission workflow within Question Bank.
 * Creates, updates, and upserts translations linked to source questions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TranslationWorkflowService {

    /**
     * All 22 Eighth Schedule languages supported by the platform.
     * BCP-47 / ISO 639 codes aligned with IndicTrans2 language tags.
     */
    public static final Set<String> SUPPORTED_LANGUAGES = Set.of(
            "hi",  // Hindi
            "bn",  // Bengali
            "te",  // Telugu
            "mr",  // Marathi
            "ta",  // Tamil
            "ur",  // Urdu
            "gu",  // Gujarati
            "kn",  // Kannada
            "ml",  // Malayalam
            "or",  // Odia
            "pa",  // Punjabi
            "as",  // Assamese
            "mai", // Maithili
            "sa",  // Sanskrit
            "sd",  // Sindhi
            "ne",  // Nepali
            "kok", // Konkani
            "doi", // Dogri
            "mni", // Manipuri
            "sat", // Santali
            "bo",  // Bodo
            "kas"  // Kashmiri
    );

    private final TranslationRepository translationRepository;
    private final QuestionRepository questionRepository;
    private final TranslationPayloadService payloadService;

    /**
     * Request a new translation for a question in the specified language.
     * Creates a {@link Translation} entity in {@code DRAFT} status with the
     * structured payload (content + options + explanation).
     *
     * @param request  the translation request DTO from the translator
     * @param tenantId examination authority identifier
     * @return the created Translation entity
     * @throws IllegalArgumentException if the language code is unsupported, the
     *                                  source question is not found, or the supplied
     *                                  option IDs do not match the source question
     * @throws IllegalStateException    if a translation already exists for this
     *                                  question / language / tenant combination
     */
    public Translation requestTranslation(TranslationRequest request, String tenantId) {
        validateLanguageCode(request.getLanguageCode());

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Source question not found: " + request.getQuestionId()));

        ensureNoDuplicateTranslation(request.getQuestionId(), request.getLanguageCode(), tenantId);
        validateOptionIds(request, question);

        TranslatedQuestionPayload payload = buildPayload(request, question);
        String serialized = payloadService.serialize(payload);

        Translation translation = Translation.builder()
                .questionId(request.getQuestionId())
                .languageCode(request.getLanguageCode())
                .translatedPayload(serialized)
                .payloadEncrypted(payloadService.isEncryptionEnabled())
                .sourceVersion(question.getVersion() != null ? question.getVersion() : 0L)
                .status(Translation.TranslationStatus.DRAFT)
                .translatorId(request.getTranslatorId())
                .build();
        translation.setTenantId(tenantId);

        log.info("Translation requested: questionId={}, lang={}, translatorId={}, tenant={}",
                request.getQuestionId(), request.getLanguageCode(), request.getTranslatorId(), tenantId);

        return translationRepository.save(translation);
    }

    /**
     * Resubmit a rejected translation with updated content.
     * Transitions status from {@code REJECTED} back to {@code DRAFT}.
     *
     * @param translationId existing translation UUID
     * @param request       the updated translation payload
     * @param tenantId      examination authority identifier
     * @return updated Translation entity
     * @throws IllegalStateException if translation is not in REJECTED status
     */
    public Translation resubmitTranslation(
            UUID translationId, TranslationRequest request, String tenantId) {

        Translation translation = translationRepository.findById(translationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Translation not found: " + translationId));

        if (translation.getStatus() != Translation.TranslationStatus.REJECTED) {
            throw new IllegalStateException(
                    "Only REJECTED translations can be resubmitted. Current status: "
                            + translation.getStatus());
        }

        Question question = questionRepository.findById(translation.getQuestionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Source question not found: " + translation.getQuestionId()));

        validateOptionIds(request, question);

        TranslatedQuestionPayload payload = buildPayload(request, question);
        String serialized = payloadService.serialize(payload);

        translation.setTranslatedPayload(serialized);
        translation.setPayloadEncrypted(payloadService.isEncryptionEnabled());
        translation.setSourceVersion(question.getVersion() != null ? question.getVersion() : 0L);
        translation.setStatus(Translation.TranslationStatus.DRAFT);
        translation.setTranslatorId(request.getTranslatorId());
        translation.setReviewerId(null);
        translation.setReviewComments(null);

        log.info("Translation resubmitted: translationId={}, lang={}, tenant={}",
                translationId, translation.getLanguageCode(), tenantId);

        return translationRepository.save(translation);
    }

    /**
     * Upsert a translation (e.g. from automated IndicTrans2 background pipeline).
     * If existing translation exists, updates payload and sets to target status (e.g. PUBLISHED).
     * If not, inserts new translation directly.
     */
    public Translation upsertTranslation(
            UUID questionId,
            String languageCode,
            String content,
            List<TranslatedOptionDto> options,
            String explanation,
            Translation.TranslationStatus targetStatus,
            UUID translatorOrSystemId,
            String reviewComments,
            String tenantId) {

        validateLanguageCode(languageCode);

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Source question not found: " + questionId));

        Map<String, QuestionOption> sourceOptionMap = (question.getOptions() != null)
                ? question.getOptions().stream().collect(Collectors.toMap(QuestionOption::getId, o -> o, (a, b) -> a))
                : Collections.emptyMap();

        List<TranslatedQuestionPayload.TranslatedOption> payloadOptions;
        if (options != null && !options.isEmpty()) {
            payloadOptions = options.stream().map(dto -> {
                QuestionOption src = sourceOptionMap.get(dto.id());
                String imgUrl = (dto.imageUrl() != null && !dto.imageUrl().isBlank())
                        ? dto.imageUrl()
                        : (src != null ? src.getImageUrl() : null);
                String altText = (dto.imageAltText() != null && !dto.imageAltText().isBlank())
                        ? dto.imageAltText()
                        : (src != null ? src.getImageAltText() : null);
                return new TranslatedQuestionPayload.TranslatedOption(dto.id(), dto.text(), imgUrl, altText);
            }).toList();
        } else if (question.getOptions() != null && !question.getOptions().isEmpty()) {
            payloadOptions = question.getOptions().stream().map(src ->
                    new TranslatedQuestionPayload.TranslatedOption(
                            src.getId(),
                            src.getText(),
                            src.getImageUrl(),
                            src.getImageAltText()
                    )
            ).toList();
        } else {
            payloadOptions = Collections.emptyList();
        }

        TranslatedQuestionPayload payload = new TranslatedQuestionPayload(content, payloadOptions, explanation);
        String serialized = payloadService.serialize(payload);

        List<Translation> existingList = translationRepository
                .findByQuestionIdAndLanguageCodeAndTenantId(questionId, languageCode, tenantId);

        Translation.TranslationStatus resolvedStatus = (targetStatus != null) ? targetStatus : Translation.TranslationStatus.PUBLISHED;

        Translation translation;
        if (!existingList.isEmpty()) {
            translation = existingList.get(0);
            translation.setTranslatedPayload(serialized);
            translation.setPayloadEncrypted(payloadService.isEncryptionEnabled());
            translation.setSourceVersion(question.getVersion() != null ? question.getVersion() : 0L);
            translation.setStatus(resolvedStatus);
            if (translatorOrSystemId != null) {
                translation.setTranslatorId(translatorOrSystemId);
            }
            if (reviewComments != null) {
                translation.setReviewComments(reviewComments);
            }
            if (resolvedStatus == Translation.TranslationStatus.APPROVED || resolvedStatus == Translation.TranslationStatus.PUBLISHED) {
                translation.setReviewerId(translatorOrSystemId);
            }
        } else {
            translation = Translation.builder()
                    .questionId(questionId)
                    .languageCode(languageCode)
                    .translatedPayload(serialized)
                    .payloadEncrypted(payloadService.isEncryptionEnabled())
                    .sourceVersion(question.getVersion() != null ? question.getVersion() : 0L)
                    .status(resolvedStatus)
                    .translatorId(translatorOrSystemId != null ? translatorOrSystemId : UUID.fromString("00000000-0000-0000-0000-000000000001"))
                    .reviewerId((resolvedStatus == Translation.TranslationStatus.APPROVED || resolvedStatus == Translation.TranslationStatus.PUBLISHED) ? translatorOrSystemId : null)
                    .reviewComments(reviewComments)
                    .build();
            translation.setTenantId(tenantId);
        }

        log.info("Translation upserted: questionId={}, lang={}, status={}, tenant={}",
                questionId, languageCode, resolvedStatus, tenantId);

        return translationRepository.save(translation);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void validateLanguageCode(String code) {
        if (!SUPPORTED_LANGUAGES.contains(code)) {
            throw new IllegalArgumentException(
                    "Unsupported language code: " + code + ". Supported: " + SUPPORTED_LANGUAGES);
        }
    }

    private void ensureNoDuplicateTranslation(UUID questionId, String languageCode, String tenantId) {
        List<Translation> existing = translationRepository
                .findByQuestionIdAndLanguageCodeAndTenantId(questionId, languageCode, tenantId);
        if (!existing.isEmpty()) {
            throw new IllegalStateException(
                    "Translation already exists for question " + questionId
                            + " in language " + languageCode);
        }
    }

    /**
     * Validates that every option ID in the translated options matches an option
     * ID present in the source question, and that no source option is missing.
     */
    private void validateOptionIds(TranslationRequest request, Question question) {
        if (request.getTranslatedOptions() == null || request.getTranslatedOptions().isEmpty()) {
            return; // No options to validate (SHORT_ANSWER etc.)
        }

        List<QuestionOption> sourceOptions =
                question.getOptions() != null ? question.getOptions() : Collections.emptyList();

        Set<String> sourceIds = sourceOptions.stream()
                .map(QuestionOption::getId)
                .collect(Collectors.toSet());

        Set<String> translatedIds = request.getTranslatedOptions().stream()
                .map(TranslatedOptionDto::id)
                .collect(Collectors.toSet());

        Set<String> unknown = translatedIds.stream()
                .filter(id -> !sourceIds.contains(id))
                .collect(Collectors.toSet());

        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException(
                    "Translated options contain IDs not present in source question: " + unknown);
        }
    }

    private TranslatedQuestionPayload buildPayload(TranslationRequest request, Question question) {
        Map<String, QuestionOption> sourceOptionMap = (question != null && question.getOptions() != null)
                ? question.getOptions().stream().collect(Collectors.toMap(QuestionOption::getId, o -> o, (a, b) -> a))
                : Collections.emptyMap();

        List<TranslatedQuestionPayload.TranslatedOption> options;
        if (request.getTranslatedOptions() != null && !request.getTranslatedOptions().isEmpty()) {
            options = request.getTranslatedOptions().stream()
                    .map(dto -> {
                        QuestionOption src = sourceOptionMap.get(dto.id());
                        String imgUrl = (dto.imageUrl() != null && !dto.imageUrl().isBlank())
                                ? dto.imageUrl()
                                : (src != null ? src.getImageUrl() : null);
                        String altText = (dto.imageAltText() != null && !dto.imageAltText().isBlank())
                                ? dto.imageAltText()
                                : (src != null ? src.getImageAltText() : null);
                        return new TranslatedQuestionPayload.TranslatedOption(dto.id(), dto.text(), imgUrl, altText);
                    })
                    .toList();
        } else if (question != null && question.getOptions() != null && !question.getOptions().isEmpty()) {
            options = question.getOptions().stream().map(src ->
                    new TranslatedQuestionPayload.TranslatedOption(
                            src.getId(),
                            src.getText(),
                            src.getImageUrl(),
                            src.getImageAltText()
                    )
            ).toList();
        } else {
            options = Collections.emptyList();
        }

        return new TranslatedQuestionPayload(
                request.getTranslatedContent(),
                options,
                request.getTranslatedExplanation()
        );
    }
}
