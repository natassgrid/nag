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
import com.examplatform.questionbank.translation.dto.AutoTranslateResponse;
import com.examplatform.questionbank.translation.dto.TranslatedOptionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service to perform machine translation of questions and options using
 * the local IndicTrans2 AI model (supporting 22 Indian scheduled languages).
 * Preserves all LaTeX, KaTeX formulas, symbols, and chemical/physical expressions.
 */
@Slf4j
@Service
public class IndicTrans2Service {

    private final RestClient restClient;
    private final QuestionRepository questionRepository;

    private static final Map<String, String> LANG_CODE_MAP = Map.ofEntries(
            Map.entry("hi", "hin_Deva"),
            Map.entry("hin_deva", "hin_Deva"),
            Map.entry("bn", "ben_Beng"),
            Map.entry("ben_beng", "ben_Beng"),
            Map.entry("te", "tel_Telu"),
            Map.entry("tel_telu", "tel_Telu"),
            Map.entry("mr", "mar_Deva"),
            Map.entry("mar_deva", "mar_Deva"),
            Map.entry("ta", "tam_Taml"),
            Map.entry("tam_taml", "tam_Taml"),
            Map.entry("ur", "urd_Arab"),
            Map.entry("urd_arab", "urd_Arab"),
            Map.entry("gu", "guj_Gujr"),
            Map.entry("guj_gujr", "guj_Gujr"),
            Map.entry("kn", "kan_Knda"),
            Map.entry("kan_knda", "kan_Knda"),
            Map.entry("ml", "mal_Mlym"),
            Map.entry("mal_mlym", "mal_Mlym"),
            Map.entry("or", "ory_Orya"),
            Map.entry("ory_orya", "ory_Orya"),
            Map.entry("pa", "pan_Guru"),
            Map.entry("pan_guru", "pan_Guru"),
            Map.entry("as", "asm_Beng"),
            Map.entry("asm_beng", "asm_Beng"),
            Map.entry("mai", "mai_Deva"),
            Map.entry("mai_deva", "mai_Deva"),
            Map.entry("sa", "san_Deva"),
            Map.entry("san_deva", "san_Deva"),
            Map.entry("sd", "snd_Arab"),
            Map.entry("snd_arab", "snd_Arab"),
            Map.entry("ne", "npi_Deva"),
            Map.entry("npi_deva", "npi_Deva"),
            Map.entry("kok", "gom_Deva"),
            Map.entry("gom_deva", "gom_Deva"),
            Map.entry("doi", "doi_Deva"),
            Map.entry("doi_deva", "doi_Deva"),
            Map.entry("mni", "mni_Beng"),
            Map.entry("mni_beng", "mni_Beng"),
            Map.entry("sat", "sat_Olck"),
            Map.entry("sat_olck", "sat_Olck"),
            Map.entry("bo", "brx_Deva"),
            Map.entry("brx_deva", "brx_Deva"),
            Map.entry("kas", "kas_Deva"),
            Map.entry("kas_deva", "kas_Deva")
    );

    public IndicTrans2Service(
            @Value("${indictrans2.url:http://localhost:7860}") String indictrans2Url,
            QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
        this.restClient = RestClient.builder()
                .baseUrl(indictrans2Url)
                .build();
    }

    public String mapToIndicLang(String code) {
        if (code == null || code.isBlank()) {
            return "hin_Deva";
        }
        String mapped = LANG_CODE_MAP.get(code.toLowerCase());
        return mapped != null ? mapped : code;
    }

    @SuppressWarnings("unchecked")
    public String translateSingleText(String text, String targetLang) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String indicLang = mapToIndicLang(targetLang);
        LatexPreservationUtil.MaskResult maskResult = LatexPreservationUtil.mask(text);
        String textToTranslate = maskResult.maskedText();

