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

package com.examplatform.delivery.service;

import com.examplatform.delivery.dto.QuestionDeliveryDto;
import com.examplatform.delivery.dto.QuestionOptionDeliveryDto;
import com.examplatform.delivery.dto.TranslatedQuestionDeliveryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Component responsible for deterministic option randomization seeded per candidate test session,
 * maintaining option ID mappings, correct answer index reindexing, and syncing multi-language translations.
 */
@Slf4j
@Component
public class OptionRandomizer {

    /**
     * Randomizes option order for each question deterministically using a session seed.
     * Preserves originalIndex, option ID, imageUrl, imageAltText, and passage grouping info.
     *
     * @param questions original list of questions
     * @param seedId    UUID used as randomization seed (e.g. sessionId or candidateId)
     * @return new list of questions with randomized option order
     */
    public List<QuestionDeliveryDto> randomizeOptions(List<QuestionDeliveryDto> questions, UUID seedId) {
        if (questions == null || questions.isEmpty()) {
            return Collections.emptyList();
        }

        long baseSeed = computeBaseSeed(seedId);
        List<QuestionDeliveryDto> randomizedList = new ArrayList<>(questions.size());

        for (QuestionDeliveryDto q : questions) {
            randomizedList.add(randomizeQuestion(q, baseSeed));
        }

        return randomizedList;
    }

    private long computeBaseSeed(UUID seedId) {
        if (seedId != null) {
            return seedId.getMostSignificantBits() ^ seedId.getLeastSignificantBits();
        }
        return System.currentTimeMillis();
    }

    private QuestionDeliveryDto randomizeQuestion(QuestionDeliveryDto q, long baseSeed) {
        if (q.getOptions() == null || q.getOptions().size() <= 1) {
            return q;
        }

        long qSeed = baseSeed ^ (q.getId() != null ? q.getId().hashCode() : 0);
        List<QuestionOptionDeliveryDto> shuffled = new ArrayList<>(q.getOptions());
        Collections.shuffle(shuffled, new Random(qSeed));

        ReindexedOptionsResult reindexedResult = reindexOptions(shuffled, q.getCorrectOptionIndex());
        Map<String, TranslatedQuestionDeliveryDto> randomizedTranslations =
                reindexTranslations(q.getTranslations(), reindexedResult.reindexedOptions(), q.getImageUrl(), q.getImageAltText());

        Integer finalCorrectIndex = reindexedResult.newCorrectOptionIndex() != null
                ? reindexedResult.newCorrectOptionIndex()
                : q.getCorrectOptionIndex();

        return buildRandomizedQuestion(q, reindexedResult.reindexedOptions(), finalCorrectIndex, randomizedTranslations);
    }

    record ReindexedOptionsResult(
            List<QuestionOptionDeliveryDto> reindexedOptions,
            Integer newCorrectOptionIndex
    ) {}

    private ReindexedOptionsResult reindexOptions(List<QuestionOptionDeliveryDto> shuffled, Integer originalCorrectIndex) {
        Integer newCorrectOptionIndex = null;
        List<QuestionOptionDeliveryDto> reindexedOptions = new ArrayList<>(shuffled.size());

        for (int i = 0; i < shuffled.size(); i++) {
            QuestionOptionDeliveryDto original = shuffled.get(i);
            if (originalCorrectIndex != null && original.getOriginalIndex() == originalCorrectIndex) {
                newCorrectOptionIndex = i;
            }

            reindexedOptions.add(QuestionOptionDeliveryDto.builder()
                    .id(original.getId())
                    .index(i)
                    .originalIndex(original.getOriginalIndex())
                    .text(original.getText())
                    .imageUrl(original.getImageUrl())
                    .imageAltText(original.getImageAltText())
                    .build());
        }

        return new ReindexedOptionsResult(reindexedOptions, newCorrectOptionIndex);
    }

