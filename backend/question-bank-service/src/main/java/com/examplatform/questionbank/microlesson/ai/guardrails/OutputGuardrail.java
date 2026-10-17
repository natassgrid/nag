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

package com.examplatform.questionbank.microlesson.ai.guardrails;

import com.examplatform.questionbank.exception.AiSecurityException;
import com.examplatform.questionbank.microlesson.dto.MicroLessonContentDto;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates AI generation outputs against sensitive information disclosure (OWASP LLM02),
 * system prompt leakage (OWASP LLM07), and malicious script/XSS payloads (OWASP LLM02/LLM09).
 */
@Component
public class OutputGuardrail {

    private static final Logger log = LoggerFactory.getLogger(OutputGuardrail.class);

    private static final List<String> SYSTEM_PROMPT_MARKERS = List.of(
            "CRITICAL JSON & LATEX ESCAPING RULES",
            "Never output conversational filler before or after the JSON",
            "You are an elite, master-level Indian competitive exam faculty",
            "You are an expert master faculty and pedagogical content designer",
            "CRITICAL INSTRUCTION:",
            "Output must be strictly raw, valid JSON only",
            "The output must match this exact schema:"
    );

    private static final List<Pattern> SENSITIVE_INFO_PATTERNS = List.of(
            Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b"), // AWS Access Key ID
            Pattern.compile("\\bsk-[a-zA-Z0-9_-]{20,}\\b"), // OpenAI Secret API Key
            Pattern.compile("\\bAIza[0-9A-Za-z_-]{35}\\b"), // Google API Key
            Pattern.compile("-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"), // Cryptographic Private Key
            Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"), // Social Security Number
            Pattern.compile("at\\s+[a-zA-Z0-9_.]+\\([A-Za-z0-9_]+\\.java:\\d+\\)"), // Java Stack Trace Dump
            Pattern.compile("Exception in thread \"") // Thread dump marker
    );

    private static final List<Pattern> XSS_PAYLOAD_PATTERNS = List.of(
            Pattern.compile("(?i)<\\s*script\\b"),
            Pattern.compile("(?i)javascript\\s*:"),
            Pattern.compile("(?i)onerror\\s*="),
            Pattern.compile("(?i)onload\\s*=")
    );

    @Value("${app.ai.guardrails.enabled:${AI_GUARDRAILS_ENABLED:true}}")
    private boolean enabled = true;

    @Value("${app.ai.guardrails.strict-mode:${AI_GUARDRAIL_STRICT_MODE:true}}")
    private boolean strictMode = true;

    public OutputGuardrail() {}

    public OutputGuardrail(boolean enabled, boolean strictMode) {
        this.enabled = enabled;
        this.strictMode = strictMode;
    }

    /**
     * Inspects raw LLM response text and parsed MicroLessonContentDto fields.
     *
     * @param content     parsed micro-lesson DTO
     * @param rawResponse raw response text received from LLM
     * @throws AiSecurityException if leakage or malicious content is detected
     */
    public void validateOutput(MicroLessonContentDto content, String rawResponse) {
        if (!enabled) {
            return;
        }

        List<String> snippetsToCheck = new ArrayList<>();
        if (rawResponse != null && !rawResponse.isBlank()) {
            snippetsToCheck.add(rawResponse);
        }

        if (content != null) {
            if (content.coreConcept() != null) snippetsToCheck.add(content.coreConcept());
            if (content.trap() != null) snippetsToCheck.add(content.trap());
            if (content.cheatSheet() != null) snippetsToCheck.addAll(content.cheatSheet());

            StepByStepBreakdownDto breakdown = content.stepByStepBreakdown();
            if (breakdown != null) {
                if (breakdown.questionStem() != null) snippetsToCheck.add(breakdown.questionStem());
                if (breakdown.steps() != null) snippetsToCheck.addAll(breakdown.steps());
                if (breakdown.finalAnswer() != null) snippetsToCheck.add(breakdown.finalAnswer());
                if (breakdown.keyTakeaways() != null) snippetsToCheck.addAll(breakdown.keyTakeaways());
            }
        }

        for (String text : snippetsToCheck) {
            if (text == null || text.isBlank()) {
                continue;
            }

            // 1. System Prompt Leakage (OWASP LLM07)
            for (String marker : SYSTEM_PROMPT_MARKERS) {
                if (text.toLowerCase().contains(marker.toLowerCase())) {
                    log.warn("AI output guardrail detected system prompt leakage marker: '{}'", marker);
                    throw new AiSecurityException(
                            "AI security violation: System prompt leakage detected in AI output matching marker: " + marker);
                }
            }

            // 2. Sensitive Information / Secret Key Disclosure (OWASP LLM02)
            for (Pattern pattern : SENSITIVE_INFO_PATTERNS) {
                if (pattern.matcher(text).find()) {
                    log.warn("AI output guardrail detected sensitive key or credential leakage: [{}]", pattern.pattern());
                    throw new AiSecurityException(
                            "AI security violation: Sensitive credential or secret key disclosure detected in AI output: "
                                    + pattern.pattern());
                }
            }

            // 3. Executable / XSS Payload Injection (OWASP LLM02/LLM09)
            for (Pattern pattern : XSS_PAYLOAD_PATTERNS) {
                if (pattern.matcher(text).find()) {
                    log.warn("AI output guardrail detected executable XSS payload in content: [{}]", pattern.pattern());
                    throw new AiSecurityException(
                            "AI security violation: Executable script or XSS payload detected in AI output: "
                                    + pattern.pattern());
                }
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isStrictMode() {
        return strictMode;
    }

    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }
}
