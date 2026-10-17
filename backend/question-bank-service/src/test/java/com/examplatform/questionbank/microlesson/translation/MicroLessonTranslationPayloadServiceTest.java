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

package com.examplatform.questionbank.microlesson.translation;

import com.examplatform.questionbank.microlesson.domain.SubtopicMicroLesson;
import com.examplatform.questionbank.microlesson.dto.*;
import com.examplatform.questionbank.microlesson.translation.service.MicroLessonTranslationPayloadService;
import com.examplatform.questionbank.translation.ai.IndicTrans2Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MicroLessonTranslationPayloadServiceTest {

    @Mock
    private IndicTrans2Service indicTrans2Service;

    private ObjectMapper objectMapper;
    private MicroLessonTranslationPayloadService payloadService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        payloadService = new MicroLessonTranslationPayloadService(indicTrans2Service, objectMapper);
    }

    @Test
    @DisplayName("Should extract, translate, and preserve LaTeX formulas in micro-lesson components")
    void shouldExtractAndTranslateAllMicroLessonFieldsPreservingLatex() throws Exception {
        StepItemDto stepItem = new StepItemDto(1, "Step 1 Title", "Explanation with $$F = ma$$", "\\vec{F} = m\\vec{a}", "Check units");
        TrapAlertDto trapAlert = new TrapAlertDto("Trap Alert", "Trap Desc", "Common mistake $$v = u + at$$", "Correct way");
        FormulaEntryDto formula = new FormulaEntryDto("Newton Second Law", "F = ma", "Calculates net force", List.of("m", "a"));
        QuickCheckItemDto quickCheck = new QuickCheckItemDto("What is $$F$$?", List.of("$$10\\text{ N}$$", "$$5\\text{ N}$$"), "$$10\\text{ N}$$", "Use $$F=ma$$");

        StepByStepBreakdownDto breakdownDto = new StepByStepBreakdownDto(
                "Solve for $$F$$ when $$m = 5\\text{ kg}$$ and $$a = 2\\text{ m/s}^2$$.",
                List.of("Step A with $$\\Delta p$$", "Step B with $$\\Delta t$$"),
                "Final answer is $$F = 10\\text{ N}$$",
                List.of(stepItem),
                trapAlert,
                List.of(formula),
                List.of("Key takeaway with $$\\sum F = 0$$"),
                List.of(quickCheck)
        );

        String breakdownJson = objectMapper.writeValueAsString(breakdownDto);
        List<String> cheatSheet = List.of(
                "Newton's Second Law: $$F = ma$$",
                "Work Energy Theorem: $$W = \\Delta K$$",
                "Pure formula: $$\\int_{0}^{t} F \\, dt$$"
        );
        String cheatSheetJson = objectMapper.writeValueAsString(cheatSheet);

        SubtopicMicroLesson lesson = SubtopicMicroLesson.builder()
                .subjectId(1L)
                .topicId(2L)
                .subtopicId(3L)
                .subject("Physics")
                .topic("Mechanics")
                .subtopic("Newton's Laws")
                .targetExam("JEE_MAIN")
                .coreConcept("Force is product of mass and acceleration: $$F = ma$$.")
                .trap("Neglecting vectors in $$\\vec{F}_{\\text{net}} = m\\vec{a}$$.")
                .cheatSheetJson(cheatSheetJson)
                .stepByStepBreakdownJson(breakdownJson)
                .contentVersion(2)
                .build();
        ReflectionTestUtils.setField(lesson, "id", UUID.randomUUID());

        // Mock IndicTrans2 returning translated strings with simulated Hindi translations and preserved LaTeX
        when(indicTrans2Service.translateBatch(anyList(), eq("hi"))).thenAnswer(invocation -> {
            List<String> input = invocation.getArgument(0);
            List<String> translated = new ArrayList<>();
            for (String s : input) {
                // Simulate Hindi translation with preserved LaTeX
                translated.add("अनुवाद: " + s);
            }
            return translated;
        });

        MicroLessonTranslationPayloadService.TranslatedMicroLessonPayload result =
                payloadService.translateMicroLesson(lesson, "hi");

        assertThat(result).isNotNull();
        assertThat(result.coreConcept()).startsWith("अनुवाद: Force is product of mass and acceleration: $$F = ma$$.");
        assertThat(result.trap()).startsWith("अनुवाद: Neglecting vectors in $$\\vec{F}_{\\text{net}} = m\\vec{a}$$.");

        // Verify cheat sheet
        assertThat(result.cheatSheetList()).hasSize(3);
        assertThat(result.cheatSheetList().get(0)).contains("$$F = ma$$");
        assertThat(result.cheatSheetList().get(1)).contains("$$W = \\Delta K$$");

        // Verify StepByStepBreakdown
        StepByStepBreakdownDto translatedBreakdown = result.breakdownDto();
        assertThat(translatedBreakdown).isNotNull();
        assertThat(translatedBreakdown.questionStem()).contains("$$F$$");
        assertThat(translatedBreakdown.finalAnswer()).contains("$$F = 10\\text{ N}$$");

        // Verify structured steps: stepNumber and formula (LaTeX) must be strictly untouched!
        assertThat(translatedBreakdown.structuredSteps()).hasSize(1);
        StepItemDto transStep = translatedBreakdown.structuredSteps().get(0);
        assertThat(transStep.stepNumber()).isEqualTo(1);
        assertThat(transStep.formula()).isEqualTo("\\vec{F} = m\\vec{a}");
        assertThat(transStep.title()).startsWith("अनुवाद:");

        // Verify structured formulas: latex must remain intact
        assertThat(translatedBreakdown.structuredFormulas()).hasSize(1);
        FormulaEntryDto transFormula = translatedBreakdown.structuredFormulas().get(0);
        assertThat(transFormula.latex()).isEqualTo("F = ma");
        assertThat(transFormula.name()).startsWith("अनुवाद:");

        // Verify structured trap alert
        assertThat(translatedBreakdown.structuredTrapAlert()).isNotNull();
        assertThat(translatedBreakdown.structuredTrapAlert().title()).startsWith("अनुवाद:");
        assertThat(translatedBreakdown.structuredTrapAlert().commonMistake()).contains("$$v = u + at$$");

        // Verify quick check
        assertThat(translatedBreakdown.quickCheckItems()).hasSize(1);
        assertThat(translatedBreakdown.quickCheckItems().get(0).question()).contains("$$F$$");
    }

    @Test
    @DisplayName("Should clean stray underscores introduced by translation token delimiters")
    void shouldCleanStrayUnderscoresInTranslations() {
        SubtopicMicroLesson lesson = SubtopicMicroLesson.builder()
                .coreConcept("Calculate _$$F = ma$$_ force")
                .trap("Watch out for _$$a = 0$$_ condition")
                .cheatSheetJson("[]")
                .stepByStepBreakdownJson("{}")
                .build();
        ReflectionTestUtils.setField(lesson, "id", UUID.randomUUID());

        when(indicTrans2Service.translateBatch(anyList(), eq("hi"))).thenAnswer(invocation -> {
            List<String> input = invocation.getArgument(0);
            return input.stream().map(s -> "अनुवाद _$$F = ma$$_ परीक्षण").toList();
        });

        MicroLessonTranslationPayloadService.TranslatedMicroLessonPayload result =
                payloadService.translateMicroLesson(lesson, "hi");

        assertThat(result.coreConcept()).doesNotContain("_$$");
        assertThat(result.coreConcept()).contains("$$F = ma$$");
    }

    @Test
    @DisplayName("Should handle null or empty breakdown and cheat sheet JSON gracefully")
    void shouldHandleNullAndEmptyBreakdownGracefully() {
        SubtopicMicroLesson lesson = SubtopicMicroLesson.builder()
                .coreConcept("Core concept")
                .trap("Common trap")
                .cheatSheetJson(null)
                .stepByStepBreakdownJson(null)
                .build();
        ReflectionTestUtils.setField(lesson, "id", UUID.randomUUID());

        when(indicTrans2Service.translateBatch(anyList(), eq("hi"))).thenAnswer(invocation -> invocation.getArgument(0));

        MicroLessonTranslationPayloadService.TranslatedMicroLessonPayload result =
                payloadService.translateMicroLesson(lesson, "hi");

        assertThat(result).isNotNull();
        assertThat(result.cheatSheetJson()).isEqualTo("[]");
        assertThat(result.stepByStepBreakdownJson()).isEqualTo("{}");
    }
}
