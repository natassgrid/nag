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
import com.examplatform.questionbank.microlesson.dto.*;
import com.examplatform.questionbank.translation.ai.IndicTrans2Service;
import com.examplatform.shared.ai.latex.LatexPreservationUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service to extract, translate, preserve LaTeX formulas in, and reassemble
 * multilingual micro-lesson contents.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MicroLessonTranslationPayloadService {

    private final IndicTrans2Service indicTrans2Service;
    private final ObjectMapper objectMapper;

    public record TranslatedMicroLessonPayload(
            String coreConcept,
            String trap,
            String cheatSheetJson,
            String stepByStepBreakdownJson,
            List<String> cheatSheetList,
            StepByStepBreakdownDto breakdownDto
    ) {}

    private static class TranslatableItem {
        final String text;
        final Consumer<String> setter;

        TranslatableItem(String text, Consumer<String> setter) {
            this.text = text;
            this.setter = setter;
        }
    }

    public TranslatedMicroLessonPayload translateMicroLesson(SubtopicMicroLesson lesson, String targetLanguage) {
        if (lesson == null) {
            throw new IllegalArgumentException("SubtopicMicroLesson must not be null");
        }

        List<TranslatableItem> translatableItems = new ArrayList<>();

        // 1. Core concept and Trap
        String[] coreConceptHolder = new String[]{lesson.getCoreConcept() != null ? lesson.getCoreConcept() : ""};
        registerItem(translatableItems, lesson.getCoreConcept(), t -> coreConceptHolder[0] = t);

        String[] trapHolder = new String[]{lesson.getTrap() != null ? lesson.getTrap() : ""};
        registerItem(translatableItems, lesson.getTrap(), t -> trapHolder[0] = t);

        // 2. Cheat Sheet
        List<String> rawCheatSheet = parseCheatSheetJson(lesson.getCheatSheetJson());
        List<String> translatedCheatSheet = new ArrayList<>(rawCheatSheet);
        for (int i = 0; i < rawCheatSheet.size(); i++) {
            final int idx = i;
            registerItem(translatableItems, rawCheatSheet.get(i), t -> translatedCheatSheet.set(idx, t));
        }

        // 3. Step By Step Breakdown
        StepByStepBreakdownDto breakdownDto = parseBreakdownJson(lesson.getStepByStepBreakdownJson());
        BreakdownRebuilder breakdownRebuilder = new BreakdownRebuilder(breakdownDto);
        breakdownRebuilder.registerItems(translatableItems);

        // 4. Batch translation execution
        if (!translatableItems.isEmpty()) {
            List<String> rawTexts = translatableItems.stream()
                    .map(it -> it.text)
                    .toList();

            List<String> translatedTexts = indicTrans2Service.translateBatch(rawTexts, targetLanguage);

            for (int i = 0; i < translatableItems.size(); i++) {
                String translated = (i < translatedTexts.size()) ? translatedTexts.get(i) : translatableItems.get(i).text;
                String cleaned = LatexPreservationUtil.cleanStrayUnderscores(translated);
                translatableItems.get(i).setter.accept(cleaned);
            }
        }

        // 5. Reconstruct JSON payloads
        String translatedCheatSheetJson = serialize(translatedCheatSheet);
        StepByStepBreakdownDto finalBreakdownDto = breakdownRebuilder.build();
        String translatedBreakdownJson = serialize(finalBreakdownDto);

        return new TranslatedMicroLessonPayload(
                coreConceptHolder[0],
                trapHolder[0],
                translatedCheatSheetJson,
                translatedBreakdownJson,
                translatedCheatSheet,
                finalBreakdownDto
        );
    }

    private void registerItem(List<TranslatableItem> items, String text, Consumer<String> setter) {
        if (text != null && !text.isBlank()) {
            items.add(new TranslatableItem(text, setter));
        }
    }

    private List<String> parseCheatSheetJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse cheatSheetJson: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public StepByStepBreakdownDto parseBreakdownJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, StepByStepBreakdownDto.class);
        } catch (Exception e) {
            log.warn("Failed to parse stepByStepBreakdownJson: {}", e.getMessage());
            return null;
        }
    }

    public List<String> parseCheatSheet(String json) {
        return parseCheatSheetJson(json);
    }

    private String serialize(Object obj) {
        if (obj == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize micro-lesson JSON", e);
        }
    }

    private class BreakdownRebuilder {
        private final StepByStepBreakdownDto source;
        private String questionStem;
        private String finalAnswer;
        private List<String> steps;
        private List<StepItemHolder> structuredSteps;
        private TrapAlertHolder structuredTrapAlert;
        private List<FormulaHolder> structuredFormulas;
        private List<String> keyTakeaways;
        private List<QuickCheckHolder> quickCheckItems;

        BreakdownRebuilder(StepByStepBreakdownDto source) {
            this.source = source;
            if (source != null) {
                this.questionStem = source.questionStem();
                this.finalAnswer = source.finalAnswer();

                if (source.steps() != null) {
                    this.steps = new ArrayList<>(source.steps());
                }
                if (source.structuredSteps() != null) {
                    this.structuredSteps = new ArrayList<>();
                    for (StepItemDto s : source.structuredSteps()) {
                        this.structuredSteps.add(new StepItemHolder(s));
                    }
                }
                if (source.structuredTrapAlert() != null) {
                    this.structuredTrapAlert = new TrapAlertHolder(source.structuredTrapAlert());
                }
                if (source.structuredFormulas() != null) {
                    this.structuredFormulas = new ArrayList<>();
                    for (FormulaEntryDto f : source.structuredFormulas()) {
                        this.structuredFormulas.add(new FormulaHolder(f));
                    }
                }
                if (source.keyTakeaways() != null) {
                    this.keyTakeaways = new ArrayList<>(source.keyTakeaways());
                }
                if (source.quickCheckItems() != null) {
                    this.quickCheckItems = new ArrayList<>();
                    for (QuickCheckItemDto q : source.quickCheckItems()) {
                        this.quickCheckItems.add(new QuickCheckHolder(q));
                    }
                }
            }
        }

        void registerItems(List<TranslatableItem> items) {
            if (source == null) {
                return;
            }

            registerItem(items, questionStem, t -> questionStem = t);
            registerItem(items, finalAnswer, t -> finalAnswer = t);

            if (steps != null) {
                for (int i = 0; i < steps.size(); i++) {
                    final int idx = i;
                    registerItem(items, steps.get(i), t -> steps.set(idx, t));
                }
            }

            if (structuredSteps != null) {
                for (StepItemHolder stepHolder : structuredSteps) {
                    registerItem(items, stepHolder.title, t -> stepHolder.title = t);
                    registerItem(items, stepHolder.explanation, t -> stepHolder.explanation = t);
                    registerItem(items, stepHolder.tip, t -> stepHolder.tip = t);
                    // stepNumber and formula (LaTeX) are preserved intact
                }
            }

            if (structuredTrapAlert != null) {
                registerItem(items, structuredTrapAlert.title, t -> structuredTrapAlert.title = t);
                registerItem(items, structuredTrapAlert.description, t -> structuredTrapAlert.description = t);
                registerItem(items, structuredTrapAlert.commonMistake, t -> structuredTrapAlert.commonMistake = t);
                registerItem(items, structuredTrapAlert.correctApproach, t -> structuredTrapAlert.correctApproach = t);
            }

            if (structuredFormulas != null) {
                for (FormulaHolder formulaHolder : structuredFormulas) {
                    registerItem(items, formulaHolder.name, t -> formulaHolder.name = t);
                    registerItem(items, formulaHolder.description, t -> formulaHolder.description = t);
                    if (formulaHolder.variables != null) {
                        for (int v = 0; v < formulaHolder.variables.size(); v++) {
                            final int vIdx = v;
                            registerItem(items, formulaHolder.variables.get(v), t -> formulaHolder.variables.set(vIdx, t));
                        }
                    }
                    // latex formula is preserved intact
                }
            }

            if (keyTakeaways != null) {
                for (int i = 0; i < keyTakeaways.size(); i++) {
                    final int idx = i;
                    registerItem(items, keyTakeaways.get(i), t -> keyTakeaways.set(idx, t));
                }
            }

            if (quickCheckItems != null) {
                for (QuickCheckHolder qch : quickCheckItems) {
                    registerItem(items, qch.question, t -> qch.question = t);
                    if (qch.options != null) {
                        for (int opt = 0; opt < qch.options.size(); opt++) {
                            final int optIdx = opt;
                            registerItem(items, qch.options.get(opt), t -> qch.options.set(optIdx, t));
                        }
                    }
                    registerItem(items, qch.correctAnswer, t -> qch.correctAnswer = t);
                    registerItem(items, qch.explanation, t -> qch.explanation = t);
                }
            }
        }

        StepByStepBreakdownDto build() {
            if (source == null) {
                return null;
            }

            List<StepItemDto> builtSteps = null;
            if (structuredSteps != null) {
                builtSteps = structuredSteps.stream()
                        .map(StepItemHolder::toDto)
                        .toList();
            }

            TrapAlertDto builtTrap = structuredTrapAlert != null ? structuredTrapAlert.toDto() : null;

            List<FormulaEntryDto> builtFormulas = null;
            if (structuredFormulas != null) {
                builtFormulas = structuredFormulas.stream()
                        .map(FormulaHolder::toDto)
                        .toList();
            }

            List<QuickCheckItemDto> builtQuickCheck = null;
            if (quickCheckItems != null) {
                builtQuickCheck = quickCheckItems.stream()
                        .map(QuickCheckHolder::toDto)
                        .toList();
            }

            return new StepByStepBreakdownDto(
                    questionStem,
                    steps,
                    finalAnswer,
                    builtSteps,
                    builtTrap,
                    builtFormulas,
                    keyTakeaways,
                    builtQuickCheck
            );
        }
    }

    private static class StepItemHolder {
        int stepNumber;
        String title;
        String explanation;
        String formula;
        String tip;

        StepItemHolder(StepItemDto dto) {
            this.stepNumber = dto.stepNumber();
            this.title = dto.title();
            this.explanation = dto.explanation();
            this.formula = dto.formula();
            this.tip = dto.tip();
        }

        StepItemDto toDto() {
            return new StepItemDto(stepNumber, title, explanation, formula, tip);
        }
    }

    private static class TrapAlertHolder {
        String title;
        String description;
        String commonMistake;
        String correctApproach;

        TrapAlertHolder(TrapAlertDto dto) {
            this.title = dto.title();
            this.description = dto.description();
            this.commonMistake = dto.commonMistake();
            this.correctApproach = dto.correctApproach();
        }

        TrapAlertDto toDto() {
            return new TrapAlertDto(title, description, commonMistake, correctApproach);
        }
    }

    private static class FormulaHolder {
        String name;
        String latex;
        String description;
        List<String> variables;

        FormulaHolder(FormulaEntryDto dto) {
            this.name = dto.name();
            this.latex = dto.latex();
            this.description = dto.description();
            this.variables = dto.variables() != null ? new ArrayList<>(dto.variables()) : null;
        }

        FormulaEntryDto toDto() {
            return new FormulaEntryDto(name, latex, description, variables);
        }
    }

    private static class QuickCheckHolder {
        String question;
        List<String> options;
        String correctAnswer;
        String explanation;

        QuickCheckHolder(QuickCheckItemDto dto) {
            this.question = dto.question();
            this.options = dto.options() != null ? new ArrayList<>(dto.options()) : null;
            this.correctAnswer = dto.correctAnswer();
            this.explanation = dto.explanation();
        }

        QuickCheckItemDto toDto() {
            return new QuickCheckItemDto(question, options, correctAnswer, explanation);
        }
    }
}