    private Map<String, TranslatedQuestionDeliveryDto> reindexTranslations(
            Map<String, TranslatedQuestionDeliveryDto> translations,
            List<QuestionOptionDeliveryDto> baseOptions,
            String defaultImageUrl,
            String defaultImageAltText) {
        if (translations == null || translations.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, TranslatedQuestionDeliveryDto> randomizedTranslations = new HashMap<>();
        for (Map.Entry<String, TranslatedQuestionDeliveryDto> entry : translations.entrySet()) {
            randomizedTranslations.put(
                    entry.getKey(),
                    reindexSingleTranslation(entry.getValue(), baseOptions, defaultImageUrl, defaultImageAltText)
            );
        }
        return randomizedTranslations;
    }

    private TranslatedQuestionDeliveryDto reindexSingleTranslation(
            TranslatedQuestionDeliveryDto trans,
            List<QuestionOptionDeliveryDto> baseOptions,
            String defaultImageUrl,
            String defaultImageAltText) {
        List<QuestionOptionDeliveryDto> transOptions = reindexTranslationOptions(trans.getOptions(), baseOptions);

        return TranslatedQuestionDeliveryDto.builder()
                .languageCode(trans.getLanguageCode())
                .content(trans.getContent())
                .imageUrl(trans.getImageUrl() != null ? trans.getImageUrl() : defaultImageUrl)
                .imageAltText(trans.getImageAltText() != null ? trans.getImageAltText() : defaultImageAltText)
                .options(transOptions)
                .explanation(trans.getExplanation())
                .build();
    }

    private List<QuestionOptionDeliveryDto> reindexTranslationOptions(
            List<QuestionOptionDeliveryDto> transOptionsList,
            List<QuestionOptionDeliveryDto> baseOptions) {
        if (transOptionsList == null || transOptionsList.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, QuestionOptionDeliveryDto> transOptMap = new HashMap<>();
        for (QuestionOptionDeliveryDto opt : transOptionsList) {
            transOptMap.put(opt.getId(), opt);
        }

        List<QuestionOptionDeliveryDto> result = new ArrayList<>(baseOptions.size());
        for (int i = 0; i < baseOptions.size(); i++) {
            QuestionOptionDeliveryDto baseOpt = baseOptions.get(i);
            QuestionOptionDeliveryDto transOpt = transOptMap.get(baseOpt.getId());
            result.add(buildAlignedOption(baseOpt, transOpt, i));
        }
        return result;
    }

    private QuestionOptionDeliveryDto buildAlignedOption(
            QuestionOptionDeliveryDto baseOpt,
            QuestionOptionDeliveryDto transOpt,
            int index) {
        if (transOpt == null) {
            return QuestionOptionDeliveryDto.builder()
                    .id(baseOpt.getId())
                    .index(index)
                    .originalIndex(baseOpt.getOriginalIndex())
                    .text(baseOpt.getText())
                    .imageUrl(baseOpt.getImageUrl())
                    .imageAltText(baseOpt.getImageAltText())
                    .build();
        }

        String optImg = resolveImage(transOpt.getImageUrl(), baseOpt.getImageUrl());
        String optAlt = resolveImage(transOpt.getImageAltText(), baseOpt.getImageAltText());

        return QuestionOptionDeliveryDto.builder()
                .id(baseOpt.getId())
                .index(index)
                .originalIndex(transOpt.getOriginalIndex())
                .text(transOpt.getText())
                .imageUrl(optImg)
                .imageAltText(optAlt)
                .build();
    }

    private String resolveImage(String specific, String fallback) {
        return (specific != null && !specific.isBlank()) ? specific : fallback;
    }

    private QuestionDeliveryDto buildRandomizedQuestion(
            QuestionDeliveryDto q,
            List<QuestionOptionDeliveryDto> reindexedOptions,
            Integer finalCorrectIndex,
            Map<String, TranslatedQuestionDeliveryDto> randomizedTranslations) {
        return QuestionDeliveryDto.builder()
                .id(q.getId())
                .text(q.getText())
                .imageUrl(q.getImageUrl())
                .imageAltText(q.getImageAltText())
                .hasImages(q.isHasImages())
                .options(reindexedOptions)
                .marks(q.getMarks())
                .negativeMarks(q.getNegativeMarks())
                .sectionId(q.getSectionId())
                .sectionName(q.getSectionName())
                .topic(q.getTopic())
                .correctOptionIndex(finalCorrectIndex)
                .explanation(q.getExplanation())
                .questionType(q.getQuestionType())
                .passageId(q.getPassageId())
                .passageContent(q.getPassageContent())
                .passageOrderIndex(q.getPassageOrderIndex())
                .translations(randomizedTranslations)
                .build();
    }
}
