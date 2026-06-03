package com.examplatform.candidate.client;

import com.examplatform.candidate.dto.DigiLockerResponse;

/**
 * Stub interface for calling DigiLocker API with OAuth2 token.
 * Implementations will integrate with the actual DigiLocker
 * document verification service.
 *
 * Validates: Requirements 1.3
 */
public interface DigiLockerClient {

    /**
     * Fetches a document from DigiLocker for verification.
     *
     * @param token   the OAuth2 access token
     * @param docType the document type to fetch (e.g., "AADHAAR", "PAN")
     * @return the document response from DigiLocker
     */
    DigiLockerResponse fetchDocument(String token, String docType);
}
