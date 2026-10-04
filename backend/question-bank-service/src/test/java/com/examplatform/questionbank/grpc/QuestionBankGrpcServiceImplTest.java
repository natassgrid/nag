/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.questionbank.grpc;

import com.examplatform.questionbank.domain.Passage;
import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.repository.PassageRepository;
import com.examplatform.questionbank.repository.QuestionRepository;
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
    private StreamObserver<PaperQuestionsGrpcResponse> paperQuestionsObserver;

    @Mock
    private StreamObserver<BatchFindQuestionsGrpcResponse> batchObserver;

    @Mock
    private StreamObserver<BlueprintMatchGrpcResponse> blueprintObserver;

    private QuestionBankGrpcServiceImpl grpcService;

    @BeforeEach
    void setUp() {
        grpcService = new QuestionBankGrpcServiceImpl(questionRepository, passageRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("getQuestionsForPaper returns questions from repository")
    void getQuestionsForPaperSuccess() {
        UUID qId = UUID.randomUUID();
        Question q = Question.builder()
                .content("What is the capital of India?")
                .questionType("MCQ")
                .difficulty("EASY")
                .build();
        ReflectionTestUtils.setField(q, "id", qId);

        when(questionRepository.findByTenantId(eq("tenant-1"))).thenReturn(List.of(q));

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
                .content("Sample question")
                .passageId(passageId)
                .passageOrderIndex(0)
                .build();
        ReflectionTestUtils.setField(q, "id", qId);

        when(questionRepository.findQuestionsByIdsIn(eq(List.of(qId)), eq("tenant-1"))).thenReturn(List.of(q));
        when(passageRepository.findAllById(eq(List.of(passageId)))).thenReturn(List.of(passage));

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
                .subject("Physics")
                .topic("Thermodynamics")
                .difficulty("MEDIUM")
                .cognitiveLevel("APPLY")
                .content("Calculate entropy change...")
                .usageCount(3)
                .build();
        ReflectionTestUtils.setField(q, "id", qId);

        when(questionRepository.findBlueprintQuestions(
                eq("Physics"), eq("Thermodynamics"), eq("MEDIUM"), eq("APPLY"), eq("tenant-1")
        )).thenReturn(List.of(q));

        BlueprintMatchGrpcRequest request = BlueprintMatchGrpcRequest.newBuilder()
                .setSubject("Physics")
                .setTopic("Thermodynamics")
                .setDifficulty("MEDIUM")
                .setCognitiveLevel("APPLY")
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
        assertThat(response.getQuestions(0).getDifficulty()).isEqualTo("MEDIUM");
        assertThat(response.getQuestions(0).getCognitiveLevel()).isEqualTo("APPLY");
        assertThat(response.getQuestions(0).getUsageCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("matchBlueprint uses fallback query when exact match is empty")
    void matchBlueprintFallbackSuccess() {
        UUID qId = UUID.randomUUID();
        Question q = Question.builder()
                .subject("Physics")
                .topic("Thermodynamics")
                .difficulty("MEDIUM")
                .content("Fallback question...")
                .build();
        ReflectionTestUtils.setField(q, "id", qId);

        when(questionRepository.findBlueprintQuestions(
                eq("Physics"), eq("Thermodynamics"), eq("MEDIUM"), eq("APPLY"), eq("tenant-1")
        )).thenReturn(List.of());

        when(questionRepository.findBlueprintQuestionsFallback(
                eq("Physics"), eq("Thermodynamics"), eq("MEDIUM"), eq("tenant-1")
        )).thenReturn(List.of(q));

        BlueprintMatchGrpcRequest request = BlueprintMatchGrpcRequest.newBuilder()
                .setSubject("Physics")
                .setTopic("Thermodynamics")
                .setDifficulty("MEDIUM")
                .setCognitiveLevel("APPLY")
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
