/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 */

package com.examplatform.questionbank.grpc;

import com.examplatform.questionbank.domain.Passage;
import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.repository.PassageRepository;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.shared.constants.QuestionTaxonomyConstants;
import com.examplatform.shared.taxonomy.TaxonomyClient;
import com.examplatform.shared.taxonomy.TaxonomyNames;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionBankGrpcServiceImplTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private PassageRepository passageRepository;

    @Mock
    private TaxonomyClient taxonomyClient;

    @Mock
    private StreamObserver<PaperQuestionsGrpcResponse> paperQuestionsObserver;

    @Mock
    private StreamObserver<BatchFindQuestionsGrpcResponse> batchObserver;

    @Mock
    private StreamObserver<BlueprintMatchGrpcResponse> blueprintObserver;

    private QuestionBankGrpcServiceImpl grpcService;

    @BeforeEach
    void setUp() {
        grpcService = new QuestionBankGrpcServiceImpl(questionRepository, passageRepository, new ObjectMapper(), taxonomyClient);
    }

    @Test
    @DisplayName("getQuestionsForPaper returns questions from repository")
    void getQuestionsForPaperSuccess() {
        UUID qId = UUID.randomUUID();
        Question q = Question.builder()
                .subjectId(1L)
                .topicId(10L)
                .subtopicId(100L)
                .content("What is the capital of India?")
                .questionType("MCQ")
                .difficulty(QuestionTaxonomyConstants.Difficulty.EASY)
                .build();
        ReflectionTestUtils.setField(q, "id", qId);

        when(questionRepository.findByTenantId(eq("tenant-1"))).thenReturn(List.of(q));
        when(taxonomyClient.resolveNames(any(com.examplatform.shared.taxonomy.TaxonomyNodeRef.class), any())).thenReturn(new TaxonomyNames("General Studies", "Polity", "Capitals"));

        PaperQuestionsGrpcRequest request = PaperQuestionsGrpcRequest.newBuilder()
                .setPaperId(UUID.randomUUID().toString())
                .setTenantId("tenant-1")
                .build();

        grpcService.getQuestionsForPaper(request, paperQuestionsObserver);

        ArgumentCaptor<PaperQuestionsGrpcResponse> captor = ArgumentCaptor.forClass(PaperQuestionsGrpcResponse.class);
        verify(paperQuestionsObserver).onNext(captor.capture());
        verify(paperQuestionsObserver).onCompleted();

        PaperQuestionsGrpcResponse response = captor.getValue();
        assertThat(response.getQuestionsCount()).isEqualTo(1);
        assertThat(response.getQuestions(0).getId()).isEqualTo(qId.toString());
        assertThat(response.getQuestions(0).getContent()).isEqualTo("What is the capital of India?");
        assertThat(response.getQuestions(0).getSubtopicId()).isEqualTo("100");
    }

    @Test
    @DisplayName("batchFindQuestions returns matching questions with passage data")
    void batchFindQuestionsSuccess() {
        UUID qId = UUID.randomUUID();
        UUID passageId = UUID.randomUUID();
        Passage passage = Passage.builder()
                .content("Read this passage carefully...")
                .build();
        ReflectionTestUtils.setField(passage, "id", passageId);

        Question q = Question.builder()
                .subjectId(1L)
                .topicId(10L)
                .subtopicId(100L)
                .content("Sample question")
                .passageId(passageId)
                .passageOrderIndex(0)
                .build();
        ReflectionTestUtils.setField(q, "id", qId);

        when(questionRepository.findQuestionsByIdsIn(eq(List.of(qId)), eq("tenant-1"))).thenReturn(List.of(q));
        when(passageRepository.findAllById(eq(List.of(passageId)))).thenReturn(List.of(passage));
        when(taxonomyClient.resolveNames(any(com.examplatform.shared.taxonomy.TaxonomyNodeRef.class), any())).thenReturn(new TaxonomyNames("English", "Comprehension", "Passage Analysis"));

        BatchFindQuestionsGrpcRequest request = BatchFindQuestionsGrpcRequest.newBuilder()
                .addQuestionIds(qId.toString())
                .setTenantId("tenant-1")
                .build();

        grpcService.batchFindQuestions(request, batchObserver);

        ArgumentCaptor<BatchFindQuestionsGrpcResponse> captor = ArgumentCaptor.forClass(BatchFindQuestionsGrpcResponse.class);
        verify(batchObserver).onNext(captor.capture());
        verify(batchObserver).onCompleted();

        BatchFindQuestionsGrpcResponse response = captor.getValue();
        assertThat(response.getQuestionsCount()).isEqualTo(1);
        assertThat(response.getQuestions(0).getId()).isEqualTo(qId.toString());
        assertThat(response.getQuestions(0).getContent()).isEqualTo("Sample question");
        assertThat(response.getQuestions(0).getPassageId()).isEqualTo(passageId.toString());
        assertThat(response.getQuestions(0).getPassageContent()).isEqualTo("Read this passage carefully...");
        assertThat(response.getQuestions(0).getPassageOrderIndex()).isEqualTo(0);
    }

    @Test
    @DisplayName("matchBlueprint returns matching questions with subject, topic, and difficulty")
    void matchBlueprintSuccess() {
        UUID qId = UUID.randomUUID();
        Question q = Question.builder()
                .subjectId(101L)
                .topicId(202L)
                .subtopicId(303L)
                .difficulty(QuestionTaxonomyConstants.Difficulty.MEDIUM)
                .cognitiveLevel(QuestionTaxonomyConstants.CognitiveLevel.APPLY)
                .content("Calculate entropy change...")
                .usageCount(3)
                .build();
        ReflectionTestUtils.setField(q, "id", qId);

        when(questionRepository.findBlueprintQuestions(
                eq(101L), eq(202L), eq(303L), eq(QuestionTaxonomyConstants.Difficulty.MEDIUM), eq(
                QuestionTaxonomyConstants.CognitiveLevel.APPLY), eq("tenant-1")
        )).thenReturn(List.of(q));
        when(taxonomyClient.resolveNames(any(com.examplatform.shared.taxonomy.TaxonomyNodeRef.class), any())).thenReturn(new TaxonomyNames("Physics", "Thermodynamics", "Entropy"));

        BlueprintMatchGrpcRequest request = BlueprintMatchGrpcRequest.newBuilder()
                .setSubjectId(101L)
                .setTopicId(202L)
                .setSubtopicId(303L)
                .setDifficulty(QuestionTaxonomyConstants.Difficulty.MEDIUM)
                .setCognitiveLevel(QuestionTaxonomyConstants.CognitiveLevel.APPLY)
                .setTenantId("tenant-1")
                .build();

        grpcService.matchBlueprint(request, blueprintObserver);

        ArgumentCaptor<BlueprintMatchGrpcResponse> captor = ArgumentCaptor.forClass(BlueprintMatchGrpcResponse.class);
        verify(blueprintObserver).onNext(captor.capture());
        verify(blueprintObserver).onCompleted();

        BlueprintMatchGrpcResponse response = captor.getValue();
        assertThat(response.getQuestionsCount()).isEqualTo(1);
        assertThat(response.getQuestions(0).getId()).isEqualTo(qId.toString());
        assertThat(response.getQuestions(0).getSubject()).isEqualTo("Physics");
        assertThat(response.getQuestions(0).getTopic()).isEqualTo("Thermodynamics");
        assertThat(response.getQuestions(0).getSubtopic()).isEqualTo("Entropy");
        assertThat(response.getQuestions(0).getSubjectId()).isEqualTo("101");
        assertThat(response.getQuestions(0).getTopicId()).isEqualTo("202");
        assertThat(response.getQuestions(0).getSubtopicId()).isEqualTo("303");
        assertThat(response.getQuestions(0).getDifficulty()).isEqualTo(QuestionTaxonomyConstants.Difficulty.MEDIUM);
        assertThat(response.getQuestions(0).getCognitiveLevel()).isEqualTo(
                QuestionTaxonomyConstants.CognitiveLevel.APPLY);
        assertThat(response.getQuestions(0).getUsageCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("matchBlueprint uses fallback query when exact match is empty")
    void matchBlueprintFallbackSuccess() {
        UUID qId = UUID.randomUUID();
        Question q = Question.builder()
                .subjectId(101L)
                .topicId(202L)
                .subtopicId(303L)
                .difficulty(QuestionTaxonomyConstants.Difficulty.MEDIUM)
                .content("Fallback question...")
                .build();
        ReflectionTestUtils.setField(q, "id", qId);

        when(questionRepository.findBlueprintQuestions(
                eq(101L), eq(202L), eq(303L), eq(QuestionTaxonomyConstants.Difficulty.MEDIUM), eq(
                QuestionTaxonomyConstants.CognitiveLevel.APPLY), eq("tenant-1")
        )).thenReturn(List.of());

        when(questionRepository.findBlueprintQuestionsFallback(
                eq(101L), eq(202L), eq(303L), eq(QuestionTaxonomyConstants.Difficulty.MEDIUM), eq("tenant-1")
        )).thenReturn(List.of(q));
        when(taxonomyClient.resolveNames(any(com.examplatform.shared.taxonomy.TaxonomyNodeRef.class), any())).thenReturn(new TaxonomyNames("Physics", "Thermodynamics", "Fallback"));

        BlueprintMatchGrpcRequest request = BlueprintMatchGrpcRequest.newBuilder()
                .setSubjectId(101L)
                .setTopicId(202L)
                .setSubtopicId(303L)
                .setDifficulty(QuestionTaxonomyConstants.Difficulty.MEDIUM)
                .setCognitiveLevel(QuestionTaxonomyConstants.CognitiveLevel.APPLY)
                .setTenantId("tenant-1")
                .build();

        grpcService.matchBlueprint(request, blueprintObserver);

        ArgumentCaptor<BlueprintMatchGrpcResponse> captor = ArgumentCaptor.forClass(BlueprintMatchGrpcResponse.class);
        verify(blueprintObserver).onNext(captor.capture());
        verify(blueprintObserver).onCompleted();

        BlueprintMatchGrpcResponse response = captor.getValue();
        assertThat(response.getQuestionsCount()).isEqualTo(1);
        assertThat(response.getQuestions(0).getId()).isEqualTo(qId.toString());
        assertThat(response.getQuestions(0).getContent()).isEqualTo("Fallback question...");
    }
}
