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
import com.examplatform.questionbank.domain.QuestionOption;
import com.examplatform.questionbank.domain.QuestionType;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    void shouldFetchAndTranslateAllQuestionsFromRealDatabase() {
        // Given 3 questions in database
        Question q1 = Question.builder()
                .subject("Physics")
                .topic("Mechanics")
                .type(QuestionType.MCQ)
                .content("What is Newton's first law?")
                .authorId(UUID.randomUUID())
                .options(List.of(QuestionOption.builder().key("A").text("Inertia").correct(true).build()))
                .state("APPROVED")
                .version(1)
                .build();
        q1.setTenantId(TENANT_ID);

        Question q2 = Question.builder()
                .subject("Chemistry")
                .topic("Organic")
                .type(QuestionType.MCQ)
                .content("What is methane?")
                .authorId(UUID.randomUUID())
                .options(List.of(QuestionOption.builder().key("A").text("CH4").correct(true).build()))
                .state("APPROVED")
                .version(1)
                .build();
        q2.setTenantId(TENANT_ID);

        questionRepository.saveAll(List.of(q1, q2));

        // Mock IndicTrans2 response
        when(indicTrans2Service.autoTranslateQuestionEntity(any(Question.class), eq("hi")))
                .thenAnswer(inv -> {
                    Question q = inv.getArgument(0);
                    return AutoTranslateResponse.builder()
                            .questionId(q.getId())
                            .languageCode("hi")
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
                .createdAt(Instant.now())
                .build();
        job.setTenantId(TENANT_ID);
        job = jobRepository.save(job);

        // When
        worker.processBatchTranslationJob(job.getId(), TENANT_ID);

        // Then
        BatchTranslationJob completedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(completedJob.getStatus()).isEqualTo(BatchTranslationJobStatus.COMPLETED);
        assertThat(completedJob.getTotalQuestions()).isEqualTo(2);
        assertThat(completedJob.getProcessedQuestions()).isEqualTo(2);
        assertThat(completedJob.getSuccessCount()).isEqualTo(2);
        assertThat(completedJob.getFailureCount()).isEqualTo(0);

        List<Translation> translations = translationRepository.findByTenantId(TENANT_ID);
        assertThat(translations).hasSize(2);
        assertThat(translations).allMatch(t -> "hi".equals(t.getLanguageCode()) &&
                t.getStatus() == Translation.TranslationStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Worker filters by subject correctly on real database")
    void shouldFilterBySubjectOnRealDatabase() {
        Question q1 = Question.builder()
                .subject("Mathematics")
                .topic("Algebra")
                .type(QuestionType.MCQ)
                .content("Solve 2x + 4 = 10")
                .authorId(UUID.randomUUID())
                .options(List.of(QuestionOption.builder().key("A").text("x = 3").correct(true).build()))
                .state("APPROVED")
                .version(1)
                .build();
        q1.setTenantId(TENANT_ID);

        Question q2 = Question.builder()
                .subject("History")
                .topic("Modern India")
                .type(QuestionType.MCQ)
                .content("When did India become independent?")
                .authorId(UUID.randomUUID())
                .options(List.of(QuestionOption.builder().key("A").text("1947").correct(true).build()))
                .state("APPROVED")
                .version(1)
                .build();
        q2.setTenantId(TENANT_ID);

        questionRepository.saveAll(List.of(q1, q2));

        when(indicTrans2Service.autoTranslateQuestionEntity(any(Question.class), eq("hi")))
                .thenAnswer(inv -> {
                    Question q = inv.getArgument(0);
                    return AutoTranslateResponse.builder()
                            .questionId(q.getId())
                            .languageCode("hi")
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
                .createdAt(Instant.now())
                .build();
        job.setTenantId(TENANT_ID);
        job = jobRepository.save(job);

        // When
        worker.processBatchTranslationJob(job.getId(), TENANT_ID);

        // Then
        BatchTranslationJob completedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(completedJob.getStatus()).isEqualTo(BatchTranslationJobStatus.COMPLETED);
        assertThat(completedJob.getTotalQuestions()).isEqualTo(1);
        assertThat(completedJob.getProcessedQuestions()).isEqualTo(1);

        List<Translation> translations = translationRepository.findByTenantId(TENANT_ID);
        assertThat(translations).hasSize(1);
        assertThat(translations.get(0).getQuestionId()).isEqualTo(q1.getId());
    }
}
