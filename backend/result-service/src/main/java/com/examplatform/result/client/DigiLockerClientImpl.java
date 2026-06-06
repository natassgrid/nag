package com.examplatform.result.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stub implementation of DigiLockerClient for local development.
 * In production, this would make HTTP calls to the DigiLocker API.
 */
@Slf4j
@Component
public class DigiLockerClientImpl implements DigiLockerClient {

    @Override
    public void pushScorecard(UUID candidateId, String pdfRef) {
        log.info("[STUB] Pushing scorecard to DigiLocker for candidate={}, pdfRef={}", candidateId, pdfRef);
        // TODO: Implement actual DigiLocker API integration for production
    }
}
