// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.ai.agent;

import com.examplatform.questionbank.microlesson.dto.MicroLessonContentDto;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeterministicJsonRepairEngine Unit Tests")
class DeterministicJsonRepairEngineTest {

    private DeterministicJsonRepairEngine repairEngine;
    private ObjectMapper aiObjectMapper;

    private static final char CH_FF = (char) 0x0C;
    private static final char CH_TAB = (char) 0x09;
    private static final char CH_BS = (char) 0x08;
    private static final char CH_CR = (char) 0x0D;
    private static final char CH_LF = (char) 0x0A;

    @BeforeEach
    void setUp() {
        repairEngine = new DeterministicJsonRepairEngine();
        aiObjectMapper = new ObjectMapper()
                .configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true)
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    @DisplayName("Strips markdown code fences and conversational preambles")
    void testStripMarkdownAndPreambles() {
        String responseWithMarkdown = """
                Here is the requested micro-lesson JSON:
                ```json
                {
                  "coreConcept": "Concept explanation.",
                  "trap": "Common pitfall."
                }
                ```
                I hope this helps your studies!
                """;

        String stripped = repairEngine.stripMarkdownAndPreambles(responseWithMarkdown);
        assertThat(stripped).startsWith("{").endsWith("}");
        assertThat(stripped).doesNotContain("Here is the requested");
        assertThat(stripped).doesNotContain("I hope this helps");
    }

    @Test
    @DisplayName("Removes StringTemplate leaked brace escapes in JSON boundaries and LaTeX formulas")
    void testCleanStringTemplateBraces() {
        String leakedJson = "\\{ \"coreConcept\": \"Formula \\\\frac\\{1\\}\\{100\\} calculation.\" \\}";
        String cleaned = repairEngine.cleanStringTemplateBraces(leakedJson);
        assertThat(cleaned).isEqualTo("{ \"coreConcept\": \"Formula \\\\frac{1}{100} calculation.\" }");
    }

    @Test
    @DisplayName("Repairs corrupted control characters collided with LaTeX commands")
    void testSanitizeCorruptedControlChars() {
        // String with corrupted control characters (form-feed, tab, backspace, carriage return, newline)
        String corrupted = "Formula: " + CH_FF + "rac{1}{2} and " + CH_TAB + "ext{m} and "
                + CH_TAB + "heta and " + CH_BS + "eta and " + CH_CR + "ightarrow and x " + CH_LF + "eq y";

        String sanitized = DeterministicJsonRepairEngine.sanitizeLatexJson(corrupted);
        assertThat(sanitized).contains("\\\\frac{1}{2}");
        assertThat(sanitized).contains("\\\\text{m}");
        assertThat(sanitized).contains("\\\\theta");
        assertThat(sanitized).contains("\\\\beta");
        assertThat(sanitized).contains("\\\\rightarrow");
        assertThat(sanitized).contains("\\\\neq y");
    }

    @Test
    @DisplayName("Double-escapes single backslashes in JSON strings while preserving valid JSON escapes")
    void testSanitizeSingleBackslashesInJson() {
        String rawJson = "{\"coreConcept\": \"Compute \\frac{a}{b} \\times 100\\% where \\alpha = 5 and Step 1:\\nNew line \\\"quoted\\\"\"}";

        String sanitized = DeterministicJsonRepairEngine.sanitizeLatexJson(rawJson);
        // \frac becomes \\frac
        assertThat(sanitized).contains("\\\\frac{a}{b}");
        // \times becomes \\times
        assertThat(sanitized).contains("\\\\times");
        // \% becomes \\%
        assertThat(sanitized).contains("\\\\%");
        // \alpha becomes \\alpha
        assertThat(sanitized).contains("\\\\alpha");
        // \" stays \"
        assertThat(sanitized).contains("\\\"quoted\\\"");
        // \n before New line stays \n
        assertThat(sanitized).contains("\\nNew line");
    }

    @Test
    @DisplayName("Repairs truncated JSON with prematurely closed or missing object markers")
    void testRepairTruncatedJsonMissingOuterBrace() {
        // Production bug case: LLM emitted closing brace for stepByStepBreakdown, but cut off before outer object closed
        String truncated = """
                {
                  "coreConcept": "Thermodynamics first law states internal energy change.",
                  "trap": "Confusing work done on system vs by system.",
                  "cheatSheet": ["$$\\Delta U = Q - W$$"],
                  "stepByStepBreakdown": {
                    "questionStem": "Calculate internal energy change.",
                    "steps": ["Step 1: State the first law", "Step 2: Substitute Q and W"],
                    "finalAnswer": "50 J"
                  }
                """;

        String repaired = repairEngine.repairTruncatedJson(truncated);
        assertThat(repaired.trim()).endsWith("}");

        // Must be parseable by Jackson
        MicroLessonContentDto dto = null;
        try {
            dto = aiObjectMapper.readValue(repaired, MicroLessonContentDto.class);
        } catch (Exception e) {
            org.junit.jupiter.api.Assertions.fail("Failed to parse repaired JSON: " + e.getMessage());
        }

        assertThat(dto).isNotNull();
        assertThat(dto.stepByStepBreakdown().finalAnswer()).isEqualTo("50 J");
    }

