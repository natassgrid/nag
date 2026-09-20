/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure - DPI Platform
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
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.translation.domain.BatchTranslationJob;
import com.examplatform.questionbank.translation.domain.BatchTranslationJobStatus;
import com.examplatform.questionbank.translation.domain.Translation;
import com.examplatform.questionbank.translation.dto.AutoTranslateResponse;
import com.examplatform.questionbank.translation.repository.BatchTranslationJobRepository;
import com.examplatform.questionbank.translation.repository.TranslationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncBatchTranslationWorkerTest {

    @Mock
    private BatchTranslationJobRepository jobRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private TranslationRepository translationRepository;

    @Mock
    private IndicTrans2Service indicTrans2Service;

    @Mock
    private TranslationWorkflowService translationWorkflowService;

    @InjectMocks
    private AsyncBatchTranslationWorker worker;

    private UUID jobId;
    private String tenantId;
    private UUID questionId1;
    private UUID questionId2;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        tenantId = "default";
        questionId1 = UUID.randomUUID();
        questionId2 = UUID.randomUUID();
        worker.setChunkPauseIntervalQuestions(0);
        worker.setChunkPauseDurationSeconds(0);
    }

    @Test
    @DisplayName("Should successfully process all questions and update job to COMPLETED")
    void shouldProcessBatchTranslationSuccessfully() {
        BatchTranslationJob job = BatchTranslationJob.builder()
                .status(BatchTranslationJobStatus.PENDING)
                .sourceLanguage("en")
                .targetLanguage("hi")
                .targetStatus("PUBLISHED")
                .batchSize(10)
                .throttleDelayMs(0)
                .maxConcurrency(2)
                .overwriteExisting(true)
                .initiatedBy(UUID.randomUUID())
                .build();
        ReflectionTestUtils.setField(job, "id", jobId);
        job.setTenantId(tenantId);

        Question q1 = Question.builder().content("What is physics?").build();
        ReflectionTestUtils.setField(q1, "id", questionId1);
        Question q2 = Question.builder().content("What is chemistry?").build();
        ReflectionTestUtils.setField(q2, "id", questionId2);

        Page<Question> page = new PageImpl<>(List.of(q1, q2), PageRequest.of(0, 10), 2);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(questionRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        AutoTranslateResponse trans1 = AutoTranslateResponse.builder()
                .questionId(questionId1)
                .languageCode("hi")
                .translatedContent("भौतिकी क्या है?")
                .build();
        AutoTranslateResponse trans2 = AutoTranslateResponse.builder()
                .questionId(questionId2)
                .languageCode("hi")
                .translatedContent("रसायन विज्ञान क्या है?")
                .build();

        when(indicTrans2Service.autoTranslateQuestionEntity(q1, "hi")).thenReturn(trans1);
        when(indicTrans2Service.autoTranslateQuestionEntity(q2, "hi")).thenReturn(trans2);

        worker.processBatchTranslationJob(jobId, tenantId);

        assertThat(job.getStatus()).isEqualTo(BatchTranslationJobStatus.COMPLETED);
        assertThat(job.getTotalQuestions()).isEqualTo(2);

        verify(jobRepository, times(2)).incrementSuccess(jobId);
        verify(translationWorkflowService, times(2)).upsertTranslation(
                any(), eq("hi"), any(), any(), any(), eq(Translation.TranslationStatus.PUBLISHED), any(), any(), eq(tenantId)
        );
    }

    @Test
    @DisplayName("Should successfully process explicit questionIds for paper generation batch")
    void shouldProcessExplicitQuestionIdsForPaperBatch() {
        UUID paperId = UUID.randomUUID();
        BatchTranslationJob job = BatchTranslationJob.builder()
                .status(BatchTranslationJobStatus.PENDING)
                .sourceLanguage("en")
                .targetLanguage("hi")
                .targetStatus("PUBLISHED")
                .paperId(paperId)
                .questionIds(List.of(questionId1, questionId2))
                .batchSize(10)
                .throttleDelayMs(0)
                .maxConcurrency(2)
                .overwriteExisting(true)
                .initiatedBy(UUID.randomUUID())
                .build();
        ReflectionTestUtils.setField(job, "id", jobId);
        job.setTenantId(tenantId);

        Question q1 = Question.builder().content("Paper Q1").build();
        ReflectionTestUtils.setField(q1, "id", questionId1);
        Question q2 = Question.builder().content("Paper Q2").build();
        ReflectionTestUtils.setField(q2, "id", questionId2);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(questionRepository.findQuestionsByIdsIn(eq(List.of(questionId1, questionId2)), eq(tenantId)))
                .thenReturn(List.of(q1, q2));

        AutoTranslateResponse trans1 = AutoTranslateResponse.builder()
                .questionId(questionId1)
                .languageCode("hi")
                .translatedContent("पेपर प्रश्न 1")
                .build();
        AutoTranslateResponse trans2 = AutoTranslateResponse.builder()
                .questionId(questionId2)
                .languageCode("hi")
                .translatedContent("पेपर प्रश्न 2")
                .build();

        when(indicTrans2Service.autoTranslateQuestionEntity(q1, "hi")).thenReturn(trans1);
        when(indicTrans2Service.autoTranslateQuestionEntity(q2, "hi")).thenReturn(trans2);

        worker.processBatchTranslationJob(jobId, tenantId);

        assertThat(job.getStatus()).isEqualTo(BatchTranslationJobStatus.COMPLETED);
        assertThat(job.getTotalQuestions()).isEqualTo(2);

        verify(questionRepository).findQuestionsByIdsIn(eq(List.of(questionId1, questionId2)), eq(tenantId));
        verify(jobRepository, times(2)).incrementSuccess(jobId);
        verify(translationWorkflowService, times(2)).upsertTranslation(
                any(), eq("hi"), any(), any(), any(), eq(Translation.TranslationStatus.PUBLISHED), any(), any(), eq(tenantId)
        );
    }

    @Test
    @DisplayName("Should skip translation when overwriteExisting is false and translation already exists")
    void shouldSkipTranslationWhenOverwriteFalseAndExists() {
        BatchTranslationJob job = BatchTranslationJob.builder()
                .status(BatchTranslationJobStatus.PENDING)
                .sourceLanguage("en")
                .targetLanguage("hi")
                .targetStatus("PUBLISHED")
                .questionIds(List.of(questionId1))
                .batchSize(10)
                .throttleDelayMs(0)
                .maxConcurrency(2)
                .overwriteExisting(false)
                .initiatedBy(UUID.randomUUID())
                .build();
        ReflectionTestUtils.setField(job, "id", jobId);
        job.setTenantId(tenantId);

        Question q1 = Question.builder().content("Paper Q1").build();
        ReflectionTestUtils.setField(q1, "id", questionId1);

        Translation existing = Translation.builder().questionId(questionId1).languageCode("hi").build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(questionRepository.findQuestionsByIdsIn(eq(List.of(questionId1)), eq(tenantId)))
                .thenReturn(List.of(q1));
        when(translationRepository.findByQuestionIdAndLanguageCodeAndTenantId(questionId1, "hi", tenantId))
                .thenReturn(List.of(existing));

        worker.processBatchTranslationJob(jobId, tenantId);

        assertThat(job.getStatus()).isEqualTo(BatchTranslationJobStatus.COMPLETED);
        verify(indicTrans2Service, never()).autoTranslateQuestionEntity(any(), any());
        verify(jobRepository, times(1)).incrementSuccess(jobId);
    }

    @Test
    @DisplayName("Should isolate errors per question so remaining questions succeed")
    void shouldIsolateErrorsPerQuestion() {
        BatchTranslationJob job = BatchTranslationJob.builder()
                .status(BatchTranslationJobStatus.PENDING)
                .sourceLanguage("en")
                .targetLanguage("hi")
                .targetStatus("PUBLISHED")
                .batchSize(10)
                .throttleDelayMs(0)
                .maxConcurrency(2)
                .overwriteExisting(true)
                .initiatedBy(UUID.randomUUID())
                .build();
        ReflectionTestUtils.setField(job, "id", jobId);
        job.setTenantId(tenantId);

        Question q1 = Question.builder().content("Question 1").build();
        ReflectionTestUtils.setField(q1, "id", questionId1);
        Question q2 = Question.builder().content("Question 2").build();
        ReflectionTestUtils.setField(q2, "id", questionId2);

        Page<Question> page = new PageImpl<>(List.of(q1, q2), PageRequest.of(0, 10), 2);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(questionRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        // Q1 fails with network / AI error
        when(indicTrans2Service.autoTranslateQuestionEntity(q1, "hi"))
                .thenThrow(new RuntimeException("IndicTrans2 connection reset"));

        // Q2 succeeds
        AutoTranslateResponse trans2 = AutoTranslateResponse.builder()
                .questionId(questionId2)
                .languageCode("hi")
                .translatedContent("प्रश्न 2")
                .build();
        when(indicTrans2Service.autoTranslateQuestionEntity(q2, "hi")).thenReturn(trans2);

        worker.processBatchTranslationJob(jobId, tenantId);

        assertThat(job.getStatus()).isEqualTo(BatchTranslationJobStatus.COMPLETED);
        verify(jobRepository, times(1)).incrementFailure(jobId);
        verify(jobRepository, times(1)).incrementSuccess(jobId);

        verify(translationWorkflowService, times(1)).upsertTranslation(
                eq(questionId2), eq("hi"), any(), any(), any(), eq(Translation.TranslationStatus.PUBLISHED), any(), any(), eq(tenantId)
        );
    }

    @Test
    @DisplayName("Should halt processing gracefully when job is CANCELLED")
    void shouldHaltWhenJobCancelled() {
        BatchTranslationJob job = BatchTranslationJob.builder()
                .status(BatchTranslationJobStatus.CANCELLED)
                .build();
        ReflectionTestUtils.setField(job, "id", jobId);
        job.setTenantId(tenantId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        worker.processBatchTranslationJob(jobId, tenantId);

        verify(questionRepository, never()).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should query by subject filter when subjectFilter is present on job")
    void shouldQueryBySubjectFilterWhenPresent() {
        BatchTranslationJob job = BatchTranslationJob.builder()
                .status(BatchTranslationJobStatus.PENDING)
                .sourceLanguage("en")
                .targetLanguage("hi")
                .targetStatus("PUBLISHED")
                .subjectFilter("Quantitative Aptitude")
                .batchSize(10)
                .throttleDelayMs(0)
                .maxConcurrency(1)
                .build();
        ReflectionTestUtils.setField(job, "id", jobId);
        job.setTenantId(tenantId);

        Question q1 = Question.builder().content("Math Q1").build();
        ReflectionTestUtils.setField(q1, "id", questionId1);

        Page<Question> page = new PageImpl<>(List.of(q1), PageRequest.of(0, 10), 1);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(questionRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        when(indicTrans2Service.autoTranslateQuestionEntity(any(), eq("hi"))).thenReturn(
                AutoTranslateResponse.builder().questionId(questionId1).languageCode("hi").translatedContent("गणित").build()
        );

        worker.processBatchTranslationJob(jobId, tenantId);

        assertThat(job.getStatus()).isEqualTo(BatchTranslationJobStatus.COMPLETED);
        verify(questionRepository).findAll(any(Specification.class), any(PageRequest.class));
        verify(jobRepository, times(1)).incrementSuccess(jobId);
    }

    @Test
    @DisplayName("Should support chunk pause interval in development configuration")
    void shouldSupportChunkPauseInterval() {
        worker.setChunkPauseIntervalQuestions(1);
        worker.setChunkPauseDurationSeconds(1);

        BatchTranslationJob job = BatchTranslationJob.builder()
                .status(BatchTranslationJobStatus.PENDING)
                .sourceLanguage("en")
                .targetLanguage("hi")
                .targetStatus("PUBLISHED")
                .batchSize(10)
                .throttleDelayMs(0)
                .maxConcurrency(1)
                .build();
        ReflectionTestUtils.setField(job, "id", jobId);
        job.setTenantId(tenantId);

        Question q1 = Question.builder().content("Q1").build();
        ReflectionTestUtils.setField(q1, "id", questionId1);
        Question q2 = Question.builder().content("Q2").build();
        ReflectionTestUtils.setField(q2, "id", questionId2);

        Page<Question> page = new PageImpl<>(List.of(q1, q2), PageRequest.of(0, 10), 2);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(questionRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        when(indicTrans2Service.autoTranslateQuestionEntity(any(), eq("hi"))).thenReturn(
                AutoTranslateResponse.builder().questionId(questionId1).languageCode("hi").translatedContent("Q").build()
        );

        worker.processBatchTranslationJob(jobId, tenantId);

        assertThat(job.getStatus()).isEqualTo(BatchTranslationJobStatus.COMPLETED);
        verify(jobRepository, times(2)).incrementSuccess(jobId);
    }
}
