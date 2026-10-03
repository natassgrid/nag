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

import com.examplatform.delivery.dto.QuestionDeliveryDto;
import com.examplatform.delivery.dto.QuestionOptionDeliveryDto;
import com.examplatform.delivery.dto.TranslatedQuestionDeliveryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

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

        long baseSeed = seedId != null
                ? (seedId.getMostSignificantBits() ^ seedId.getLeastSignificantBits())
                : System.currentTimeMillis();

        List<QuestionDeliveryDto> randomizedList = new ArrayList<>(questions.size());

        for (QuestionDeliveryDto q : questions) {
            if (q.getOptions() == null || q.getOptions().size() <= 1) {
                randomizedList.add(q);
                continue;
            }

            long qSeed = baseSeed ^ (q.getId() != null ? q.getId().hashCode() : 0);
            Random random = new Random(qSeed);

            List<QuestionOptionDeliveryDto> shuffled = new ArrayList<>(q.getOptions());
            Collections.shuffle(shuffled, random);

            Integer newCorrectOptionIndex = null;
            List<QuestionOptionDeliveryDto> reindexedOptions = new ArrayList<>(shuffled.size());
            Map<String, Integer> idToNewIndexMap = new HashMap<>();

            for (int i = 0; i < shuffled.size(); i++) {
                QuestionOptionDeliveryDto original = shuffled.get(i);
                if (q.getCorrectOptionIndex() != null && original.getOriginalIndex() == q.getCorrectOptionIndex()) {
                    newCorrectOptionIndex = i;
                }
                idToNewIndexMap.put(original.getId(), i);

                reindexedOptions.add(QuestionOptionDeliveryDto.builder()
                        .id(original.getId())
                        .index(i)
                        .originalIndex(original.getOriginalIndex())
                        .text(original.getText())
                        .imageUrl(original.getImageUrl())
                        .imageAltText(original.getImageAltText())
                        .build());
            }

            // Also re-index translations to match the exact option IDs and preserve image assets
            Map<String, TranslatedQuestionDeliveryDto> randomizedTranslations = new HashMap<>();
            if (q.getTranslations() != null) {
                for (Map.Entry<String, TranslatedQuestionDeliveryDto> entry : q.getTranslations().entrySet()) {
                    TranslatedQuestionDeliveryDto trans = entry.getValue();
                    List<QuestionOptionDeliveryDto> transOptions = new ArrayList<>();
                    if (trans.getOptions() != null && !trans.getOptions().isEmpty()) {
                        Map<String, QuestionOptionDeliveryDto> transOptMap = new HashMap<>();
                        for (QuestionOptionDeliveryDto opt : trans.getOptions()) {
                            transOptMap.put(opt.getId(), opt);
                        }

                        for (int i = 0; i < reindexedOptions.size(); i++) {
                            QuestionOptionDeliveryDto baseOpt = reindexedOptions.get(i);
                            QuestionOptionDeliveryDto transOpt = transOptMap.get(baseOpt.getId());
                            if (transOpt != null) {
                                String optImg = (transOpt.getImageUrl() != null && !transOpt.getImageUrl().isBlank())
                                        ? transOpt.getImageUrl()
                                        : baseOpt.getImageUrl();
                                String optAlt = (transOpt.getImageAltText() != null && !transOpt.getImageAltText().isBlank())
                                        ? transOpt.getImageAltText()
                                        : baseOpt.getImageAltText();

                                transOptions.add(QuestionOptionDeliveryDto.builder()
                                        .id(baseOpt.getId())
                                        .index(i)
                                        .originalIndex(transOpt.getOriginalIndex())
                                        .text(transOpt.getText())
                                        .imageUrl(optImg)
                                        .imageAltText(optAlt)
                                        .build());
                            } else {
                                transOptions.add(QuestionOptionDeliveryDto.builder()
                                        .id(baseOpt.getId())
                                        .index(i)
                                        .originalIndex(baseOpt.getOriginalIndex())
                                        .text(baseOpt.getText())
                                        .imageUrl(baseOpt.getImageUrl())
                                        .imageAltText(baseOpt.getImageAltText())
                                        .build());
                            }
                        }
                    }

                    randomizedTranslations.put(entry.getKey(), TranslatedQuestionDeliveryDto.builder()
                            .languageCode(trans.getLanguageCode())
                            .content(trans.getContent())
                            .imageUrl(trans.getImageUrl() != null ? trans.getImageUrl() : q.getImageUrl())
                            .imageAltText(trans.getImageAltText() != null ? trans.getImageAltText() : q.getImageAltText())
                            .options(transOptions)
                            .explanation(trans.getExplanation())
                            .build());
                }
            }

            randomizedList.add(QuestionDeliveryDto.builder()
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
                    .correctOptionIndex(newCorrectOptionIndex != null ? newCorrectOptionIndex : q.getCorrectOptionIndex())
                    .explanation(q.getExplanation())
                    .questionType(q.getQuestionType())
                    .passageId(q.getPassageId())
                    .passageContent(q.getPassageContent())
                    .passageOrderIndex(q.getPassageOrderIndex())
                    .translations(randomizedTranslations)
                    .build());
        }

        return randomizedList;
    }
}
