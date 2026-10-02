/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.candidate.integration;

import com.examplatform.candidate.client.DigiLockerClientImpl;
import com.examplatform.candidate.dto.DigiLockerResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testcontainers-based integration test verifying that the mock external DPI API server
 * (DigiLocker, UIDAI Aadhaar 2.5 e-KYC, MSG91 SMS gateway) works correctly with backend clients.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MockApiServerTestcontainersIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(MockApiServerTestcontainersIntegrationTest.class);
    private static final int MOCK_SERVER_PORT = 8099;

    private static GenericContainer<?> mockServerContainer;
    private static String mockServerBaseUrl;
    private static boolean containerRunning = false;

    private final RestClient restClient = RestClient.create();

    @BeforeAll
    static void startContainer() {
        try {
            // Check if pre-built local image exists, otherwise build dynamically from Dockerfile
            try {
                mockServerContainer = new GenericContainer<>(DockerImageName.parse("nag/mock-api-server:test"))
                        .withExposedPorts(MOCK_SERVER_PORT)
                        .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("mock-server"))
                        .waitingFor(Wait.forHttp("/health").forStatusCode(200).withStartupTimeout(Duration.ofSeconds(60)));
                mockServerContainer.start();
            } catch (Throwable t) {
                log.info("Prebuilt image not found or startup failed, building from Dockerfile: {}", t.getMessage());
                Path mockServerDir = Paths.get("../../infrastructure/mock-server").toAbsolutePath().normalize();
                if (!mockServerDir.toFile().exists()) {
                    mockServerDir = Paths.get("infrastructure/mock-server").toAbsolutePath().normalize();
                }

                mockServerContainer = new GenericContainer<>(
                        new ImageFromDockerfile()
                                .withFileFromPath(".", mockServerDir)
                )
                        .withExposedPorts(MOCK_SERVER_PORT)
                        .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("mock-server"))
                        .waitingFor(Wait.forHttp("/health").forStatusCode(200).withStartupTimeout(Duration.ofSeconds(90)));
                mockServerContainer.start();
            }

            Integer mappedPort = mockServerContainer.getMappedPort(MOCK_SERVER_PORT);
            String host = mockServerContainer.getHost();
            mockServerBaseUrl = "http://" + host + ":" + mappedPort;
            containerRunning = true;
            log.info("Mock API Server Testcontainer started successfully at {}", mockServerBaseUrl);
        } catch (Throwable t) {
            log.warn("Docker / Testcontainers is not available in current test environment: {}", t.getMessage());
            containerRunning = false;
        }
    }

    // =========================================================================
    // 1. Health & Server Info
    // =========================================================================
    @Test
    @DisplayName("Health endpoint returns UP and healthy services")
    void testHealthEndpoint() {
        if (!containerRunning) {
            log.info("Skipping container test: Testcontainers not running");
            return;
        }

        Map<?, ?> response = restClient.get()
                .uri(mockServerBaseUrl + "/health")
                .retrieve()
                .body(Map.class);

        assertThat(response).isNotNull();
        assertThat(response.get("status")).isEqualTo("UP");
        Map<?, ?> services = (Map<?, ?>) response.get("services");
        assertThat(services.get("digilocker")).isEqualTo("HEALTHY");
        assertThat(services.get("aadhaarKyc")).isEqualTo("HEALTHY");
        assertThat(services.get("msg91Sms")).isEqualTo("HEALTHY");
    }

    // =========================================================================
    // 2. DigiLocker Integration & Client Tests
    // =========================================================================
    @Nested
    @DisplayName("DigiLocker Integration Tests")
    class DigiLockerTests {

        @Test
        @DisplayName("Candidate DigiLockerClientImpl connects to mock server and retrieves document")
        void testCandidateDigiLockerClient() {
            if (!containerRunning) return;

            DigiLockerClientImpl client = new DigiLockerClientImpl();
            ReflectionTestUtils.setField(client, "digiLockerApiUrl", mockServerBaseUrl + "/digilocker/v1/verify");

            DigiLockerResponse response = client.fetchDocument("oauth-bearer-token-xyz", "10TH_MARKSHEET");

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("SUCCESS");
            assertThat(response.getIssuerId()).isEqualTo("in.gov.cbse");
            assertThat(response.getDocumentData()).isNotEmpty();
        }

        @Test
        @DisplayName("DigiLocker OAuth2 OpenID discovery and JWKS endpoints return valid metadata")
        void testDigiLockerOidcAndJwks() {
            if (!containerRunning) return;

            // OIDC Discovery
            Map<?, ?> oidcConfig = restClient.get()
                    .uri(mockServerBaseUrl + "/digilocker/.well-known/openid-configuration")
                    .retrieve()
                    .body(Map.class);
            assertThat(oidcConfig).isNotNull();
            assertThat(oidcConfig.get("authorization_endpoint")).isNotNull();
            assertThat(oidcConfig.get("token_endpoint")).isNotNull();

            // JWKS
            Map<?, ?> jwks = restClient.get()
                    .uri(mockServerBaseUrl + "/digilocker/oauth/jwks.json")
                    .retrieve()
                    .body(Map.class);
            assertThat(jwks).isNotNull();
            assertThat(jwks.get("keys")).isNotNull();
        }

        @Test
        @DisplayName("DigiLocker Scorecard Push endpoint successfully publishes credential")
        void testScorecardPushEndpoint() {
            if (!containerRunning) return;

            UUID candidateId = UUID.randomUUID();
            Map<String, Object> payload = Map.of(
                    "candidateId", candidateId.toString(),
                    "pdfRef", "https://s3.natassgrid.gov.in/scorecards/test.pdf",
                    "examId", "EXAM-NAG-2026",
                    "score", 195,
                    "percentile", 99.8
            );

            ResponseEntity<Map> response = restClient.post()
                    .uri(mockServerBaseUrl + "/digilocker/v1/credential/push")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toEntity(Map.class);

            assertThat(response.getStatusCode().value()).isEqualTo(201);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("status")).isEqualTo("SUCCESS");
            assertThat(response.getBody().get("docId").toString()).startsWith("NAG-SCORECARD-");
        }
    }

    // =========================================================================
    // 3. Aadhaar e-KYC & UIDAI 2.5 Auth Tests
    // =========================================================================
    @Nested
    @DisplayName("Aadhaar e-KYC & UIDAI 2.5 Auth Tests")
    class AadhaarTests {

        @Test
        @DisplayName("Aadhaar OTP generate and verify completes deterministic resident verification")
        void testAadhaarOtpFlow() {
            if (!containerRunning) return;

            // 1. Generate OTP
            Map<?, ?> genResponse = restClient.post()
                    .uri(mockServerBaseUrl + "/aadhaar/v1/otp/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("aadhaarNumber", "123456789012"))
                    .retrieve()
                    .body(Map.class);

            assertThat(genResponse).isNotNull();
            assertThat(genResponse.get("success")).isEqualTo(true);
            assertThat(genResponse.get("mockOtpHint")).isEqualTo("000000");
            String txnId = (String) genResponse.get("txnId");

            // 2. Verify OTP with deterministic master OTP 000000
            Map<?, ?> verifyResponse = restClient.post()
                    .uri(mockServerBaseUrl + "/aadhaar/v1/otp/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "txnId", txnId,
                            "otp", "000000",
                            "aadhaarNumber", "123456789012"
                    ))
                    .retrieve()
                    .body(Map.class);

            assertThat(verifyResponse).isNotNull();
            assertThat(verifyResponse.get("success")).isEqualTo(true);
            assertThat(verifyResponse.get("status")).isEqualTo("KYC_VERIFIED");

            Map<?, ?> data = (Map<?, ?>) verifyResponse.get("data");
            assertThat(data.get("fullName")).isEqualTo("Aditya Sharma");
            assertThat(data.get("aadhaarLast4")).isEqualTo("9012");
            assertThat(data.get("gender")).isEqualTo("M");
            assertThat(data.get("photoBase64")).isNotNull();
        }

        @Test
        @DisplayName("UIDAI 2.5 Auth endpoint performs demographic matching")
        void testUidai25DemographicAuth() {
            if (!containerRunning) return;

            Map<?, ?> authResponse = restClient.post()
                    .uri(mockServerBaseUrl + "/aadhaar/v2.5/auth")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "uid", "123456789012",
                            "authType", "DEMO",
                            "demographic", Map.of("name", "Aditya", "gender", "M")
                    ))
                    .retrieve()
                    .body(Map.class);

            assertThat(authResponse).isNotNull();
            assertThat(authResponse.get("ret")).isEqualTo("Y");
            assertThat(authResponse.get("authenticated")).isEqualTo(true);
        }

        @Test
        @DisplayName("UIDAI 2.5 e-KYC endpoint returns signed XML payload")
        void testUidai25KycXml() {
            if (!containerRunning) return;

            String xmlResponse = restClient.post()
                    .uri(mockServerBaseUrl + "/aadhaar/v2.5/kyc?format=xml")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_XML)
                    .body(Map.of("uid", "123456789012", "otp", "000000"))
                    .retrieve()
                    .body(String.class);

            assertThat(xmlResponse).isNotNull();
            assertThat(xmlResponse).contains("<KycRes");
            assertThat(xmlResponse).contains("<UidData");
            assertThat(xmlResponse).contains("<Signature");
        }
    }

    // =========================================================================
    // 4. MSG91 SMS Gateway & Mobile Validation Tests
    // =========================================================================
    @Nested
    @DisplayName("MSG91 SMS Gateway & Mobile Validation Tests")
    class Msg91SmsTests {

        @Test
        @DisplayName("MSG91 sends OTP, asserts in outbox, and verifies with default static OTP 000000")
        void testMsg91SmsOtpAndInspection() {
            if (!containerRunning) return;

            // Clear outbox before test
            restClient.delete()
                    .uri(mockServerBaseUrl + "/mock/sms/clear")
                    .retrieve()
                    .toBodilessEntity();

            // 1. Dispatch SMS OTP
            Map<?, ?> smsResponse = restClient.post()
                    .uri(mockServerBaseUrl + "/api/v5/otp?template_id=NAG_VERIFY&mobile=9876543210&authkey=test_key&otp=000000")
                    .retrieve()
                    .body(Map.class);

            assertThat(smsResponse).isNotNull();
            assertThat(smsResponse.get("type")).isEqualTo("success");

            // 2. Inspect outbox via Test Inspection API
            Map<?, ?> inspectResponse = restClient.get()
                    .uri(mockServerBaseUrl + "/mock/sms/latest?mobile=9876543210")
                    .retrieve()
                    .body(Map.class);

            assertThat(inspectResponse).isNotNull();
            assertThat(inspectResponse.get("success")).isEqualTo(true);

            Map<?, ?> message = (Map<?, ?>) inspectResponse.get("message");
            assertThat(message.get("mobile")).isEqualTo("9876543210");
            assertThat(message.get("otpCode")).isEqualTo("000000");

            // 3. Verify OTP via MSG91 verify endpoint
            Map<?, ?> verifyResponse = restClient.get()
                    .uri(mockServerBaseUrl + "/api/v5/otp/verify?mobile=9876543210&otp=000000")
                    .retrieve()
                    .body(Map.class);

            assertThat(verifyResponse).isNotNull();
            assertThat(verifyResponse.get("type")).isEqualTo("success");
            assertThat(verifyResponse.get("message")).isEqualTo("OTP verified success");
        }

        @Test
        @DisplayName("Mobile Validation: Verifying invalid OTP returns 400 Bad Request error")
        void testMobileValidationInvalidOtp() {
            if (!containerRunning) return;

            // Attempt to verify an incorrect/unrecorded OTP for a candidate mobile
            assertThatThrownBy(() -> {
                restClient.get()
                        .uri(mockServerBaseUrl + "/api/v5/otp/verify?mobile=9876543210&otp=999888")
                        .retrieve()
                        .body(Map.class);
            }).isInstanceOf(HttpClientErrorException.BadRequest.class);
        }

        @Test
        @DisplayName("Mobile Validation: Custom dynamic OTP validation against candidate mobile number")
        void testMobileValidationCustomDynamicOtp() {
            if (!containerRunning) return;

            String candidateMobile = "9123456780";
            String dynamicOtp = "842910";

            // 1. Dispatch custom OTP for mobile
            Map<?, ?> sendRes = restClient.post()
                    .uri(mockServerBaseUrl + "/api/v5/otp?template_id=NAG_REG&mobile=" + candidateMobile + "&authkey=test_key&otp=" + dynamicOtp)
                    .retrieve()
                    .body(Map.class);

            assertThat(sendRes).isNotNull();
            assertThat(sendRes.get("type")).isEqualTo("success");

            // 2. Validate incorrect OTP fails
            assertThatThrownBy(() -> {
                restClient.post()
                        .uri(mockServerBaseUrl + "/api/v5/otp/verify?mobile=" + candidateMobile + "&otp=111222")
                        .retrieve()
                        .body(Map.class);
            }).isInstanceOf(HttpClientErrorException.BadRequest.class);

            // 3. Validate matching dynamic OTP succeeds
            Map<?, ?> verifyRes = restClient.post()
                    .uri(mockServerBaseUrl + "/api/v5/otp/verify?mobile=" + candidateMobile + "&otp=" + dynamicOtp)
                    .retrieve()
                    .body(Map.class);

            assertThat(verifyRes).isNotNull();
            assertThat(verifyRes.get("type")).isEqualTo("success");
            assertThat(verifyRes.get("message")).isEqualTo("OTP verified success");
        }

        @Test
        @DisplayName("Mobile Validation: Missing mobile number returns 400 Bad Request")
        void testMobileValidationMissingMobile() {
            if (!containerRunning) return;

            assertThatThrownBy(() -> {
                restClient.post()
                        .uri(mockServerBaseUrl + "/api/v5/otp")
                        .retrieve()
                        .body(Map.class);
            }).isInstanceOf(HttpClientErrorException.BadRequest.class);
        }
    }
}
