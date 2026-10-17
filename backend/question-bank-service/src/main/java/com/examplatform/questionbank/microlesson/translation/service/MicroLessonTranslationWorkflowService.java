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

package com.examplatform.questionbank.microlesson.translation.service;

import com.examplatform.questionbank.microlesson.domain.SubtopicMicroLesson;
import com.examplatform.questionbank.microlesson.dto.MicroLessonResponse;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import com.examplatform.questionbank.microlesson.repository.SubtopicMicroLessonRepository;
import com.examplatform.questionbank.microlesson.translation.domain.MicroLessonTranslation;
import com.examplatform.questionbank.microlesson.translation.dto.MicroLessonTranslationResponse;
import com.examplatform.questionbank.microlesson.translation.repository.MicroLessonTranslationRepository;
import com.examplatform.questionbank.translation.service.TranslationWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MicroLessonTranslationWorkflowService {

    private final MicroLessonTranslationRepository translationRepository;
    private final SubtopicMicroLessonRepository lessonRepository;
    private final MicroLessonTranslationPayloadService payloadService;

    private static final UUID DEFAULT_SYSTEM_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Set<String> APPROVED_OR_PUBLISHED_STATUSES = Set.of("PUBLISHED", "APPROVED");

    /**
     * Auto-translates a single micro-lesson by ID into the target language using IndicTrans2
     * and saves it with status PUBLISHED.
     */
    public MicroLessonTranslationResponse autoTranslateSingleLesson(
            UUID lessonId,
            String targetLanguage,
            UUID translatorId,
            String tenantId) {

        String targetLang = resolveAndValidateTargetLanguage(targetLanguage);

        SubtopicMicroLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Micro-lesson not found: " + lessonId));

        MicroLessonTranslationPayloadService.TranslatedMicroLessonPayload translatedPayload =
                payloadService.translateMicroLesson(lesson, targetLang);

        UUID effectiveTranslatorId = (translatorId != null) ? translatorId : DEFAULT_SYSTEM_UUID;

        MicroLessonTranslation translation = upsertTranslation(
                lesson.getId(),
                targetLang,
                translatedPayload.coreConcept(),
                translatedPayload.trap(),
                translatedPayload.cheatSheetJson(),
                translatedPayload.stepByStepBreakdownJson(),
                "PUBLISHED",
                effectiveTranslatorId,
                null,
                "Auto-translated via IndicTrans2",
                lesson.getContentVersion(),
                tenantId
        );

        return toResponse(translation);
    }

    /**
     * Upsert a micro-lesson translation in an isolated transaction.
     * Uses Propagation.REQUIRES_NEW to isolate single-lesson translation operations
     * from failing other lessons during batch processing.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MicroLessonTranslation upsertTranslation(
            UUID lessonId,
            String languageCode,
            String coreConcept,
            String trap,
            String cheatSheetJson,
            String stepByStepBreakdownJson,
            String status,
            UUID translatorId,
            UUID reviewerId,
            String reviewComments,
            int sourceVersion,
            String tenantId) {

        String resolvedStatus = (status != null && !status.isBlank()) ? status : "PUBLISHED";
        String lang = languageCode != null ? languageCode.toLowerCase().trim() : "hi";
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";

        Optional<MicroLessonTranslation> existingOpt = translationRepository
                .findByMicrolessonIdAndLanguageCodeAndTenantId(lessonId, lang, effectiveTenant);

        MicroLessonTranslation translation;
        if (existingOpt.isPresent()) {
            translation = existingOpt.get();
            translation.setCoreConcept(coreConcept != null ? coreConcept : "");
            translation.setTrap(trap != null ? trap : "");
            translation.setCheatSheetJson(cheatSheetJson != null ? cheatSheetJson : "[]");
            translation.setStepByStepBreakdownJson(stepByStepBreakdownJson != null ? stepByStepBreakdownJson : "{}");
            translation.setSourceVersion(sourceVersion);
            translation.setStatus(resolvedStatus);
            if (translatorId != null) {
                translation.setTranslatorId(translatorId);
            }
            if (reviewerId != null) {
                translation.setReviewerId(reviewerId);
            }
            if (reviewComments != null) {
                translation.setReviewComments(reviewComments);
            }
            log.info("Updated existing micro-lesson translation id={} for lessonId={}, lang={}, tenant={}",
                    translation.getId(), lessonId, lang, effectiveTenant);
        } else {
            translation = MicroLessonTranslation.builder()
                    .microlessonId(lessonId)
                    .languageCode(lang)
                    .coreConcept(coreConcept != null ? coreConcept : "")
                    .trap(trap != null ? trap : "")
                    .cheatSheetJson(cheatSheetJson != null ? cheatSheetJson : "[]")
                    .stepByStepBreakdownJson(stepByStepBreakdownJson != null ? stepByStepBreakdownJson : "{}")
                    .sourceVersion(sourceVersion)
                    .status(resolvedStatus)
                    .translatorId(translatorId != null ? translatorId : DEFAULT_SYSTEM_UUID)
                    .reviewerId(reviewerId)
                    .reviewComments(reviewComments)
                    .build();
            translation.setTenantId(effectiveTenant);
            log.info("Created new micro-lesson translation for lessonId={}, lang={}, tenant={}",
                    lessonId, lang, effectiveTenant);
        }

        return translationRepository.save(translation);
    }

    /**
     * Approves an existing translation.
     */
    @Transactional
    public MicroLessonTranslationResponse approveTranslation(
            UUID translationId,
            UUID reviewerId,
            String reviewComments,
            String tenantId) {

        MicroLessonTranslation translation = translationRepository.findById(translationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Translation not found: " + translationId));

        translation.setStatus("APPROVED");
        translation.setReviewerId(reviewerId);
        if (reviewComments != null && !reviewComments.isBlank()) {
            translation.setReviewComments(reviewComments);
        }

        MicroLessonTranslation saved = translationRepository.save(translation);
        log.info("Translation {} approved by reviewer {}", translationId, reviewerId);
        return toResponse(saved);
    }

    /**
     * Rejects an existing translation with reviewer comments.
     */
    @Transactional
    public MicroLessonTranslationResponse rejectTranslation(
            UUID translationId,
            UUID reviewerId,
            String reviewComments,
            String tenantId) {

        MicroLessonTranslation translation = translationRepository.findById(translationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Translation not found: " + translationId));

        translation.setStatus("REJECTED");
        translation.setReviewerId(reviewerId);
        translation.setReviewComments(reviewComments);

        MicroLessonTranslation saved = translationRepository.save(translation);
        log.info("Translation {} rejected by reviewer {}: {}", translationId, reviewerId, reviewComments);
        return toResponse(saved);
    }

    /**
     * Lists all translations for a micro-lesson (administrative view).
     */
    @Transactional(readOnly = true)
    public List<MicroLessonTranslationResponse> getTranslationsForLesson(UUID lessonId, String tenantId) {
        List<MicroLessonTranslation> list = translationRepository.findByMicrolessonId(lessonId);
        return list.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retrieves localized micro-lesson for candidate consumption with automatic fallback to English.
     */
    @Transactional(readOnly = true)
    public MicroLessonResponse getLocalizedMicroLesson(UUID lessonId, String lang, String tenantId) {
        SubtopicMicroLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Micro-lesson not found: " + lessonId));

        if (lang == null || lang.isBlank() || "en".equalsIgnoreCase(lang.trim()) || "eng".equalsIgnoreCase(lang.trim())) {
            return toMicroLessonResponse(lesson, "en", false);
        }

        String targetLang = lang.trim().toLowerCase();

        List<MicroLessonTranslation> translations = translationRepository.findByMicrolessonIdAndLanguageCodeAndStatusIn(
                lesson.getId(), targetLang, APPROVED_OR_PUBLISHED_STATUSES, tenantId);

        if (!translations.isEmpty()) {
            MicroLessonTranslation matched = translations.getFirst();
            return toLocalizedMicroLessonResponse(lesson, matched, targetLang, false);
        }

        // Fallback to English base lesson
        log.info("No approved translation found for lessonId={} in lang={}. Falling back to English.", lessonId, targetLang);
        return toMicroLessonResponse(lesson, "en", true);
    }

    public MicroLessonResponse toMicroLessonResponse(SubtopicMicroLesson lesson, String languageCode, boolean fallbackToEnglish) {
        List<String> cheatSheet = payloadService.parseCheatSheet(lesson.getCheatSheetJson());
        StepByStepBreakdownDto breakdown = payloadService.parseBreakdownJson(lesson.getStepByStepBreakdownJson());

        return new MicroLessonResponse(
                lesson.getId(),
                lesson.getSubjectId(),
                lesson.getTopicId(),
                lesson.getSubtopicId(),
                lesson.getSubject(),
                lesson.getTopic(),
                lesson.getSubtopic(),
                lesson.getTargetExam(),
                lesson.getCoreConcept(),
                lesson.getTrap(),
                cheatSheet,
                breakdown,
                lesson.getStatus(),
                lesson.getReviewedBy(),
                lesson.getReviewedAt(),
                lesson.getReviewComments(),
                lesson.getContentVersion(),
                lesson.getCreatedAt(),
                lesson.getUpdatedAt(),
                languageCode,
                fallbackToEnglish
        );
    }

    public MicroLessonResponse toLocalizedMicroLessonResponse(
            SubtopicMicroLesson lesson,
            MicroLessonTranslation translation,
            String languageCode,
            boolean fallbackToEnglish) {

        List<String> cheatSheet = payloadService.parseCheatSheet(translation.getCheatSheetJson());
        StepByStepBreakdownDto breakdown = payloadService.parseBreakdownJson(translation.getStepByStepBreakdownJson());

        return new MicroLessonResponse(
                lesson.getId(),
                lesson.getSubjectId(),
                lesson.getTopicId(),
                lesson.getSubtopicId(),
                lesson.getSubject(),
                lesson.getTopic(),
                lesson.getSubtopic(),
                lesson.getTargetExam(),
                translation.getCoreConcept(),
                translation.getTrap(),
                cheatSheet,
                breakdown,
                translation.getStatus(),
                translation.getReviewerId(),
                translation.getUpdatedAt(),
                translation.getReviewComments(),
                translation.getSourceVersion(),
                translation.getCreatedAt(),
                translation.getUpdatedAt(),
                languageCode,
                fallbackToEnglish
        );
    }

    public MicroLessonTranslationResponse toResponse(MicroLessonTranslation entity) {
        List<String> cheatSheet = payloadService.parseCheatSheet(entity.getCheatSheetJson());
        StepByStepBreakdownDto breakdown = payloadService.parseBreakdownJson(entity.getStepByStepBreakdownJson());

        return MicroLessonTranslationResponse.builder()
                .id(entity.getId())
                .microlessonId(entity.getMicrolessonId())
                .languageCode(entity.getLanguageCode())
                .coreConcept(entity.getCoreConcept())
                .trap(entity.getTrap())
                .cheatSheet(cheatSheet)
                .stepByStepBreakdown(breakdown)
                .sourceVersion(entity.getSourceVersion())
                .status(entity.getStatus())
                .translatorId(entity.getTranslatorId())
                .reviewerId(entity.getReviewerId())
                .reviewComments(entity.getReviewComments())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }

    private String resolveAndValidateTargetLanguage(String targetLanguage) {
        String targetLang = (targetLanguage != null && !targetLanguage.isBlank())
                ? targetLanguage.toLowerCase().trim()
                : "hi";

        if (!TranslationWorkflowService.SUPPORTED_LANGUAGES.contains(targetLang)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported target language code: " + targetLang);
        }
        return targetLang;
    }
}
