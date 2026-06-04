package com.examplatform.delivery.client;

import com.examplatform.delivery.dto.CandidateExtension;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stub implementation of CandidateProfileClient for local development.
 * In production, this would call the candidate-service REST API.
 */
@Slf4j
@Component
public class CandidateProfileClientImpl implements CandidateProfileClient {

    @Override
    public CandidateExtension getExtension(UUID candidateId, String tenantId) {
        log.info("[STUB] Getting candidate extension: candidate={}, tenant={}", candidateId, tenantId);
        return null; // No extension by default
    }
}
