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

package com.examplatform.evaluation.client;

import com.examplatform.evaluation.dto.AnswerKey;
import com.examplatform.evaluation.dto.MarkingScheme;
import com.examplatform.evaluation.exception.UpstreamServiceUnavailableException;
import com.examplatform.questionbank.grpc.BatchFindQuestionsGrpcRequest;
import com.examplatform.questionbank.grpc.BatchFindQuestionsGrpcResponse;
import com.examplatform.questionbank.grpc.PaperQuestionsGrpcRequest;
import com.examplatform.questionbank.grpc.PaperQuestionsGrpcResponse;
import com.examplatform.questionbank.grpc.QuestionBankGrpcServiceGrpc;
import com.examplatform.questionbank.grpc.QuestionSummaryGrpc;
import com.examplatform.shared.grpc.GrpcChannelFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * gRPC client implementation of {@link AnswerKeyClient} communicating with question-bank-service.
 * Retrieves answer keys and marking scheme data with Resilience4j circuit breaker protection.
 */
@Slf4j
@Component
public class QuestionBankGrpcAnswerKeyClient implements AnswerKeyClient {

    private final String grpcHost;
    private final int grpcPort;
    private final long timeoutMs;
    private final ObjectMapper objectMapper;

    public QuestionBankGrpcAnswerKeyClient(
            @Value("${grpc.client.questionbank.host:localhost}") String grpcHost,
            @Value("${grpc.client.questionbank.port:9083}") int grpcPort,
            @Value("${grpc.client.questionbank.timeout-ms:5000}") long timeoutMs,
            ObjectMapper objectMapper) {
        this.grpcHost = grpcHost;
        this.grpcPort = grpcPort;
        this.timeoutMs = timeoutMs;
        this.objectMapper = objectMapper;
    }

