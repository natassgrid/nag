package com.examplatform.candidate.client;

import com.examplatform.candidate.dto.DigiLockerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stub implementation of DigiLockerClient for local development.
 * In production, this would call the DigiLocker API with OAuth2 tokens.
 */
@Slf4j
@Component
public class DigiLockerClientImpl implements DigiLockerClient {

    @Override
    public DigiLockerResponse fetchDocument(String token, String docType) {
        log.info("[STUB] Fetching document from DigiLocker: docType={}", docType);
        return new DigiLockerResponse("SUCCESS", "STUB_DOC_DATA", docType);
    }
}
