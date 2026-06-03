package com.examplatform.questionbank.service;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ExposureTrackingService}.
 *
 * Validates: Requirements 4.8, 4.9
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExposureTrackingService")
class ExposureTrackingServiceTest {

    @Mock
    QuestionRepository questionRepository;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    ExposureTrackingService exposureTrackingService;

    @Test
    @DisplayName("trackUsage increments usage count and sets lastUsedAt")
    void tracksUsageCorrectly() {
        UUID questionId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        String shiftId = "SHIFT-A";

        Question question = Question.builder()
                .subject("Mathematics")
                .topic("Algebra")
                .difficulty("MEDIUM")
                .cognitiveLevel("APPLY")
                .questionType("MCQ")
                .authorId(UUID.randomUUID())
                .usageCount(2)
                .build();

        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        exposureTrackingService.trackUsage(questionId, examId, shiftId);

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(captor.capture());

        Question saved = captor.getValue();
        assertThat(saved.getUsageCount()).isEqualTo(3);
        assertThat(saved.getLastUsedAt()).isNotNull();
        assertThat(saved.getUsedInExamIdsJson()).contains(examId.toString());
        assertThat(saved.getUsedInShiftIdsJson()).contains(shiftId);
    }

    @Test
    @DisplayName("trackUsage appends to existing exam/shift lists")
    void appendsToExistingLists() {
        UUID questionId = UUID.randomUUID();
        UUID existingExamId = UUID.randomUUID();
        UUID newExamId = UUID.randomUUID();
        String existingShift = "SHIFT-A";
        String newShift = "SHIFT-B";

        Question question = Question.builder()
                .subject("Physics")
                .topic("Mechanics")
                .difficulty("HARD")
                .cognitiveLevel("ANALYZE")
                .questionType("MCQ")
                .authorId(UUID.randomUUID())
                .usageCount(1)
                .usedInExamIdsJson("[\"" + existingExamId + "\"]")
                .usedInShiftIdsJson("[\"" + existingShift + "\"]")
                .build();

        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        exposureTrackingService.trackUsage(questionId, newExamId, newShift);

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(captor.capture());

        Question saved = captor.getValue();
        assertThat(saved.getUsageCount()).isEqualTo(2);
        assertThat(saved.getUsedInExamIdsJson()).contains(existingExamId.toString());
        assertThat(saved.getUsedInExamIdsJson()).contains(newExamId.toString());
        assertThat(saved.getUsedInShiftIdsJson()).contains(existingShift);
        assertThat(saved.getUsedInShiftIdsJson()).contains(newShift);
    }

    @Test
    @DisplayName("trackUsage throws when question not found")
    void throwsWhenQuestionNotFound() {
        UUID questionId = UUID.randomUUID();

        when(questionRepository.findById(questionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exposureTrackingService.trackUsage(questionId, UUID.randomUUID(), "SHIFT-A"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(questionId.toString());
    }
}
