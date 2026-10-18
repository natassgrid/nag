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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates and sanitizes prompt input fields against prompt injection, jailbreaks,
 * and boundary breakout attacks (OWASP Top 10 for LLM Applications: LLM01).
 */
@Component
public class InputGuardrail {

    private static final Logger log = LoggerFactory.getLogger(InputGuardrail.class);

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            // OWASP LLM01: Direct Prompt Injection - instruction overrides
            Pattern.compile("(?i)\\bignore\\s+(?:all\\s+|any\\s+)?(?:previous|prior|above|system)\\s+(?:instructions|prompts|rules|directives)\\b"),
            Pattern.compile("(?i)\\bdisregard\\s+(?:all\\s+|any\\s+)?(?:previous|prior|above|system)\\s+(?:instructions|prompts|rules|directives)\\b"),
            Pattern.compile("(?i)\\bforget\\s+(?:all\\s+|any\\s+)?(?:previous|prior|above|system)\\s+(?:instructions|prompts|rules|directives)\\b"),
            // OWASP LLM01: Jailbreak personas & developer mode triggers
            Pattern.compile("(?i)\\b(?:system\\s+override|developer\\s+mode|jailbreak|act\\s+as\\s+dan|do\\s+anything\\s+now)\\b"),
            // OWASP LLM07: System Prompt Extraction / Leakage extraction attempts
            Pattern.compile("(?i)\\b(?:reveal|print|show|output|repeat|dump)\\s+(?:your\\s+|the\\s+)?(?:(?:system|initial|hidden|secret|internal)\\s+)+(?:prompt|instructions|rules)\\b"),
            // OWASP LLM02/LLM09: XSS / Script injection
            Pattern.compile("(?i)(?:<\\s*script\\b|javascript\\s*:|onerror\\s*=|onload\\s*=)"),
            // Structural XML boundary breakout tags
            Pattern.compile("(?i)</?\\s*(?:system_prompt|user_input|subject|topic|subtopic|reference_context)\\s*>"),
            // LLM delimiter token injections (ChatML, Llama, Mistral)
            Pattern.compile("(?i)(?:<\\|im_start\\|>|<\\|im_end\\|>|\\[INST\\]|\\[/INST\\]|<<SYS>>)")
    );

    @Value("${app.ai.guardrails.enabled:${AI_GUARDRAILS_ENABLED:true}}")
    private boolean enabled = true;

    @Value("${app.ai.guardrails.strict-mode:${AI_GUARDRAIL_STRICT_MODE:true}}")
    private boolean strictMode = true;

    @Value("${app.ai.guardrails.max-input-length:${AI_MAX_PROMPT_INPUT_LENGTH:4000}}")
    private int maxInputLength = 4000;

    public InputGuardrail() {}

    public InputGuardrail(boolean enabled, boolean strictMode, int maxInputLength) {
        this.enabled = enabled;
        this.strictMode = strictMode;
        this.maxInputLength = maxInputLength;
    }

    /**
     * Validates and sanitizes a prompt input field.
     *
     * @param fieldName logical name of the field being checked (for error diagnostics)
     * @param input     user or system-provided text
     * @return sanitized string
     * @throws AiSecurityException if prompt injection or structural breakout is detected
     */
    public String validateAndSanitize(String fieldName, String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        if (!enabled) {
            return input;
        }

        // Sanitize control characters (preserve \t, \n, \r)
        String sanitized = input.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

        // Enforce maximum length limit (OWASP LLM04: Resource Exhaustion / Denial of Service)
        if (sanitized.length() > maxInputLength) {
            if (strictMode) {
                log.warn("AI input guardrail rejected input for field '{}': length {} exceeds limit {}",
                        fieldName, sanitized.length(), maxInputLength);
                throw new AiSecurityException(
                        "Input field '" + fieldName + "' exceeds maximum allowed length of " + maxInputLength + " characters");
            }
            log.info("AI input guardrail truncated field '{}' from {} to {} characters",
                    fieldName, sanitized.length(), maxInputLength);
            sanitized = sanitized.substring(0, maxInputLength);
        }

        // Detect prompt injection, jailbreaks, and token breakout patterns (OWASP LLM01)
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(sanitized).find()) {
                log.warn("AI prompt injection attempt detected in field '{}': pattern [{}]",
                        fieldName, pattern.pattern());
                throw new AiSecurityException(
                        "AI security violation: Potential prompt injection or forbidden pattern detected in field '"
                                + fieldName + "': matched [" + pattern.pattern() + "]");
            }
        }

        return sanitized;
    }

    /**
     * Wraps sanitized user content inside an explicit XML boundary tag to mitigate boundary confusion.
     *
     * @param tagName        XML tag name (e.g. "subject", "subtopic", "reference_context")
     * @param sanitizedValue sanitized input value
     * @return wrapped boundary string
     */
    public String wrapInBoundary(String tagName, String sanitizedValue) {
        return "<" + tagName + ">" + (sanitizedValue != null ? sanitizedValue : "") + "</" + tagName + ">";
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

    public int getMaxInputLength() {
        return maxInputLength;
    }

    public void setMaxInputLength(int maxInputLength) {
        this.maxInputLength = maxInputLength;
    }
}
