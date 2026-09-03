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

package com.examplatform.evaluation.service;

import com.examplatform.evaluation.repository.EvaluationRepository;
import com.examplatform.shared.config.DynamicConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for partial marking logic in Multi_MCQ evaluation.
 * Validates Requirement 12.3: Partial marking for Multiple_Correct_MCQ.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Partial Marking for Multi_MCQ")
class PartialMarkingTest {

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private DynamicConfigService dynamicConfigService;

    private AutoEvaluationService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        service = new AutoEvaluationService(evaluationRepository, objectMapper, kafkaTemplate, dynamicConfigService);
    }

    @Nested
    @DisplayName("Full marks scenarios")
    class FullMarksTests {

        @Test
        @DisplayName("All correct options selected -> full marks (4.0)")
        void allCorrectOptionsSelected_awardsFullMarks() {
            double score = service.evaluateMultiMcqPartial(
                    "[\"opt-1\",\"opt-2\",\"opt-3\",\"opt-4\"]",
                    "[\"opt-1\",\"opt-2\",\"opt-3\",\"opt-4\"]",
                    4.0);

            assertThat(score).isCloseTo(4.0, within(1e-6));
        }

        @Test
        @DisplayName("Two of two correct selected -> full marks (4.0)")
        void twoOfTwoCorrectSelected_awardsFullMarks() {
            double score = service.evaluateMultiMcqPartial(
                    "[\"opt-a\",\"opt-c\"]",
                    "[\"opt-a\",\"opt-c\"]",
                    4.0);

            assertThat(score).isCloseTo(4.0, within(1e-6));
        }
    }

    @Nested
    @DisplayName("Partial marks scenarios (JEE Advanced style)")
    class PartialMarksTests {

        @Test
        @DisplayName("3 out of 4 correct selected, no wrong options -> 3.0 marks")
        void threeOutOfFourCorrectSelected_awardsPartialMarks() {
            // (4.0 / 4) * 3 = 3.0
            double score = service.evaluateMultiMcqPartial(
                    "[\"opt-1\",\"opt-2\",\"opt-3\",\"opt-4\"]",
                    "[\"opt-1\",\"opt-2\",\"opt-3\"]",
                    4.0);

            assertThat(score).isCloseTo(3.0, within(1e-6));
        }

        @Test
        @DisplayName("2 out of 4 correct selected, no wrong options -> 2.0 marks")
        void twoOutOfFourCorrectSelected_awardsPartialMarks() {
            // (4.0 / 4) * 2 = 2.0
            double score = service.evaluateMultiMcqPartial(
                    "[\"opt-1\",\"opt-2\",\"opt-3\",\"opt-4\"]",
                    "[\"opt-1\",\"opt-2\"]",
                    4.0);

            assertThat(score).isCloseTo(2.0, within(1e-6));
        }

        @Test
        @DisplayName("1 out of 4 correct selected, no wrong options -> 1.0 mark")
        void oneOutOfFourCorrectSelected_awardsPartialMarks() {
            // (4.0 / 4) * 1 = 1.0
            double score = service.evaluateMultiMcqPartial(
                    "[\"opt-1\",\"opt-2\",\"opt-3\",\"opt-4\"]",
                    "[\"opt-1\"]",
                    4.0);

            assertThat(score).isCloseTo(1.0, within(1e-6));
        }

        @Test
        @DisplayName("1 out of 3 correct selected, no wrong options -> 4/3 = 1.333 marks")
        void oneOutOfThreeCorrectSelected_awardsFractionalMarks() {
            // (4.0 / 3) * 1 = 1.3333...
            double score = service.evaluateMultiMcqPartial(
                    "[\"opt-1\",\"opt-2\",\"opt-3\"]",
                    "[\"opt-2\"]",
                    4.0);

            assertThat(score).isCloseTo(4.0 / 3.0, within(1e-4));
        }
    }

    @Nested
    @DisplayName("Negative marks scenarios")
    class NegativeMarksTests {

        @Test
        @DisplayName("Any incorrect option selected -> -2.0 negative marks")
        void incorrectOptionSelected_awardsNegativeMarks() {
            // Correct: [1, 2, 3], Selected: [1, 2, 4] -> 4 is incorrect -> -2.0
            double score = service.evaluateMultiMcqPartial(
                    "[\"opt-1\",\"opt-2\",\"opt-3\"]",
                    "[\"opt-1\",\"opt-2\",\"opt-4\"]",
                    4.0);

            assertThat(score).isCloseTo(-2.0, within(1e-6));
        }

        @Test
        @DisplayName("All incorrect options selected -> -2.0 negative marks")
        void allIncorrectOptionsSelected_awardsNegativeMarks() {
            double score = service.evaluateMultiMcqPartial(
                    "[\"opt-1\",\"opt-2\"]",
                    "[\"opt-3\",\"opt-4\"]",
                    4.0);

            assertThat(score).isCloseTo(-2.0, within(1e-6));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Empty candidate selection -> 0.0 marks")
        void emptySelection_awardsZeroMarks() {
            double score = service.evaluateMultiMcqPartial(
                    "[\"opt-1\",\"opt-2\"]",
                    "[]",
                    4.0);

            assertThat(score).isCloseTo(0.0, within(1e-6));
        }

        @Test
        @DisplayName("Null candidate selection -> 0.0 marks")
        void nullSelection_awardsZeroMarks() {
            double score = service.evaluateMultiMcqPartial(
                    "[\"opt-1\",\"opt-2\"]",
                    null,
                    4.0);

            assertThat(score).isCloseTo(0.0, within(1e-6));
        }

        @Test
        @DisplayName("Whitespace-padded JSON arrays -> parsed correctly")
        void whitespaceInJson_parsedCorrectly() {
            double score = service.evaluateMultiMcqPartial(
                    " [ \"opt-1\" , \"opt-2\" ] ",
                    " [ \"opt-1\" ] ",
                    4.0);

            assertThat(score).isCloseTo(2.0, within(1e-6));
        }
    }
}