        try {
            Map<String, Object> reqBody = Map.of(
                    "text", textToTranslate,
                    "source_lang", "eng_Latn",
                    "target_lang", indicLang
            );
            Map<String, Object> resp = restClient.post()
                    .uri("/translate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(reqBody)
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.containsKey("translated_text")) {
                String rawTranslated = (String) resp.get("translated_text");
                return LatexPreservationUtil.unmask(rawTranslated, maskResult.preservedTokens());
            }
        } catch (Exception e) {
            log.error("Failed to translate text via IndicTrans2: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "IndicTrans2 translation service unavailable: " + e.getMessage());
        }
        return text;
    }

    @SuppressWarnings("unchecked")
    public List<String> translateBatch(List<String> texts, String targetLang) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }
        String indicLang = mapToIndicLang(targetLang);
        List<LatexPreservationUtil.MaskResult> maskResults = new ArrayList<>(texts.size());
        List<String> maskedTexts = new ArrayList<>(texts.size());

        for (String t : texts) {
            LatexPreservationUtil.MaskResult mr = LatexPreservationUtil.mask(t);
            maskResults.add(mr);
            maskedTexts.add(mr.maskedText());
        }

        try {
            Map<String, Object> reqBody = Map.of(
                    "texts", maskedTexts,
                    "target_lang", indicLang
            );
            Map<String, Object> resp = restClient.post()
                    .uri("/translate/batch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(reqBody)
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.containsKey("translations")) {
                Object transObj = resp.get("translations");
                if (transObj instanceof List) {
                    List<String> rawList = (List<String>) transObj;
                    List<String> unmaskedList = new ArrayList<>(rawList.size());
                    for (int i = 0; i < rawList.size(); i++) {
                        String raw = rawList.get(i);
                        List<String> tokens = (i < maskResults.size()) ? maskResults.get(i).preservedTokens() : List.of();
                        unmaskedList.add(LatexPreservationUtil.unmask(raw, tokens));
                    }
                    return unmaskedList;
                }
            }
        } catch (Exception e) {
            log.warn("Batch translation endpoint failed, falling back to sequential: {}", e.getMessage());
            List<String> fallbackResults = new ArrayList<>();
            for (String t : texts) {
                fallbackResults.add(translateSingleText(t, indicLang));
            }
            return fallbackResults;
        }
        return texts;
    }

    public AutoTranslateResponse autoTranslateQuestion(UUID questionId, String languageCode) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found: " + questionId));
        return autoTranslateQuestionEntity(question, languageCode);
    }

    public AutoTranslateResponse autoTranslateQuestionEntity(Question question, String languageCode) {
        String indicLang = mapToIndicLang(languageCode);

        List<String> textsToTranslate = new ArrayList<>();
        textsToTranslate.add(question.getContent() != null ? question.getContent() : "");

        int optionCount = 0;
        if (question.getOptions() != null) {
            for (QuestionOption opt : question.getOptions()) {
                textsToTranslate.add(opt.getText() != null ? opt.getText() : "");
                optionCount++;
            }
        }

        boolean hasExplanation = question.getExplanation() != null && !question.getExplanation().isBlank();
        if (hasExplanation) {
            textsToTranslate.add(question.getExplanation());
        }

        List<String> translatedList = translateBatch(textsToTranslate, indicLang);

        String translatedContent = translatedList.isEmpty() ? question.getContent() : translatedList.get(0);

        List<TranslatedOptionDto> translatedOptions = new ArrayList<>();
        if (question.getOptions() != null) {
            for (int i = 0; i < optionCount; i++) {
                QuestionOption originalOpt = question.getOptions().get(i);
                String transOptText = (i + 1 < translatedList.size()) ? translatedList.get(i + 1) : originalOpt.getText();
                translatedOptions.add(new TranslatedOptionDto(
                        originalOpt.getId(),
                        transOptText
                ));
            }
        }

        String translatedExplanation = null;
        if (hasExplanation) {
            int explIndex = 1 + optionCount;
            if (explIndex < translatedList.size()) {
                translatedExplanation = translatedList.get(explIndex);
            }
        }

        return AutoTranslateResponse.builder()
                .questionId(question.getId())
                .languageCode(languageCode)
                .translatedContent(translatedContent)
                .translatedOptions(translatedOptions)
                .translatedExplanation(translatedExplanation)
                .build();
    }
}
