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
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.translation.domain.Translation;
import com.examplatform.questionbank.translation.repository.TranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Manages the translation request workflow within Question Bank.
 * Creates translations in DRAFT state linked to source questions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TranslationWorkflowService {

    /**
     * All 22 Eighth Schedule languages + extras supported by the platform.
     */
    public static final Set<String> SUPPORTED_LANGUAGES = Set.of(
            "hi", "bn", "te", "mr", "ta", "ur", "gu", "kn", "ml", "or",
            "pa", "as", "mai", "sa", "sd", "ne", "kok", "doi", "mni", "sat", "bo", "kas"
    );

    private final TranslationRepository translationRepository;
    private final QuestionRepository questionRepository;

    /**
     * Request a new translation for a question in the specified language.
     * Creates a Translation entity in DRAFT status.
     *
     * @param questionId   the source question UUID
     * @param languageCode ISO language code (must be in SUPPORTED_LANGUAGES)
     * @param translatorId the translator user UUID
     * @param tenantId     examination authority identifier
     * @return the created Translation entity
     */
    public Translation requestTranslation(UUID questionId, String languageCode,
                                           UUID translatorId, String tenantId) {
        // Validate language code
        if (!SUPPORTED_LANGUAGES.contains(languageCode)) {
            throw new IllegalArgumentException(
                    "Unsupported language code: " + languageCode +
                    ". Supported: " + SUPPORTED_LANGUAGES);
        }

        // Validate source question exists
        Optional<Question> questionOpt = questionRepository.findById(questionId);
        if (questionOpt.isEmpty()) {
            throw new IllegalArgumentException("Source question not found: " + questionId);
        }

        log.info("Translation requested for questionId={}, lang={}, translator={}, tenantId={}",
                questionId, languageCode, translatorId, tenantId);

        // Check for existing translation of the same question+language+tenant
        List<Translation> existing = translationRepository
                .findByQuestionIdAndLanguageCodeAndTenantId(questionId, languageCode, tenantId);
        if (!existing.isEmpty()) {
            throw new IllegalStateException(
                    "Translation already exists for question " + questionId +
                    " in language " + languageCode);
        }

        Translation translation = Translation.builder()
                .questionId(questionId)
                .languageCode(languageCode)
                .status(Translation.TranslationStatus.DRAFT)
                .translatorId(translatorId)
                .build();
        translation.setTenantId(tenantId);

        return translationRepository.save(translation);
    }
}
