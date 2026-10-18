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

package com.examplatform.questionbank.service;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.shared.constants.QuestionStateConstants;
import com.examplatform.shared.constants.QuestionTaxonomyConstants;
import com.examplatform.shared.constants.QuestionTypeConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link QuestionSearchService}.
 *
 * Validates: Requirements 19.3, 26.5
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionSearchService")
class QuestionSearchServiceTest {

    @Mock
    QuestionRepository questionRepository;

    @InjectMocks
    QuestionSearchService questionSearchService;

    private static final String TENANT_ID = "default";

    @Test
    @DisplayName("search returns matching questions filtered by subject and query")
    void searchReturnsFilteredResults() {
        Question q1 = Question.builder()
                .subject("Mathematics")
                .topic("Algebra")
                .subtopic("Linear Equations")
                .difficulty(QuestionTaxonomyConstants.Difficulty.MEDIUM)
                .cognitiveLevel(QuestionTaxonomyConstants.CognitiveLevel.APPLY)
                .questionType("MCQ")
                .content("Solve 2x + 3 = 7")
                .state(QuestionStateConstants.PUBLISHED)
                .authorId(UUID.randomUUID())
                .build();

        Question q2 = Question.builder()
                .subject("Mathematics")
                .topic("Calculus")
                .subtopic("Integration")
                .difficulty(QuestionTaxonomyConstants.Difficulty.HARD)
                .cognitiveLevel(QuestionTaxonomyConstants.CognitiveLevel.ANALYZE)
                .questionType("MCQ")
                .content("Integrate x^2 dx")
                .state(QuestionStateConstants.PUBLISHED)
                .authorId(UUID.randomUUID())
                .build();

        when(questionRepository.findBySubjectAndStateAndTenantId("Mathematics", QuestionStateConstants.PUBLISHED, TENANT_ID))
                .thenReturn(List.of(q1, q2));

        Page<QuestionResponse> results = questionSearchService.search(
                "Algebra", "Mathematics", null, 0, 10, TENANT_ID);

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getTopic()).isEqualTo("Algebra");
    }

    @Test
    @DisplayName("search filters by difficulty when provided")
    void searchFiltersByDifficulty() {
        Question q1 = Question.builder()
                .subject("Physics")
                .topic("Mechanics")
                .difficulty(QuestionTaxonomyConstants.Difficulty.EASY)
                .cognitiveLevel(QuestionTaxonomyConstants.CognitiveLevel.REMEMBER)
                .questionType("MCQ")
                .content("What is Newton's first law?")
                .state(QuestionStateConstants.PUBLISHED)
                .authorId(UUID.randomUUID())
                .build();

        Question q2 = Question.builder()
                .subject("Physics")
                .topic("Optics")
                .difficulty(QuestionTaxonomyConstants.Difficulty.HARD)
                .cognitiveLevel(QuestionTaxonomyConstants.CognitiveLevel.ANALYZE)
                .questionType("MCQ")
                .content("Derive the lens equation")
                .state(QuestionStateConstants.PUBLISHED)
                .authorId(UUID.randomUUID())
                .build();

        when(questionRepository.findBySubjectAndStateAndTenantId("Physics", QuestionStateConstants.PUBLISHED, TENANT_ID))
                .thenReturn(List.of(q1, q2));

        Page<QuestionResponse> results = questionSearchService.search(
                "", "Physics", QuestionTaxonomyConstants.Difficulty.EASY, 0, 10, TENANT_ID);

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getDifficulty()).isEqualTo(QuestionTaxonomyConstants.Difficulty.EASY);
    }

    @Test
    @DisplayName("search matches across multiple fields like chapter, explanation, cognitiveLevel, and tokens")
    void searchMatchesMultiFieldsAndTokens() {
        Question q1 = Question.builder()
                .subject("Chemistry")
                .topic("Organic Chemistry")
                .subtopic("Hydrocarbons")
                .chapter("Alkanes and Alkenes")
                .difficulty(QuestionTaxonomyConstants.Difficulty.EASY)
                .cognitiveLevel(QuestionTaxonomyConstants.CognitiveLevel.UNDERSTAND)
                .questionType(QuestionTypeConstants.SINGLE_MCQ)
                .content("What is the general formula of alkanes?")
                .explanation("Alkanes follow CnH2n+2 formula.")
                .state(QuestionStateConstants.PUBLISHED)
                .authorId(UUID.randomUUID())
                .build();

        Question q2 = Question.builder()
                .subject("Chemistry")
                .topic("Inorganic Chemistry")
                .subtopic("Periodic Table")
                .chapter("Noble Gases")
                .difficulty(QuestionTaxonomyConstants.Difficulty.HARD)
                .cognitiveLevel(QuestionTaxonomyConstants.CognitiveLevel.ANALYZE)
                .questionType(QuestionTypeConstants.DESCRIPTIVE)
                .content("Why are noble gases inert?")
                .explanation("Full valence shell creates stability.")
                .state(QuestionStateConstants.PUBLISHED)
                .authorId(UUID.randomUUID())
                .build();

        when(questionRepository.findBySubjectAndStateAndTenantId("Chemistry", QuestionStateConstants.PUBLISHED, TENANT_ID))
                .thenReturn(List.of(q1, q2));

        // Multi-token search matching topic ("Organic") and difficulty ("EASY")
        Page<QuestionResponse> results = questionSearchService.search(
                "Organic EASY", "Chemistry", null, 0, 10, TENANT_ID);

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getChapter()).isEqualTo("Alkanes and Alkenes");

        // Search matching explanation keyword
        Page<QuestionResponse> resultsExpl = questionSearchService.search(
                "valence stability", "Chemistry", null, 0, 10, TENANT_ID);

        assertThat(resultsExpl.getContent()).hasSize(1);
        assertThat(resultsExpl.getContent().get(0).getTopic()).isEqualTo("Inorganic Chemistry");
    }

    @Test
    @DisplayName("search returns empty page when no matches")
    void searchReturnsEmptyOnNoMatch() {
        when(questionRepository.findBySubjectAndStateAndTenantId("Chemistry", QuestionStateConstants.PUBLISHED, TENANT_ID))
                .thenReturn(List.of());

        Page<QuestionResponse> results = questionSearchService.search(
                "nonexistent", "Chemistry", null, 0, 10, TENANT_ID);

        assertThat(results.getContent()).isEmpty();
        assertThat(results.getTotalElements()).isZero();
    }
}
