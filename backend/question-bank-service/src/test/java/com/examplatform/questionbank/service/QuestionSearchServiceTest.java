package com.examplatform.questionbank.service;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.repository.QuestionRepository;
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

    private static final String TENANT_ID = "exam-authority-1";

    @Test
    @DisplayName("search returns matching questions filtered by subject and query")
    void searchReturnsFilteredResults() {
        Question q1 = Question.builder()
                .subject("Mathematics")
                .topic("Algebra")
                .subtopic("Linear Equations")
                .difficulty("MEDIUM")
                .cognitiveLevel("APPLY")
                .questionType("MCQ")
                .content("Solve 2x + 3 = 7")
                .state("PUBLISHED")
                .authorId(UUID.randomUUID())
                .build();

        Question q2 = Question.builder()
                .subject("Mathematics")
                .topic("Calculus")
                .subtopic("Integration")
                .difficulty("HARD")
                .cognitiveLevel("ANALYZE")
                .questionType("MCQ")
                .content("Integrate x^2 dx")
                .state("PUBLISHED")
                .authorId(UUID.randomUUID())
                .build();

        when(questionRepository.findBySubjectAndStateAndTenantId("Mathematics", "PUBLISHED", TENANT_ID))
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
                .difficulty("EASY")
                .cognitiveLevel("REMEMBER")
                .questionType("MCQ")
                .content("What is Newton's first law?")
                .state("PUBLISHED")
                .authorId(UUID.randomUUID())
                .build();

        Question q2 = Question.builder()
                .subject("Physics")
                .topic("Optics")
                .difficulty("HARD")
                .cognitiveLevel("ANALYZE")
                .questionType("MCQ")
                .content("Derive the lens equation")
                .state("PUBLISHED")
                .authorId(UUID.randomUUID())
                .build();

        when(questionRepository.findBySubjectAndStateAndTenantId("Physics", "PUBLISHED", TENANT_ID))
                .thenReturn(List.of(q1, q2));

        Page<QuestionResponse> results = questionSearchService.search(
                "", "Physics", "EASY", 0, 10, TENANT_ID);

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getDifficulty()).isEqualTo("EASY");
    }

    @Test
    @DisplayName("search returns empty page when no matches")
    void searchReturnsEmptyOnNoMatch() {
        when(questionRepository.findBySubjectAndStateAndTenantId("Chemistry", "PUBLISHED", TENANT_ID))
                .thenReturn(List.of());

        Page<QuestionResponse> results = questionSearchService.search(
                "nonexistent", "Chemistry", null, 0, 10, TENANT_ID);

        assertThat(results.getContent()).isEmpty();
        assertThat(results.getTotalElements()).isZero();
    }
}
