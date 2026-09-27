/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) — Open Digital Public Infrastructure (DPI) Platform
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
 */
package com.examplatform.e2e.fixtures;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Factory class providing reusable candidate-related request payloads for E2E tests.
 *
 * <p>All methods return {@code Map<String, Object>} instances that REST Assured
 * serialises to JSON automatically when the Content-Type is {@code application/json}.
 *
 * <p>Values are representative test data — not real PII.
 */
public final class CandidateFixtures {

    private CandidateFixtures() {
        // factory class — no instances
    }

    /**
     * Builds a candidate registration request payload.
     *
     * @param email unique email address for the new candidate
     * @return map representing the registration request body
     */
    public static Map<String, Object> registrationPayload(String email) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "E2E Test Candidate");
        body.put("email", email);
        body.put("phone", "+919876543210");
        body.put("password", "Password@123");
        return body;
    }

    /**
     * Builds a candidate profile update payload with representative demographic data.
     *
     * @return map representing the profile update request body
     */
    public static Map<String, Object> profilePayload() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateOfBirth", "1995-06-15");
        body.put("gender", "MALE");
        body.put("address", "42, Test Street, E2E Nagar");
        body.put("city", "Bengaluru");
        body.put("state", "Karnataka");
        body.put("pincode", "560001");
        return body;
    }

    /**
     * Builds a KYC verification request payload using Aadhaar via DigiLocker.
     *
     * @param candidateId the UUID of the candidate to be verified
     * @return map representing the KYC verification request body
     */
    public static Map<String, Object> kycVerificationPayload(String candidateId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("candidateId", candidateId);
        body.put("documentType", "AADHAAR");
        body.put("verificationSource", "DIGILOCKER");
        return body;
    }
}
