/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.questionbank.grpc;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionBankGrpcServiceImpl extends QuestionBankGrpcServiceGrpc.QuestionBankGrpcServiceImplBase {

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void getQuestionsForPaper(PaperQuestionsGrpcRequest request,
                                     StreamObserver<PaperQuestionsGrpcResponse> responseObserver) {
        log.info("gRPC getQuestionsForPaper: paperId={}, tenant={}", request.getPaperId(), request.getTenantId());

        try {
            List<Question> questions = questionRepository.findByTenantId(request.getTenantId());
            PaperQuestionsGrpcResponse.Builder builder = PaperQuestionsGrpcResponse.newBuilder();

            for (Question q : questions) {
                builder.addQuestions(toGrpcQuestion(q));
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error processing getQuestionsForPaper gRPC request", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to retrieve paper questions: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void batchFindQuestions(BatchFindQuestionsGrpcRequest request,
                                   StreamObserver<BatchFindQuestionsGrpcResponse> responseObserver) {
        log.info("gRPC batchFindQuestions: count={}, tenant={}", request.getQuestionIdsCount(), request.getTenantId());

        try {
            List<UUID> uuids = new ArrayList<>();
            for (String idStr : request.getQuestionIdsList()) {
                try {
                    uuids.add(UUID.fromString(idStr));
                } catch (IllegalArgumentException ignored) {
                }
            }

            List<Question> questions = !uuids.isEmpty()
                    ? questionRepository.findQuestionsByIdsIn(uuids, request.getTenantId())
                    : List.of();

            BatchFindQuestionsGrpcResponse.Builder builder = BatchFindQuestionsGrpcResponse.newBuilder();
            for (Question q : questions) {
                builder.addQuestions(toGrpcQuestion(q));
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error processing batchFindQuestions gRPC request", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to batch find questions: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    private QuestionSummaryGrpc toGrpcQuestion(Question q) {
        QuestionSummaryGrpc.Builder b = QuestionSummaryGrpc.newBuilder();
        if (q.getId() != null) b.setId(q.getId().toString());
        if (q.getContent() != null) b.setContent(q.getContent());
        if (q.getQuestionType() != null) b.setQuestionType(q.getQuestionType());
        if (q.getDifficulty() != null) b.setDifficulty(q.getDifficulty());
        if (q.getCognitiveLevel() != null) b.setCognitiveLevel(q.getCognitiveLevel());
        b.setMarks(1.0);
        b.setNegativeMarks(0.0);
        if (q.getTopicId() != null) b.setTopicId(q.getTopicId().toString());
        if (q.getSubjectId() != null) b.setSubjectId(q.getSubjectId().toString());
        if (q.getAnswerKey() != null) b.setAnswerKey(q.getAnswerKey());
        if (q.getExplanation() != null) b.setExplanation(q.getExplanation());

        if (q.getOptions() != null) {
            try {
                b.setOptionsJson(objectMapper.writeValueAsString(q.getOptions()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize options for question {}", q.getId(), e);
            }
        }
        return b.build();
    }
}
