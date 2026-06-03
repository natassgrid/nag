package com.examplatform.response.service;

import com.examplatform.response.domain.Response;
import com.examplatform.response.repository.ResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Service for retrieving the full revision history of responses for an exam session.
 * Returns all responses ordered by questionId + revisionSequence for audit/evaluation purposes.
 *
 * Validates: Requirements 10.5
 */
@Service
@RequiredArgsConstructor
public class ResponseHistoryService {

    private final ResponseRepository responseRepository;

    /**
     * Retrieves all responses for a session ordered by questionId + revisionSequence.
     *
     * @param sessionId the exam session ID
     * @param tenantId  the tenant identifier
     * @return all responses for the session in order
     */
    public List<Response> getSessionResponses(UUID sessionId, String tenantId) {
        List<Response> responses = responseRepository.findBySessionIdAndTenantId(sessionId, tenantId);

        // Sort by questionId then revisionSequence ascending
        return responses.stream()
                .sorted(Comparator
                        .comparing(Response::getQuestionId)
                        .thenComparingInt(Response::getRevisionSequence))
                .toList();
    }
}
