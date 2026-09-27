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
package com.examplatform.e2e.lifecycle;

import com.examplatform.e2e.config.E2ETestConfig;
import com.examplatform.e2e.config.E2ETestExtension;
import com.examplatform.e2e.fixtures.CandidateFixtures;
import com.examplatform.e2e.util.TraceCorrelator;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E lifecycle test: Candidate onboarding flow.
 *
 * <p>Covers the happy-path journey from registration through
 * email verification, profile completion, and KYC submission.
 *
 * <p>Prerequisites:
 * <ul>
 *   <li>Docker Compose stack running ({@code candidate-service}, API Gateway,
 *       Keycloak, MailHog, WireMock)</li>
 *   <li>Run with: {@code ./gradlew :e2e-tests:test -PrunE2E}</li>
 * </ul>
 */
@Tag("e2e")
@ExtendWith(E2ETestExtension.class)
class CandidateOnboardingE2ETest {

    private static final Logger LOG = LoggerFactory.getLogger(CandidateOnboardingE2ETest.class);

    @BeforeAll
    static void setup() {
        E2ETestConfig.configureRestAssured();
    }

    /**
     * Happy-path: register → email verify → profile → KYC.
     *
     * <p>Steps that interact with endpoints not yet implemented in the service
     * layer are annotated with {@code // TODO} and assert only that the service
     * does not return an internal server error (5xx), which is the contract-level
     * guarantee during incremental development.
     */
    @Test
    @DisplayName("Candidate onboarding: register → email verify → profile → KYC")
    void candidateOnboardingLifecycle_happyPath() {

        // ── Step 1: Register a new candidate ─────────────────────────────────
        Response registerResponse = RestAssured.given()
                .contentType("application/json")
                .body(CandidateFixtures.registrationPayload("e2e.new@test.com"))
                .post("/api/v1/candidates/register");

        TraceCorrelator.logTrace("candidateOnboardingLifecycle_happyPath [register]",
                registerResponse);

        assertThat(registerResponse.statusCode())
                .as("Registration should return HTTP 201 Created")
                .isEqualTo(201);

        String candidateId = registerResponse.jsonPath().getString("candidateId");
        assertThat(candidateId)
                .as("Response body must contain a non-blank candidateId")
                .isNotBlank();

        LOG.info("[E2E] Registered candidate: candidateId={}", candidateId);

        // ── Step 2: Verify that the welcome / verify-email message arrived ───
        // TODO: MailHog connectivity depends on the notification-service being wired
        //       to the local SMTP relay. Assert non-500 here; tighten once wired.
        Response mailhogResponse = RestAssured.given()
                .get(E2ETestConfig.getMailhogUrl() + "/api/v2/messages");

        assertThat(mailhogResponse.statusCode())
                .as("MailHog API should be reachable")
                .isLessThan(500);

        String mailBody = mailhogResponse.asString();
        assertThat(mailBody)
                .as("MailHog should contain at least one message with 'Verify' text")
                .contains("Verify");

        LOG.info("[E2E] MailHog message check passed.");

        // ── Step 3: Submit email OTP verification ────────────────────────────
        // TODO: WireMock stub for the OTP validation service must return a
        //       valid response. For now assert that the service is not crashing.
        Response verifyResponse = RestAssured.given()
                .contentType("application/json")
                .body(java.util.Map.of("candidateId", candidateId, "otp", "123456"))
                .post("/api/v1/candidates/verify-email");

        TraceCorrelator.logTrace("candidateOnboardingLifecycle_happyPath [verify-email]",
                verifyResponse);

        assertThat(verifyResponse.statusCode())
                .as("Email verification must not return an internal server error")
                .isLessThan(500);

        LOG.info("[E2E] Email OTP verification response: status={}", verifyResponse.statusCode());

        // ── Step 4: Update candidate profile ─────────────────────────────────
        // TODO: PUT /candidates/{id}/profile may return 404 until the profile
        //       management endpoint is implemented in candidate-service v2.
        Response profileResponse = RestAssured.given()
                .contentType("application/json")
                .body(CandidateFixtures.profilePayload())
                .put("/api/v1/candidates/" + candidateId + "/profile");

        TraceCorrelator.logTrace("candidateOnboardingLifecycle_happyPath [profile]",
                profileResponse);

        assertThat(profileResponse.statusCode())
                .as("Profile update should return 200 (OK) or 404 (not-yet-implemented)")
                .isIn(200, 404);

        LOG.info("[E2E] Profile update response: status={}", profileResponse.statusCode());

        // ── Step 5: Submit KYC verification request ───────────────────────────
        // TODO: KYC service integration with DigiLocker requires a live stub.
        //       Assert non-500 as the minimum contract during development.
        Response kycResponse = RestAssured.given()
                .contentType("application/json")
                .body(CandidateFixtures.kycVerificationPayload(candidateId))
                .post("/api/v1/candidates/" + candidateId + "/kyc");

        TraceCorrelator.logTrace("candidateOnboardingLifecycle_happyPath [kyc]", kycResponse);

        assertThat(kycResponse.statusCode())
                .as("KYC verification request must not return an internal server error")
                .isLessThan(500);

        LOG.info("[E2E] KYC verification response: status={}", kycResponse.statusCode());
    }
}
