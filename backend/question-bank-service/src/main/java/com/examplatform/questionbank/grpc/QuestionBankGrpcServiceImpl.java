/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.taxonomy.TaxonomyClient;
import com.examplatform.shared.taxonomy.TaxonomyNames;
import com.examplatform.shared.taxonomy.TaxonomyNodeRef;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionBankGrpcServiceImpl extends QuestionBankGrpcServiceGrpc.QuestionBankGrpcServiceImplBase {

    private final QuestionRepository questionRepository;
    private final PassageRepository passageRepository;
    private final ObjectMapper objectMapper;
    private final TaxonomyClient taxonomyClient;

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
        log.info("gRPC matchBlueprint: subjectId={}, topicId={}, subtopicId={}, difficulty={}, cognitiveLevel={}, tenant={}",
                request.getSubjectId(), request.getTopicId(), request.getSubtopicId(),
                request.getDifficulty(), request.getCognitiveLevel(), request.getTenantId());

        try {
            Long subjectId = request.getSubjectId();
            Long topicId = request.getTopicId() > 0 ? request.getTopicId() : null;
            Long subtopicId = request.getSubtopicId() > 0 ? request.getSubtopicId() : null;
            String difficulty = request.getDifficulty().isBlank() ? null : request.getDifficulty().trim();
            String cognitiveLevel = request.getCognitiveLevel().isBlank() ? null : request.getCognitiveLevel().trim();
            String tenantId = request.getTenantId().isBlank() ? SharedConstants.DEFAULT_TENANT : request.getTenantId().trim();

            List<Question> questions = questionRepository.findBlueprintQuestions(
                    subjectId,
                    topicId,
                    subtopicId,
                    difficulty,
                    cognitiveLevel,
                    tenantId
            );

            if (questions.isEmpty()) {
                questions = questionRepository.findBlueprintQuestionsFallback(
                        subjectId,
                        topicId,
                        subtopicId,
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

        if (q.getId() != null) b.setId(q.getId().toString());
        if (q.getTopicId() != null) b.setTopicId(q.getTopicId().toString());
        if (q.getSubjectId() != null) b.setSubjectId(q.getSubjectId().toString());
        if (q.getSubtopicId() != null) b.setSubtopicId(q.getSubtopicId().toString());

        applyIfPresent(q.getContent(), b::setContent);
        applyIfPresent(q.getQuestionType(), b::setQuestionType);
        applyIfPresent(q.getDifficulty(), b::setDifficulty);
        applyIfPresent(q.getCognitiveLevel(), b::setCognitiveLevel);
        applyIfPresent(q.getAnswerKey(), b::setAnswerKey);
        applyIfPresent(q.getExplanation(), b::setExplanation);

        if (taxonomyClient != null) {
            TaxonomyNames names = taxonomyClient.resolveNames(
                    new TaxonomyNodeRef(q.getSubjectId(), q.getTopicId(), q.getSubtopicId()), q.getTenantId());
            applyIfPresent(names.subjectName(), b::setSubject);
            applyIfPresent(names.topicName(), b::setTopic);
            applyIfPresent(names.subtopicName(), b::setSubtopic);
        }

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

    private static void applyIfPresent(String val, Consumer<String> setter) {
        if (val != null) {
            setter.accept(val);
        }
    }
}
