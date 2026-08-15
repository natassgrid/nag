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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.examplatform.evaluation.repository.EvaluationRepository;

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

    private AutoEvaluationService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        service = new AutoEvaluationService(evaluationRepository, objectMapper, kafkaTemplate);
    }

    @Nested
    @DisplayName("Full marks scenarios")
    class FullMarksTests {

        @Test
        @DisplayName("All correct options selected → full marks (4.0)")
        void allCorrectOptionsSelected_awardsFullMarks() {
            double score = service.evaluateMultiMcqPartial(
                    "[\"opt-1\",\"opt-2\",\"opt-3\",\"opt-4\"]",
                    "[\"opt-1\",\"opt-2\",\"opt-3\",\"opt-4\"]",
                    4.0);

            assertThat(score).isEqualTo(4.0);
        }
    }

    @Nested
    @DisplayName("Partial marks scenarios (selection ⊆ answerKey)")
    class PartialMarksTests {

        @Test
        @DisplayName("2 of 4 correct options selected → half marks (2.0)")
        void twoOfFourCorrect_awardsHalfMarks() {
            double score = service.evaluateMultiMcqPartial(
                    "[\"opt-1\",\"opt-2\",\"opt-3\",\"opt-4\"]",
                    "[\"opt-1\",\"opt-3\"]",
                    4.0);

            assertThat(score).isEqualTo(2.0);
        }

        @Test
        @DisplayName("1 of 3 correct options selected → 1/3 of marks (≈1.333)")
        void oneOfThreeCorrect_awardsOneThirdMarks() {
            double score = service.evaluateMultiMcqPartial(
                    "[\"opt-a\",\"opt-b\",\"opt-c\"]",
                    "[\"opt-a\"]",
                    4.0);

            assertThat(score).isCloseTo(4.0 / 3.0, within(0.001));
        }
    }

    @Nested
    @DisplayName("Zero marks scenarios (incorrect options present)")
    class ZeroMarksTests {

        @Test
        @DisplayName("Selection contains one incorrect option → zero marks")
        void selectionContainsOneIncorrectOption_awardsZero() {
            double score = service.evaluateMultiMcqPartial(
                    "[\"opt-1\",\"opt-2\",\"opt-3\"]",
                    "[\"opt-4\"]",
                    4.0);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Selection contains mix of correct and incorrect → zero marks")
        void selectionContainsMixOfCorrectAndIncorrect_awardsZero() {
            double score = service.evaluateMultiMcqPartial(
                    "[\"opt-1\",\"opt-2\",\"opt-3\"]",
                    "[\"opt-1\",\"opt-4\"]",
                    4.0);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Empty selection → zero marks")
        void emptySelection_awardsZero() {
            double score = service.evaluateMultiMcqPartial(
                    "[\"opt-1\",\"opt-2\",\"opt-3\"]",
                    "[]",
                    4.0);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Null selection → zero marks")
        void nullSelection_awardsZero() {
            double score = service.evaluateMultiMcqPartial(
                    "[\"opt-1\",\"opt-2\",\"opt-3\"]",
                    null,
                    4.0);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("All options selected including wrong ones → zero marks")
        void allOptionsIncludingWrongSelected_awardsZero() {
            // Answer key has opt-1, opt-2, opt-3 as correct
            // Candidate selects all including opt-4 (wrong)
            double score = service.evaluateMultiMcqPartial(
                    "[\"opt-1\",\"opt-2\",\"opt-3\"]",
                    "[\"opt-1\",\"opt-2\",\"opt-3\",\"opt-4\"]",
                    4.0);

            assertThat(score).isEqualTo(0.0);
        }
    }
}
