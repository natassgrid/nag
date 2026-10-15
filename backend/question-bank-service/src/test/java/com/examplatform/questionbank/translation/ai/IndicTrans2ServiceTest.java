/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 */

package com.examplatform.questionbank.translation.ai;

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
        assertEquals("mar_Deva", indicTrans2Service.mapToIndicLang("mr"));
        assertEquals("guj_Gujr", indicTrans2Service.mapToIndicLang("gu"));
        assertEquals("kan_Knda", indicTrans2Service.mapToIndicLang("kn"));
        assertEquals("mal_Mlym", indicTrans2Service.mapToIndicLang("ml"));
        assertEquals("pan_Guru", indicTrans2Service.mapToIndicLang("pa"));
        assertEquals("urd_Arab", indicTrans2Service.mapToIndicLang("ur"));
        assertEquals("ory_Orya", indicTrans2Service.mapToIndicLang("or"));
        assertEquals("asm_Beng", indicTrans2Service.mapToIndicLang("as"));
        assertEquals("san_Deva", indicTrans2Service.mapToIndicLang("sa"));
        assertEquals("hin_Deva", indicTrans2Service.mapToIndicLang(null));
        assertEquals("custom_lang", indicTrans2Service.mapToIndicLang("custom_lang"));
    }

    @Test
    @DisplayName("Auto-translates question entity and preserves options with correct matching")
    void testAutoTranslateQuestionEntity() {
        UUID qId = UUID.randomUUID();
        QuestionOption opt1 = new QuestionOption();
        opt1.setId(UUID.randomUUID().toString());
        opt1.setText("Option 1");

        QuestionOption opt2 = new QuestionOption();
        opt2.setId(UUID.randomUUID().toString());
        opt2.setText("Option 2");

        Question question = new Question();
        ReflectionTestUtils.setField(question, "id", qId);
        question.setContent("What is Newton's second law?");
        question.setExplanation("Force equals mass times acceleration.");
        question.setOptions(List.of(opt1, opt2));

        doReturn(List.of(
                "न्यूटन का दूसरा नियम क्या है?",
                "विकल्प १",
                "विकल्प २",
                "बल द्रव्यमान और त्वरण के गुणनफल के बराबर होता है।"
        )).when(indicTrans2Service).translateBatch(any(), eq("hin_Deva"));

        AutoTranslateResponse response = indicTrans2Service.autoTranslateQuestionEntity(question, "hi");

        assertNotNull(response);
        assertEquals(qId, response.getQuestionId());
        assertEquals("hi", response.getLanguageCode());
        assertEquals("न्यूटन का दूसरा नियम क्या है?", response.getTranslatedContent());
        assertEquals(2, response.getTranslatedOptions().size());
        assertEquals("विकल्प १", response.getTranslatedOptions().get(0).text());
        assertEquals("विकल्प २", response.getTranslatedOptions().get(1).text());
        assertEquals("बल द्रव्यमान और त्वरण के गुणनफल के बराबर होता है।", response.getTranslatedExplanation());
    }

    @Test
    @DisplayName("Handles questions with image alt text translation")
    void testAutoTranslateWithImageAltText() {
        UUID qId = UUID.randomUUID();
        QuestionOption opt1 = new QuestionOption();
        opt1.setId(UUID.randomUUID().toString());
        opt1.setText("Diagram");
        opt1.setImageUrl("https://example.com/img.png");
        opt1.setImageAltText("Circuit diagram");

        Question question = new Question();
        ReflectionTestUtils.setField(question, "id", qId);
        question.setContent("Identify the circuit.");
        question.setOptions(List.of(opt1));

        doReturn(List.of(
                "परिपथ की पहचान करें।",
                "आरेख",
                "परिपथ आरेख"
        )).when(indicTrans2Service).translateBatch(any(), eq("hin_Deva"));

        AutoTranslateResponse response = indicTrans2Service.autoTranslateQuestionEntity(question, "hi");

        assertNotNull(response);
        assertEquals(1, response.getTranslatedOptions().size());
        assertEquals("आरेख", response.getTranslatedOptions().get(0).text());
        assertEquals("परिपथ आरेख", response.getTranslatedOptions().get(0).imageAltText());
    }
}
