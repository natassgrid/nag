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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service to perform machine translation of questions and options using
 * the local IndicTrans2 AI model (supporting 22 Indian scheduled languages).
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
        try {
            Map<String, Object> reqBody = Map.of(
                    "text", text,
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
                return (String) resp.get("translated_text");
            }
        } catch (Exception e) {
            log.error("Failed to translate text via IndicTrans2: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "IndicTrans2 translation service unavailable: " + e.getMessage());
        }
        return text;
    }

    public AutoTranslateResponse autoTranslateQuestion(UUID questionId, String languageCode) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found: " + questionId));

        String indicLang = mapToIndicLang(languageCode);

        // 1. Translate question content
        String translatedContent = translateSingleText(question.getContent(), indicLang);

        // 2. Translate options
        List<TranslatedOptionDto> translatedOptions = new ArrayList<>();
        if (question.getOptions() != null) {
            for (QuestionOption opt : question.getOptions()) {
                String transOptText = translateSingleText(opt.getText(), indicLang);
                translatedOptions.add(new TranslatedOptionDto(opt.getId(), transOptText));
            }
        }

        // 3. Translate explanation if present
        String translatedExplanation = null;
        if (question.getExplanation() != null && !question.getExplanation().isBlank()) {
            translatedExplanation = translateSingleText(question.getExplanation(), indicLang);
        }

        return AutoTranslateResponse.builder()
                .questionId(questionId)
                .languageCode(languageCode)
                .targetLangIndicTrans(indicLang)
                .translatedContent(translatedContent)
                .translatedOptions(translatedOptions)
                .translatedExplanation(translatedExplanation)
                .model("ai4bharat/indictrans2-en-indic-dist-200M")
                .build();
    }
}
