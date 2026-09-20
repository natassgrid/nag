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

package com.examplatform.questionbank.translation.service;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.support.AbstractIntegrationTest;
import com.examplatform.questionbank.translation.domain.BatchTranslationJob;
import com.examplatform.questionbank.translation.domain.BatchTranslationJobStatus;
import com.examplatform.questionbank.translation.domain.Translation;
import com.examplatform.questionbank.translation.dto.AutoTranslateResponse;
import com.examplatform.questionbank.translation.dto.TranslatedOptionDto;
import com.examplatform.questionbank.translation.repository.BatchTranslationJobRepository;
import com.examplatform.questionbank.translation.repository.TranslationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("AsyncBatchTranslationWorker Database Integration Tests")
class AsyncBatchTranslationWorkerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AsyncBatchTranslationWorker worker;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private BatchTranslationJobRepository jobRepository;

    @Autowired
    private TranslationRepository translationRepository;

    @MockitoBean
    private IndicTrans2Service indicTrans2Service;

    private static final String TENANT_ID = "default";

    @BeforeEach
    void setupWorker() {
        worker.setChunkPauseIntervalQuestions(0);
        worker.setChunkPauseDurationSeconds(0);
    }

    @Test
    @DisplayName("Worker queries real DB for All Subjects batch job and processes all questions")
    void shouldFetchAndTranslateAllQuestionsFromRealDatabase() throws Exception {
        // Given 2 questions in database
        Question q1 = Question.builder()
                .subjectId(1L)
                .topicId(10L)
                .subject("Physics")
                .topic("Mechanics")
                .difficulty("MEDIUM")
                .cognitiveLevel("UNDERSTAND")
                .questionType("MCQ")
                .content("What is Newton's first law?")
                .authorId(UUID.randomUUID())
                .options(List.of(QuestionOption.builder().id("A").text("Inertia").correct(true).build()))
                .state("APPROVED")
                .build();
        q1.setTenantId(TENANT_ID);

        Question q2 = Question.builder()
                .subjectId(2L)
                .topicId(20L)
                .subject("Chemistry")
                .topic("Organic")
                .difficulty("EASY")
                .cognitiveLevel("REMEMBER")
                .questionType("MCQ")
                .content("What is methane?")
                .authorId(UUID.randomUUID())
                .options(List.of(QuestionOption.builder().id("A").text("CH4").correct(true).build()))
                .state("APPROVED")
                .build();
        q2.setTenantId(TENANT_ID);

        q1 = questionRepository.save(q1);
        q2 = questionRepository.save(q2);

        // Mock IndicTrans2 response
        when(indicTrans2Service.autoTranslateQuestionEntity(any(), any()))
                .thenAnswer(inv -> {
                    Question q = inv.getArgument(0);
                    String lang = inv.getArgument(1);
                    return AutoTranslateResponse.builder()
                            .questionId(q.getId())
                            .languageCode(lang)
                            .translatedContent("अनुवाद: " + q.getContent())
                            .translatedOptions(List.of(new TranslatedOptionDto("A", "विकल्प A")))
                            .model("IndicTrans2-v1")
                            .build();
                });

        // Create Batch Job with no subject filter (All Subjects)
        BatchTranslationJob job = BatchTranslationJob.builder()
                .status(BatchTranslationJobStatus.PENDING)
                .sourceLanguage("en")
                .targetLanguage("hi")
                .targetStatus("PUBLISHED")
                .subjectFilter(null)
                .batchSize(10)
                .throttleDelayMs(0)
                .maxConcurrency(2)
                .overwriteExisting(true)
                .initiatedBy(UUID.randomUUID())
                .build();
        job.setTenantId(TENANT_ID);
        job = jobRepository.save(job);

        // When
        worker.processBatchTranslationJob(job.getId(), TENANT_ID);

        // Await background execution
        BatchTranslationJob completedJob = null;
        for (int i = 0; i < 50; i++) {
            completedJob = jobRepository.findById(job.getId()).orElse(null);
            if (completedJob != null && completedJob.getStatus() == BatchTranslationJobStatus.COMPLETED) {
                break;
            }
            Thread.sleep(100);
        }

        // Then
        assertThat(completedJob).isNotNull();
        assertThat(completedJob.getStatus()).isEqualTo(BatchTranslationJobStatus.COMPLETED);
        assertThat(completedJob.getTotalQuestions()).isEqualTo(2);
        assertThat(completedJob.getProcessedQuestions()).isEqualTo(2);
        assertThat(completedJob.getSuccessfulQuestions()).isEqualTo(2);
        assertThat(completedJob.getFailedQuestions()).isEqualTo(0);

        List<Translation> trans1 = translationRepository.findByQuestionIdAndLanguageCodeAndTenantId(q1.getId(), "hi", TENANT_ID);
        List<Translation> trans2 = translationRepository.findByQuestionIdAndLanguageCodeAndTenantId(q2.getId(), "hi", TENANT_ID);
        assertThat(trans1).hasSize(1);
        assertThat(trans2).hasSize(1);
        assertThat(trans1.get(0).getStatus()).isEqualTo(Translation.TranslationStatus.PUBLISHED);
        assertThat(trans2.get(0).getStatus()).isEqualTo(Translation.TranslationStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Worker filters by subject correctly on real database")
    void shouldFilterBySubjectOnRealDatabase() throws Exception {
        Question q1 = Question.builder()
                .subjectId(3L)
                .topicId(30L)
                .subject("Mathematics")
                .topic("Algebra")
                .difficulty("HARD")
                .cognitiveLevel("APPLY")
                .questionType("MCQ")
                .content("Solve 2x + 4 = 10")
                .authorId(UUID.randomUUID())
                .options(List.of(QuestionOption.builder().id("A").text("x = 3").correct(true).build()))
                .state("APPROVED")
                .build();
        q1.setTenantId(TENANT_ID);

        Question q2 = Question.builder()
                .subjectId(4L)
                .topicId(40L)
                .subject("History")
                .topic("Modern India")
                .difficulty("EASY")
                .cognitiveLevel("REMEMBER")
                .questionType("MCQ")
                .content("When did India become independent?")
                .authorId(UUID.randomUUID())
                .options(List.of(QuestionOption.builder().id("A").text("1947").correct(true).build()))
                .state("APPROVED")
                .build();
        q2.setTenantId(TENANT_ID);

        q1 = questionRepository.save(q1);
        q2 = questionRepository.save(q2);

        when(indicTrans2Service.autoTranslateQuestionEntity(any(), any()))
                .thenAnswer(inv -> {
                    Question q = inv.getArgument(0);
                    String lang = inv.getArgument(1);
                    return AutoTranslateResponse.builder()
                            .questionId(q.getId())
                            .languageCode(lang)
                            .translatedContent("अनुवाद: " + q.getContent())
                            .translatedOptions(List.of(new TranslatedOptionDto("A", "विकल्प A")))
                            .model("IndicTrans2-v1")
                            .build();
                });

        // Job filtered only for Mathematics
        BatchTranslationJob job = BatchTranslationJob.builder()
                .status(BatchTranslationJobStatus.PENDING)
                .sourceLanguage("en")
                .targetLanguage("hi")
                .targetStatus("PUBLISHED")
                .subjectFilter("Mathematics")
                .batchSize(10)
                .throttleDelayMs(0)
                .maxConcurrency(2)
                .overwriteExisting(true)
                .initiatedBy(UUID.randomUUID())
                .build();
        job.setTenantId(TENANT_ID);
        job = jobRepository.save(job);

        // When
        worker.processBatchTranslationJob(job.getId(), TENANT_ID);

        // Await background execution
        BatchTranslationJob completedJob = null;
        for (int i = 0; i < 50; i++) {
            completedJob = jobRepository.findById(job.getId()).orElse(null);
            if (completedJob != null && completedJob.getStatus() == BatchTranslationJobStatus.COMPLETED) {
                break;
            }
            Thread.sleep(100);
        }

        // Then
        assertThat(completedJob).isNotNull();
        assertThat(completedJob.getStatus()).isEqualTo(BatchTranslationJobStatus.COMPLETED);
        assertThat(completedJob.getTotalQuestions()).isEqualTo(1);
        assertThat(completedJob.getProcessedQuestions()).isEqualTo(1);

        List<Translation> trans1 = translationRepository.findByQuestionIdAndLanguageCodeAndTenantId(q1.getId(), "hi", TENANT_ID);
        List<Translation> trans2 = translationRepository.findByQuestionIdAndLanguageCodeAndTenantId(q2.getId(), "hi", TENANT_ID);
        assertThat(trans1).hasSize(1);
        assertThat(trans2).isEmpty();
    }
}
