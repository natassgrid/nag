package com.examplatform.questionbank.service;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.domain.QuestionVersion;
import com.examplatform.questionbank.repository.QuestionVersionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link QuestionVersioningService}.
 *
 * Validates: Requirements 4.4
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionVersioningService")
class QuestionVersioningServiceTest {

    @Mock
    private QuestionVersionRepository questionVersionRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private QuestionVersioningService questionVersioningService;

    private static final UUID QUESTION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID AUTHOR_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final String TENANT_ID = "tenant-test";

    private Question buildQuestion(String subject, String topic, String content) {
        Question q = Question.builder()
                .subject(subject)
                .topic(topic)
                .subtopic("Subtopic A")
                .chapter("Chapter 1")
                .difficulty("MEDIUM")
                .cognitiveLevel("APPLY")
                .questionType("SINGLE_MCQ")
                .content(content)
                .answerKey("{\"correct\": \"A\"}")
                .state("DRAFT")
                .authorId(AUTHOR_ID)
                .build();
        // Use reflection to set id since setId is protected
        try {
            var idField = q.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(q, QUESTION_ID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return q;
    }

    @Nested
    @DisplayName("createVersion")
    class CreateVersion {

        @Test
        @DisplayName("should detect changed fields and produce correct diff JSON")
        void shouldDetectChangedFieldsAndProduceCorrectDiff() throws Exception {
            // Given
            Question oldQuestion = buildQuestion("Mathematics", "Algebra", "<p>old content</p>");
            Question newQuestion = buildQuestion("Physics", "Algebra", "<p>new content</p>");

            when(questionVersionRepository.findTopByQuestionIdOrderByVersionNumberDesc(QUESTION_ID))
                    .thenReturn(Optional.empty());
            when(questionVersionRepository.save(any(QuestionVersion.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            questionVersioningService.createVersion(oldQuestion, newQuestion, AUTHOR_ID, TENANT_ID);

            // Then
            ArgumentCaptor<QuestionVersion> captor = ArgumentCaptor.forClass(QuestionVersion.class);
            verify(questionVersionRepository).save(captor.capture());
            QuestionVersion saved = captor.getValue();

            JsonNode diff = objectMapper.readTree(saved.getDiffJson());
            assertThat(diff.has("subject")).isTrue();
            assertThat(diff.get("subject").get("old").asText()).isEqualTo("Mathematics");
            assertThat(diff.get("subject").get("new").asText()).isEqualTo("Physics");
            assertThat(diff.has("content")).isTrue();
            assertThat(diff.get("content").get("old").asText()).isEqualTo("<p>old content</p>");
            assertThat(diff.get("content").get("new").asText()).isEqualTo("<p>new content</p>");
            // Unchanged fields should not appear in diff
            assertThat(diff.has("topic")).isFalse();
            assertThat(diff.has("difficulty")).isFalse();
        }

        @Test
        @DisplayName("should increment version number from previous version")
        void shouldIncrementVersionNumberFromPrevious() {
            // Given
            Question oldQuestion = buildQuestion("Mathematics", "Algebra", "<p>old</p>");
            Question newQuestion = buildQuestion("Mathematics", "Calculus", "<p>old</p>");

            QuestionVersion previousVersion = QuestionVersion.builder()
                    .versionNumber(3)
                    .build();
            when(questionVersionRepository.findTopByQuestionIdOrderByVersionNumberDesc(QUESTION_ID))
                    .thenReturn(Optional.of(previousVersion));
            when(questionVersionRepository.save(any(QuestionVersion.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            QuestionVersion result = questionVersioningService.createVersion(oldQuestion, newQuestion, AUTHOR_ID, TENANT_ID);

            // Then
            assertThat(result.getVersionNumber()).isEqualTo(4);
        }

        @Test
        @DisplayName("should set versionNumber to 1 for the first version")
        void shouldSetVersionNumberToOneForFirstVersion() {
            // Given
            Question oldQuestion = buildQuestion("Mathematics", "Algebra", "<p>old</p>");
            Question newQuestion = buildQuestion("Physics", "Algebra", "<p>old</p>");

            when(questionVersionRepository.findTopByQuestionIdOrderByVersionNumberDesc(QUESTION_ID))
                    .thenReturn(Optional.empty());
            when(questionVersionRepository.save(any(QuestionVersion.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            QuestionVersion result = questionVersioningService.createVersion(oldQuestion, newQuestion, AUTHOR_ID, TENANT_ID);

            // Then
            assertThat(result.getVersionNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("should produce snapshot with full JSON of updated question")
        void shouldProduceFullJsonSnapshot() throws Exception {
            // Given
            Question oldQuestion = buildQuestion("Mathematics", "Algebra", "<p>old</p>");
            Question newQuestion = buildQuestion("Physics", "Mechanics", "<p>new content</p>");

            when(questionVersionRepository.findTopByQuestionIdOrderByVersionNumberDesc(QUESTION_ID))
                    .thenReturn(Optional.empty());
            when(questionVersionRepository.save(any(QuestionVersion.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            questionVersioningService.createVersion(oldQuestion, newQuestion, AUTHOR_ID, TENANT_ID);

            // Then
            ArgumentCaptor<QuestionVersion> captor = ArgumentCaptor.forClass(QuestionVersion.class);
            verify(questionVersionRepository).save(captor.capture());
            QuestionVersion saved = captor.getValue();

            JsonNode snapshot = objectMapper.readTree(saved.getSnapshotJson());
            assertThat(snapshot.get("subject").asText()).isEqualTo("Physics");
            assertThat(snapshot.get("topic").asText()).isEqualTo("Mechanics");
            assertThat(snapshot.get("content").asText()).isEqualTo("<p>new content</p>");
            assertThat(snapshot.get("questionType").asText()).isEqualTo("SINGLE_MCQ");
            assertThat(snapshot.get("difficulty").asText()).isEqualTo("MEDIUM");
            assertThat(snapshot.get("cognitiveLevel").asText()).isEqualTo("APPLY");
            assertThat(snapshot.get("state").asText()).isEqualTo("DRAFT");
            assertThat(snapshot.get("id").asText()).isEqualTo(QUESTION_ID.toString());
            assertThat(snapshot.get("authorId").asText()).isEqualTo(AUTHOR_ID.toString());
        }

        @Test
        @DisplayName("should set authorId, questionId, changedAt, and tenantId on version")
        void shouldSetMetadataOnVersion() {
            // Given
            Question oldQuestion = buildQuestion("Mathematics", "Algebra", "<p>old</p>");
            Question newQuestion = buildQuestion("Physics", "Algebra", "<p>old</p>");

            when(questionVersionRepository.findTopByQuestionIdOrderByVersionNumberDesc(QUESTION_ID))
                    .thenReturn(Optional.empty());
            when(questionVersionRepository.save(any(QuestionVersion.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            QuestionVersion result = questionVersioningService.createVersion(oldQuestion, newQuestion, AUTHOR_ID, TENANT_ID);

            // Then
            assertThat(result.getQuestionId()).isEqualTo(QUESTION_ID);
            assertThat(result.getAuthorId()).isEqualTo(AUTHOR_ID);
            assertThat(result.getChangedAt()).isNotNull();
            assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        }
    }
}
