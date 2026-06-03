package com.examplatform.response.service;

import com.examplatform.response.config.MetricsConfig;
import com.examplatform.response.domain.Response;
import com.examplatform.response.dto.SaveResponseRequest;
import com.examplatform.response.dto.SaveResponseResponse;
import com.examplatform.response.repository.ResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Service responsible for persisting candidate responses and publishing save events.
 * Enforces Kafka acks=all before acknowledging to the client (synchronous send).
 *
 * Validates: Requirements 10.1, 20.3
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ResponseSaveService {

    private static final String TOPIC_RESPONSE_SAVED = "exam.response.saved";

    private final ResponseRepository responseRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MetricsConfig metricsConfig;

    /**
     * Saves a candidate response, publishes a Kafka event with acks=all, and returns the result.
     *
     * @param sessionId   the exam session ID
     * @param request     the save request payload
     * @param candidateId the candidate's user ID (from JWT sub)
     * @param tenantId    the tenant identifier (from X-Tenant-Id header)
     * @return the save confirmation response
     */
    public SaveResponseResponse saveResponse(UUID sessionId, SaveResponseRequest request,
                                              UUID candidateId, String tenantId) {
        // 1. Determine revision sequence
        List<Response> previousResponses = responseRepository
                .findBySessionIdAndQuestionIdOrderByRevisionSequenceDesc(sessionId, request.getQuestionId());

        int newRevision = previousResponses.isEmpty() ? 1 : previousResponses.get(0).getRevisionSequence() + 1;

        // 2. Build Response entity
        Response response = Response.builder()
                .sessionId(sessionId)
                .questionId(request.getQuestionId())
                .candidateId(candidateId)
                .selectedOptionIds(request.getSelectedOptionIds())
                .enteredValue(request.getEnteredValue())
                .timestamp(request.getTimestamp())
                .cumulativeTimeSpentMs(request.getCumulativeTimeSpentMs())
                .revisionSequence(newRevision)
                .saveSource(request.getSaveSource())
                .isFinal(false)
                .build();
        response.setTenantId(tenantId);

        // 3. Save to DB
        Response saved = responseRepository.save(response);

        // 4. Publish save confirmation to Kafka with acks=all (blocking)
        Map<String, Object> savedEvent = Map.of(
                "responseId", saved.getId(),
                "sessionId", sessionId,
                "questionId", request.getQuestionId(),
                "candidateId", candidateId,
                "revisionSequence", newRevision,
                "saveSource", request.getSaveSource(),
                "savedAt", Instant.now().toString(),
                "tenantId", tenantId
        );

        try {
            kafkaTemplate.send(TOPIC_RESPONSE_SAVED, sessionId.toString(), savedEvent)
                    .get(); // BLOCKING — ensures acks=all before returning to client
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Kafka send interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Kafka send failed", e);
        }

        // 5. Increment the response_save_rate counter
        metricsConfig.getResponseSaveCounter().increment();

        // 6. Return SaveResponseResponse
        log.info("Saved response: sessionId={}, questionId={}, revision={}, saveSource={}",
                sessionId, request.getQuestionId(), newRevision, request.getSaveSource());

        return SaveResponseResponse.builder()
                .responseId(saved.getId())
                .sessionId(sessionId)
                .questionId(request.getQuestionId())
                .revisionSequence(newRevision)
                .saveSource(request.getSaveSource())
                .savedAt(saved.getCreatedAt())
                .build();
    }
}