    @Override
    @CircuitBreaker(name = "questionBank", fallbackMethod = "fetchAnswerKeysForPaperFallback")
    public List<AnswerKey> getAnswerKeysForPaper(UUID paperId, String tenantId) {
        log.info("Fetching answer keys via gRPC for paperId={}, tenantId={} from {}:{}",
                paperId, tenantId, grpcHost, grpcPort);

        try {
            ManagedChannel channel = GrpcChannelFactory.getChannel(grpcHost, grpcPort);
            QuestionBankGrpcServiceGrpc.QuestionBankGrpcServiceBlockingStub stub =
                    QuestionBankGrpcServiceGrpc.newBlockingStub(channel)
                            .withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS);

            PaperQuestionsGrpcRequest request = PaperQuestionsGrpcRequest.newBuilder()
                    .setPaperId(paperId != null ? paperId.toString() : "")
                    .setTenantId(tenantId != null ? tenantId : "")
                    .build();

            PaperQuestionsGrpcResponse response = stub.getQuestionsForPaper(request);
            return mapGrpcQuestionsToAnswerKeys(response.getQuestionsList());
        } catch (Exception e) {
            log.error("Failed to fetch questions for paperId={} via gRPC: {}", paperId, e.getMessage());
            throw new UpstreamServiceUnavailableException(
                    "Failed to fetch answer keys for paper " + paperId + ": " + e.getMessage(), e);
        }
    }

    @Override
    @CircuitBreaker(name = "questionBank", fallbackMethod = "batchGetAnswerKeysFallback")
    public List<AnswerKey> batchGetAnswerKeys(List<UUID> questionIds, String tenantId) {
        log.info("Batch fetching answer keys via gRPC for {} questions, tenantId={} from {}:{}",
                questionIds != null ? questionIds.size() : 0, tenantId, grpcHost, grpcPort);

        if (questionIds == null || questionIds.isEmpty()) {
            return List.of();
        }

        try {
            ManagedChannel channel = GrpcChannelFactory.getChannel(grpcHost, grpcPort);
            QuestionBankGrpcServiceGrpc.QuestionBankGrpcServiceBlockingStub stub =
                    QuestionBankGrpcServiceGrpc.newBlockingStub(channel)
                            .withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS);

            BatchFindQuestionsGrpcRequest request = BatchFindQuestionsGrpcRequest.newBuilder()
                    .addAllQuestionIds(questionIds.stream().map(UUID::toString).toList())
                    .setTenantId(tenantId != null ? tenantId : "")
                    .build();

            BatchFindQuestionsGrpcResponse response = stub.batchFindQuestions(request);
            return mapGrpcQuestionsToAnswerKeys(response.getQuestionsList());
        } catch (Exception e) {
            log.error("Failed to batch fetch questions via gRPC: {}", e.getMessage());
            throw new UpstreamServiceUnavailableException(
                    "Failed to batch fetch answer keys: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback for paper answer keys retrieval when circuit breaker is open or service fails.
     */
    public List<AnswerKey> fetchAnswerKeysForPaperFallback(UUID paperId, String tenantId, Throwable ex) {
        log.warn("Circuit breaker open / fallback triggered for getAnswerKeysForPaper (paperId={}): {}",
                paperId, ex.getMessage());
        throw new UpstreamServiceUnavailableException(
                "QuestionBank service unavailable for paper " + paperId + ": " + ex.getMessage(), ex);
    }

    /**
     * Fallback for batch answer keys retrieval when circuit breaker is open or service fails.
     */
    public List<AnswerKey> batchGetAnswerKeysFallback(List<UUID> questionIds, String tenantId, Throwable ex) {
        log.warn("Circuit breaker open / fallback triggered for batchGetAnswerKeys (count={}): {}",
                questionIds != null ? questionIds.size() : 0, ex.getMessage());
        throw new UpstreamServiceUnavailableException(
                "QuestionBank service unavailable for batch questions: " + ex.getMessage(), ex);
    }

    private List<AnswerKey> mapGrpcQuestionsToAnswerKeys(List<QuestionSummaryGrpc> grpcQuestions) {
        List<AnswerKey> answerKeys = new ArrayList<>();
        for (QuestionSummaryGrpc q : grpcQuestions) {
            try {
                UUID questionId = UUID.fromString(q.getId());
                String questionType = (q.getQuestionType() != null && !q.getQuestionType().isBlank())
                        ? q.getQuestionType() : "SINGLE_MCQ";

                String correctAnswer = q.getAnswerKey();
                if ((correctAnswer == null || correctAnswer.isBlank())
                        && q.getOptionsJson() != null && !q.getOptionsJson().isBlank()) {
                    correctAnswer = extractCorrectAnswerFromOptions(q.getOptionsJson());
                }

                double marks = q.getMarks() > 0 ? q.getMarks() : 1.0;
                double negativeMarks = q.getNegativeMarks();

                AnswerKey key = AnswerKey.builder()
                        .questionId(questionId)
                        .questionType(questionType)
                        .correctAnswer(correctAnswer)
                        .marksPerQuestion(marks)
                        .negativeMarks(negativeMarks)
                        .markingScheme(MarkingScheme.STANDARD)
                        .build();

                answerKeys.add(key);
            } catch (Exception e) {
                log.warn("Failed to map gRPC question summary (id={}): {}", q.getId(), e.getMessage());
            }
        }
        return answerKeys;
    }

    private String extractCorrectAnswerFromOptions(String optionsJson) {
        try {
            JsonNode root = objectMapper.readTree(optionsJson);
            if (root.isArray()) {
                List<String> correctIds = new ArrayList<>();
                for (JsonNode opt : root) {
                    boolean isCorrect = (opt.has("isCorrect") && opt.get("isCorrect").asBoolean())
                            || (opt.has("correct") && opt.get("correct").asBoolean());
                    if (isCorrect) {
                        if (opt.has("id")) {
                            correctIds.add(opt.get("id").asText());
                        } else if (opt.has("optionId")) {
                            correctIds.add(opt.get("optionId").asText());
                        }
                    }
                }
                if (!correctIds.isEmpty()) {
                    return objectMapper.writeValueAsString(correctIds);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse correct answer from optionsJson: {}", e.getMessage());
        }
        return "";
    }
}
