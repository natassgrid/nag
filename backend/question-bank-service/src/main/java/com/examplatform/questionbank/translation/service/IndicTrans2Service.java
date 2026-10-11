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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

    private static final Map<String, String> BASE_LANG_CODES = Map.ofEntries(
            Map.entry("hi", "hin_Deva"),
            Map.entry("bn", "ben_Beng"),
            Map.entry("te", "tel_Telu"),
            Map.entry("mr", "mar_Deva"),
            Map.entry("ta", "tam_Taml"),
            Map.entry("ur", "urd_Arab"),
            Map.entry("gu", "guj_Gujr"),
            Map.entry("kn", "kan_Knda"),
            Map.entry("ml", "mal_Mlym"),
            Map.entry("or", "ory_Orya"),
            Map.entry("pa", "pan_Guru"),
            Map.entry("as", "asm_Beng"),
            Map.entry("mai", "mai_Deva"),
            Map.entry("sa", "san_Deva"),
            Map.entry("sd", "snd_Arab"),
            Map.entry("ne", "npi_Deva"),
            Map.entry("kok", "gom_Deva"),
            Map.entry("doi", "doi_Deva"),
            Map.entry("mni", "mni_Beng"),
            Map.entry("sat", "sat_Olck"),
            Map.entry("bo", "brx_Deva"),
            Map.entry("kas", "kas_Deva")
    );

    private static final Map<String, String> LANG_CODE_MAP = buildLanguageCodeMap();

    private static Map<String, String> buildLanguageCodeMap() {
        Map<String, String> map = new HashMap<>(BASE_LANG_CODES);
        for (String targetCode : BASE_LANG_CODES.values()) {
            map.put(targetCode.toLowerCase(Locale.ROOT), targetCode);
        }
        return Collections.unmodifiableMap(map);
    }

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
            List<String> unmaskedList = executeBatchRequest(maskedTexts, indicLang, maskResults);
            if (unmaskedList != null) {
                return unmaskedList;
            }
        } catch (Exception e) {
            log.warn("Batch translation endpoint failed, falling back to sequential: {}", e.getMessage());
            return fallbackSequentialTranslation(texts, indicLang);
        }
        return texts;
    }

    @SuppressWarnings("unchecked")
    private List<String> executeBatchRequest(
            List<String> maskedTexts,
            String indicLang,
            List<LatexPreservationUtil.MaskResult> maskResults) {
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

        if (resp != null && resp.get("translations") instanceof List<?> rawList) {
            return unmaskTranslations((List<String>) rawList, maskResults);
        }
        return null;
    }

    private List<String> unmaskTranslations(
            List<String> rawList,
            List<LatexPreservationUtil.MaskResult> maskResults) {
        List<String> unmaskedList = new ArrayList<>(rawList.size());
        for (int i = 0; i < rawList.size(); i++) {
            String raw = rawList.get(i);
            List<String> tokens = (i < maskResults.size()) ? maskResults.get(i).preservedTokens() : List.of();
            unmaskedList.add(LatexPreservationUtil.unmask(raw, tokens));
        }
        return unmaskedList;
    }

    private List<String> fallbackSequentialTranslation(List<String> texts, String indicLang) {
        List<String> fallbackResults = new ArrayList<>(texts.size());
        for (String t : texts) {
            fallbackResults.add(translateSingleText(t, indicLang));
        }
        return fallbackResults;
    }

    public AutoTranslateResponse autoTranslateQuestion(UUID questionId, String languageCode) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found: " + questionId));
        return autoTranslateQuestionEntity(question, languageCode);
    }

    public AutoTranslateResponse autoTranslateQuestionEntity(Question question, String languageCode) {
        String indicLang = mapToIndicLang(languageCode);
        TranslationPayload payload = preparePayload(question);

        List<String> translatedList = translateBatch(payload.texts(), indicLang);
        String translatedContent = translatedList.isEmpty() ? question.getContent() : translatedList.get(0);

        int[] cursor = new int[]{1};
        List<TranslatedOptionDto> translatedOptions = reconstructOptions(
                question.getOptions(),
                payload.optionHasAlt(),
                translatedList,
                cursor
        );

        String translatedExplanation = null;
        if (payload.hasExplanation() && cursor[0] < translatedList.size()) {
            translatedExplanation = translatedList.get(cursor[0]);
        }

        return AutoTranslateResponse.builder()
                .questionId(question.getId())
                .languageCode(languageCode)
                .translatedContent(translatedContent)
                .translatedOptions(translatedOptions)
                .translatedExplanation(translatedExplanation)
                .build();
    }

    private TranslationPayload preparePayload(Question question) {
        List<String> texts = new ArrayList<>();
        texts.add(question.getContent() != null ? question.getContent() : "");

        List<Boolean> optionHasAlt = new ArrayList<>();
        collectOptionTexts(question.getOptions(), texts, optionHasAlt);

        boolean hasExplanation = question.getExplanation() != null && !question.getExplanation().isBlank();
        if (hasExplanation) {
            texts.add(question.getExplanation());
        }

        return new TranslationPayload(texts, optionHasAlt, hasExplanation);
    }

    private void collectOptionTexts(
            List<QuestionOption> options,
            List<String> texts,
            List<Boolean> optionHasAlt) {
        if (options == null) {
            return;
        }
        for (QuestionOption opt : options) {
            texts.add(opt.getText() != null ? opt.getText() : "");
            boolean hasAlt = hasNonBlankAltText(opt);
            optionHasAlt.add(hasAlt);
            if (hasAlt) {
                texts.add(opt.getImageAltText());
            }
        }
    }

    private static boolean hasNonBlankAltText(QuestionOption opt) {
        return opt.getImageAltText() != null && !opt.getImageAltText().isBlank();
    }

    private List<TranslatedOptionDto> reconstructOptions(
            List<QuestionOption> options,
            List<Boolean> optionHasAlt,
            List<String> translatedList,
            int[] cursor) {
        if (options == null) {
            return Collections.emptyList();
        }
        List<TranslatedOptionDto> result = new ArrayList<>(options.size());
        for (int i = 0; i < options.size(); i++) {
            QuestionOption opt = options.get(i);
            String transText = getNextText(translatedList, cursor, opt.getText());
            String transAlt = opt.getImageAltText();
            if (Boolean.TRUE.equals(optionHasAlt.get(i))) {
                transAlt = getNextText(translatedList, cursor, transAlt);
            }
            result.add(new TranslatedOptionDto(opt.getId(), transText, opt.getImageUrl(), transAlt));
        }
        return result;
    }

    private String getNextText(List<String> translatedList, int[] cursor, String fallback) {
        int idx = cursor[0]++;
        return (idx < translatedList.size()) ? translatedList.get(idx) : fallback;
    }

    private record TranslationPayload(
            List<String> texts,
            List<Boolean> optionHasAlt,
            boolean hasExplanation
    ) {}
}
