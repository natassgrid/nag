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
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    com.examplatform.questionbank.repository.SubjectRepository subjectRepository;

    @Mock
    com.examplatform.shared.taxonomy.TaxonomyClient taxonomyClient;

    @InjectMocks
    QuestionSearchService questionSearchService;

    private static final String TENANT_ID = "default";

    private com.examplatform.questionbank.domain.Subject createSubject(Long id, String name) {
        com.examplatform.questionbank.domain.Subject s = com.examplatform.questionbank.domain.Subject.builder().name(name).build();
        org.springframework.test.util.ReflectionTestUtils.setField(s, "id", id);
        return s;
    }

    @Test
    @DisplayName("search returns matching questions filtered by subject and query")
    void searchReturnsFilteredResults() {
        Question q1 = Question.builder()
                .subjectId(1L)
                .topicId(10L)
                .subtopicId(100L)
                .chapter("Algebra Basics")
                .difficulty(QuestionTaxonomyConstants.Difficulty.MEDIUM)
                .cognitiveLevel(QuestionTaxonomyConstants.CognitiveLevel.APPLY)
                .questionType("MCQ")
                .content("Solve 2x + 3 = 7")
                .state(QuestionStateConstants.PUBLISHED)
                .authorId(UUID.randomUUID())
                .build();

        Question q2 = Question.builder()
                .subjectId(1L)
                .topicId(20L)
                .subtopicId(200L)
                .chapter("Calculus Basics")
                .difficulty(QuestionTaxonomyConstants.Difficulty.HARD)
                .cognitiveLevel(QuestionTaxonomyConstants.CognitiveLevel.ANALYZE)
                .questionType("MCQ")
                .content("Integrate x^2 dx")
                .state(QuestionStateConstants.PUBLISHED)
                .authorId(UUID.randomUUID())
                .build();

        when(subjectRepository.findByNameAndTenantId("Mathematics", TENANT_ID))
                .thenReturn(java.util.Optional.of(createSubject(1L, "Mathematics")));
        when(questionRepository.findBySubjectIdAndStateAndTenantId(1L, QuestionStateConstants.PUBLISHED, TENANT_ID))
                .thenReturn(List.of(q1, q2));
        when(taxonomyClient.resolveNames(any(com.examplatform.shared.taxonomy.TaxonomyNodeRef.class), any()))
                .thenReturn(new com.examplatform.shared.taxonomy.TaxonomyNames("Mathematics", "Algebra", "Linear Equations"));

        Page<QuestionResponse> results = questionSearchService.search(
                "Algebra", "Mathematics", null, 0, 10, TENANT_ID);

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getTopic()).isEqualTo("Algebra");
    }

    @Test
    @DisplayName("search filters by difficulty when provided")
    void searchFiltersByDifficulty() {
        Question q1 = Question.builder()
                .subjectId(2L)
                .topicId(11L)
                .subtopicId(101L)
                .difficulty(QuestionTaxonomyConstants.Difficulty.EASY)
                .cognitiveLevel(QuestionTaxonomyConstants.CognitiveLevel.REMEMBER)
                .questionType("MCQ")
                .content("What is Newton's first law?")
                .state(QuestionStateConstants.PUBLISHED)
                .authorId(UUID.randomUUID())
                .build();

        Question q2 = Question.builder()
                .subjectId(2L)
                .topicId(12L)
                .subtopicId(102L)
                .difficulty(QuestionTaxonomyConstants.Difficulty.HARD)
                .cognitiveLevel(QuestionTaxonomyConstants.CognitiveLevel.ANALYZE)
                .questionType("MCQ")
                .content("Derive the lens equation")
                .state(QuestionStateConstants.PUBLISHED)
                .authorId(UUID.randomUUID())
                .build();

        when(subjectRepository.findByNameAndTenantId("Physics", TENANT_ID))
                .thenReturn(java.util.Optional.of(createSubject(2L, "Physics")));
        when(questionRepository.findBySubjectIdAndStateAndTenantId(2L, QuestionStateConstants.PUBLISHED, TENANT_ID))
                .thenReturn(List.of(q1, q2));
        when(taxonomyClient.resolveNames(any(com.examplatform.shared.taxonomy.TaxonomyNodeRef.class), any()))
                .thenReturn(new com.examplatform.shared.taxonomy.TaxonomyNames("Physics", "Mechanics", "Kinematics"));

        Page<QuestionResponse> results = questionSearchService.search(
                "", "Physics", QuestionTaxonomyConstants.Difficulty.EASY, 0, 10, TENANT_ID);

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getDifficulty()).isEqualTo(QuestionTaxonomyConstants.Difficulty.EASY);
    }

    @Test
    @DisplayName("search matches across multiple fields like chapter, explanation, cognitiveLevel, and tokens")
    void searchMatchesMultiFieldsAndTokens() {
        Question q1 = Question.builder()
                .subjectId(3L)
                .topicId(31L)
                .subtopicId(301L)
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
                .subjectId(3L)
                .topicId(32L)
                .subtopicId(302L)
                .chapter("Noble Gases")
                .difficulty(QuestionTaxonomyConstants.Difficulty.HARD)
                .cognitiveLevel(QuestionTaxonomyConstants.CognitiveLevel.ANALYZE)
                .questionType(QuestionTypeConstants.DESCRIPTIVE)
                .content("Why are noble gases inert?")
                .explanation("Full valence shell creates stability.")
                .state(QuestionStateConstants.PUBLISHED)
                .authorId(UUID.randomUUID())
                .build();

        when(subjectRepository.findByNameAndTenantId("Chemistry", TENANT_ID))
                .thenReturn(java.util.Optional.of(createSubject(3L, "Chemistry")));
        when(questionRepository.findBySubjectIdAndStateAndTenantId(3L, QuestionStateConstants.PUBLISHED, TENANT_ID))
                .thenReturn(List.of(q1, q2));
        when(taxonomyClient.resolveNames(any(com.examplatform.shared.taxonomy.TaxonomyNodeRef.class), any()))
                .thenReturn(new com.examplatform.shared.taxonomy.TaxonomyNames("Chemistry", "Inorganic Chemistry", "Periodic Table"));

        // Multi-token search matching chapter ("Alkanes") and difficulty ("EASY")
        Page<QuestionResponse> results = questionSearchService.search(
                "Alkanes EASY", "Chemistry", null, 0, 10, TENANT_ID);

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
        when(subjectRepository.findByNameAndTenantId("Chemistry", TENANT_ID))
                .thenReturn(java.util.Optional.of(createSubject(3L, "Chemistry")));
        when(questionRepository.findBySubjectIdAndStateAndTenantId(3L, QuestionStateConstants.PUBLISHED, TENANT_ID))
                .thenReturn(List.of());

        Page<QuestionResponse> results = questionSearchService.search(
                "nonexistent", "Chemistry", null, 0, 10, TENANT_ID);

        assertThat(results.getContent()).isEmpty();
        assertThat(results.getTotalElements()).isZero();
    }
}
