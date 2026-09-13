/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.questionbank.translation.service;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.translation.dto.AutoTranslateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndicTrans2ServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    private IndicTrans2Service indicTrans2Service;

    @BeforeEach
    void setUp() {
        indicTrans2Service = spy(new IndicTrans2Service("http://localhost:7860", questionRepository));
    }

    @Test
    @DisplayName("Maps language codes correctly to IndicTrans2 language tokens")
    void testLanguageCodeMapping() {
        assertEquals("hin_Deva", indicTrans2Service.mapToIndicLang("hi"));
        assertEquals("hin_Deva", indicTrans2Service.mapToIndicLang("HIN_DEVA"));
        assertEquals("ben_Beng", indicTrans2Service.mapToIndicLang("bn"));
        assertEquals("tam_Taml", indicTrans2Service.mapToIndicLang("ta"));
        assertEquals("tel_Telu", indicTrans2Service.mapToIndicLang("te"));
        assertEquals("hin_Deva", indicTrans2Service.mapToIndicLang(null));
        assertEquals("hin_Deva", indicTrans2Service.mapToIndicLang(""));
    }

    @Test
    @DisplayName("autoTranslateQuestionEntity preserves LaTeX formulas across question, options, and explanation")
    void testAutoTranslateQuestionEntityWithLatex() {
        UUID qId = UUID.randomUUID();
        Question question = Question.builder()
                .content("Solve $$x^2 + y^2 = 25$$ for $x = 3$.")
                .options(List.of(
                        new QuestionOption("opt1", "$y = \\pm 4$", true),
                        new QuestionOption("opt2", "$y = \\pm 5$", false)
                ))
                .explanation("Substitute $x = 3$ into $$x^2 + y^2 = 25$$ to get $y^2 = 16$.")
                .build();
        ReflectionTestUtils.setField(question, "id", qId);

        // Mock translateBatch returning simulated translations with placeholders intact
        doReturn(List.of(
                "हल करें $$x^2 + y^2 = 25$$ $x = 3$ के लिए।",
                "$y = \\pm 4$",
                "$y = \\pm 5$",
                "प्रतिकल्पित करें $x = 3$ को $$x^2 + y^2 = 25$$ में ताकि $y^2 = 16$ प्राप्त हो।"
        )).when(indicTrans2Service).translateBatch(anyList(), eq("hin_Deva"));

        AutoTranslateResponse response = indicTrans2Service.autoTranslateQuestionEntity(question, "hi");

        assertNotNull(response);
        assertEquals(question.getId(), response.getQuestionId());
        assertEquals("hi", response.getLanguageCode());
        assertTrue(response.getTranslatedContent().contains("$$x^2 + y^2 = 25$$"));
        assertTrue(response.getTranslatedContent().contains("$x = 3$"));

        assertEquals(2, response.getTranslatedOptions().size());
        assertEquals("$y = \\pm 4$", response.getTranslatedOptions().get(0).text());
        assertEquals("$y = \\pm 5$", response.getTranslatedOptions().get(1).text());

        assertNotNull(response.getTranslatedExplanation());
        assertTrue(response.getTranslatedExplanation().contains("$$x^2 + y^2 = 25$$"));
        assertTrue(response.getTranslatedExplanation().contains("$y^2 = 16$"));
    }
}
