package com.examplatform.delivery.client;

import com.examplatform.delivery.dto.CandidateExtension;

import java.util.UUID;

/**
 * Client interface for retrieving candidate profile details from the candidate-service.
 * Implementations may use REST (WebClient/RestClient) or gRPC for inter-service communication.
 */
public interface CandidateProfileClient {

    /**
     * Retrieve the disability extension configuration for a candidate within a tenant.
     *
     * @param candidateId the candidate's unique identifier
     * @param tenantId    the tenant (examination authority) identifier
     * @return the candidate's extension details, or null if no extension is configured
     */
    CandidateExtension getExtension(UUID candidateId, String tenantId);
}