    @Test
    @DisplayName("Repairs JSON truncated mid-string and mid-array with trailing comma")
    void testRepairTruncatedMidArray() {
        String truncated = """
                {
                  "coreConcept": "Partial concept...",
                  "trap": "Trap note...",
                  "cheatSheet": [
                    "Formula 1",
                    "Formula 2",
                """;

        String repaired = repairEngine.repairTruncatedJson(truncated);
        assertThat(repaired).doesNotContain(",\n]");

        MicroLessonContentDto dto = null;
        try {
            dto = aiObjectMapper.readValue(repaired, MicroLessonContentDto.class);
        } catch (Exception e) {
            org.junit.jupiter.api.Assertions.fail("Failed to parse repaired JSON: " + e.getMessage());
        }

        assertThat(dto).isNotNull();
        assertThat(dto.cheatSheet()).hasSize(2);
    }

    @Test
    @DisplayName("Repairs JSON truncated after a key colon")
    void testRepairTruncatedAfterColon() {
        String truncated = """
                {
                  "coreConcept": "Core concept",
                  "trap":
                """;

        String repaired = repairEngine.repairTruncatedJson(truncated);
        assertThat(repaired.trim()).endsWith("}");

        MicroLessonContentDto dto = null;
        try {
            dto = aiObjectMapper.readValue(repaired, MicroLessonContentDto.class);
        } catch (Exception e) {
            org.junit.jupiter.api.Assertions.fail("Failed to parse repaired JSON: " + e.getMessage());
        }

        assertThat(dto).isNotNull();
        assertThat(dto.coreConcept()).isEqualTo("Core concept");
    }

    @Test
    @DisplayName("Deterministic field extraction succeeds even on severely malformed JSON")
    void testExtractFieldsDeterministically() {
        String severelyMangled = """
                "coreConcept": "Mangled core concept text",
                "trap": "Mangled trap description",
                "cheatSheet": ["Formula Alpha", "Formula Beta"],
                "questionStem": "Mangled question prompt",
                "steps": ["Step 1: First step", "Step 2: Second step"],
                "finalAnswer": "42"
                Random trailing corrupt noise!
                """;

        MicroLessonContentDto dto = repairEngine.extractFieldsDeterministically(severelyMangled);
        assertThat(dto).isNotNull();
        assertThat(dto.coreConcept()).isEqualTo("Mangled core concept text");
        assertThat(dto.trap()).isEqualTo("Mangled trap description");
        assertThat(dto.cheatSheet()).containsExactly("Formula Alpha", "Formula Beta");
        assertThat(dto.stepByStepBreakdown().questionStem()).isEqualTo("Mangled question prompt");
        assertThat(dto.stepByStepBreakdown().steps()).hasSize(2);
        assertThat(dto.stepByStepBreakdown().finalAnswer()).isEqualTo("42");
    }

    @Test
    @DisplayName("Full parseAndRepair pipeline repairs and ensures non-null fields on truncated input")
    void testParseAndRepairPipelineTruncated() throws Exception {
        String truncatedResponse = """
                ```json
                {
                  "coreConcept": "Angular momentum conservation principle.",
                  "trap": "Forgetting that external torque must be zero.",
                  "cheatSheet": [
                    "$$L = I \\omega$$",
                    "$$\\tau_{ext} = \\frac{dL}{dt}$$"
                  ],
                  "stepByStepBreakdown": {
                    "questionStem": "A spinning ice skater pulls her arms in.",
                    "steps": [
                      "Step 1: Note that no external torque acts on the skater.",
                      "Step 2: Apply $I_1 \\omega_1 = I_2 \\omega_2$"
                """;

        MicroLessonContentDto result = repairEngine.parseAndRepair(truncatedResponse, aiObjectMapper);
        assertThat(result).isNotNull();
        assertThat(result.coreConcept()).contains("Angular momentum");
        assertThat(result.trap()).contains("external torque");
        assertThat(result.cheatSheet()).hasSize(2);
        assertThat(result.stepByStepBreakdown()).isNotNull();
        assertThat(result.stepByStepBreakdown().questionStem()).contains("spinning ice skater");
        assertThat(result.stepByStepBreakdown().steps()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result.stepByStepBreakdown().finalAnswer()).isNotBlank();
    }
}
