package com.examplatform.result.client;

import java.util.UUID;

/**
 * Client interface for DigiLocker integration.
 * Pushes scorecard PDFs to the government's DigiLocker service
 * so candidates can access their results digitally.
 *
 * Validates: Requirements 13.5
 */
public interface DigiLockerClient {

    /**
     * Pushes a scorecard PDF reference to DigiLocker for a candidate.
     *
     * @param candidateId the candidate's unique identifier
     * @param pdfRef      the reference/URL to the generated scorecard PDF
     */
    void pushScorecard(UUID candidateId, String pdfRef);
}
