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

import com.examplatform.questionbank.domain.Passage;
import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.repository.PassageRepository;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionBankGrpcServiceImpl extends QuestionBankGrpcServiceGrpc.QuestionBankGrpcServiceImplBase {

    private final QuestionRepository questionRepository;
    private final PassageRepository passageRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void getQuestionsForPaper(PaperQuestionsGrpcRequest request,
                                     StreamObserver<PaperQuestionsGrpcResponse> responseObserver) {
        log.info("gRPC getQuestionsForPaper: paperId={}, tenant={}", request.getPaperId(), request.getTenantId());

        try {
            List<Question> questions = questionRepository.findByTenantId(request.getTenantId());
            PaperQuestionsGrpcResponse response = PaperQuestionsGrpcResponse.newBuilder()
                    .addAllQuestions(toGrpcQuestions(questions))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            handleGrpcError("retrieve paper questions", e, responseObserver);
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

            BatchFindQuestionsGrpcResponse response = BatchFindQuestionsGrpcResponse.newBuilder()
                    .addAllQuestions(toGrpcQuestions(questions))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            handleGrpcError("batch find questions", e, responseObserver);
        }
    }

    @Override
    public void matchBlueprint(BlueprintMatchGrpcRequest request,
                               StreamObserver<BlueprintMatchGrpcResponse> responseObserver) {
        log.info("gRPC matchBlueprint: subject={}, topic={}, difficulty={}, cognitiveLevel={}, tenant={}",
                request.getSubject(), request.getTopic(), request.getDifficulty(),
                request.getCognitiveLevel(), request.getTenantId());

        try {
            String subject = request.getSubject().isBlank() ? null : request.getSubject().trim();
            String topic = request.getTopic().isBlank() ? null : request.getTopic().trim();
            String difficulty = request.getDifficulty().isBlank() ? null : request.getDifficulty().trim();
            String cognitiveLevel = request.getCognitiveLevel().isBlank() ? null : request.getCognitiveLevel().trim();
            String tenantId = request.getTenantId().isBlank() ? "default" : request.getTenantId().trim();

            List<Question> questions = questionRepository.findBlueprintQuestions(
                    subject != null ? subject : "",
                    topic != null ? topic : "",
                    difficulty,
                    cognitiveLevel,
                    tenantId
            );

            if (questions.isEmpty()) {
                questions = questionRepository.findBlueprintQuestionsFallback(
                        subject != null ? subject : "",
                        topic != null ? topic : "",
                        difficulty,
                        tenantId
                );
            }

            BlueprintMatchGrpcResponse response = BlueprintMatchGrpcResponse.newBuilder()
                    .addAllQuestions(toGrpcQuestions(questions))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            handleGrpcError("match blueprint", e, responseObserver);
        }
    }

    private List<QuestionSummaryGrpc> toGrpcQuestions(List<Question> questions) {
        Map<UUID, Passage> passageMap = fetchPassagesForQuestions(questions);
        List<QuestionSummaryGrpc> list = new ArrayList<>(questions.size());
        for (Question q : questions) {
            Passage p = q.getPassageId() != null ? passageMap.get(q.getPassageId()) : null;
            list.add(toGrpcQuestion(q, p));
        }
        return list;
    }

    private void handleGrpcError(String action, Exception e, StreamObserver<?> responseObserver) {
        log.error("Error processing " + action + " gRPC request", e);
        responseObserver.onError(io.grpc.Status.INTERNAL
                .withDescription("Failed to " + action + ": " + e.getMessage())
                .withCause(e)
                .asRuntimeException());
    }

    private Map<UUID, Passage> fetchPassagesForQuestions(List<Question> questions) {
        List<UUID> passageIds = questions.stream()
                .map(Question::getPassageId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (passageIds.isEmpty()) {
            return Map.of();
        }

        List<Passage> passages = passageRepository.findAllById(passageIds);
        return passages.stream().collect(Collectors.toMap(Passage::getId, p -> p));
    }

    private QuestionSummaryGrpc toGrpcQuestion(Question q, Passage passage) {
        QuestionSummaryGrpc.Builder b = QuestionSummaryGrpc.newBuilder()
                .setMarks(1.0)
                .setNegativeMarks(0.0)
                .setUsageCount(q.getUsageCount());

        java.util.Optional.ofNullable(q.getId()).ifPresent(id -> b.setId(id.toString()));
        java.util.Optional.ofNullable(q.getContent()).ifPresent(b::setContent);
        java.util.Optional.ofNullable(q.getQuestionType()).ifPresent(b::setQuestionType);
        java.util.Optional.ofNullable(q.getDifficulty()).ifPresent(b::setDifficulty);
        java.util.Optional.ofNullable(q.getCognitiveLevel()).ifPresent(b::setCognitiveLevel);
        java.util.Optional.ofNullable(q.getTopicId()).ifPresent(id -> b.setTopicId(id.toString()));
        java.util.Optional.ofNullable(q.getSubjectId()).ifPresent(id -> b.setSubjectId(id.toString()));
        java.util.Optional.ofNullable(q.getAnswerKey()).ifPresent(b::setAnswerKey);
        java.util.Optional.ofNullable(q.getExplanation()).ifPresent(b::setExplanation);
        java.util.Optional.ofNullable(q.getSubject()).ifPresent(b::setSubject);
        java.util.Optional.ofNullable(q.getTopic()).ifPresent(b::setTopic);

        if (q.getPassageId() != null) {
            b.setPassageId(q.getPassageId().toString());
            if (passage != null && passage.getContent() != null) {
                b.setPassageContent(passage.getContent());
            }
            if (q.getPassageOrderIndex() != null) {
                b.setPassageOrderIndex(q.getPassageOrderIndex());
            }
        }

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
