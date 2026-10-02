/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.identity.integration;

import com.examplatform.identity.config.SmsProperties;
import com.examplatform.identity.repository.OtpVerificationRepository;
import com.examplatform.identity.service.Msg91SmsService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Testcontainers & Mock Server API integration test suite for Identity Service.
 * Verifies MSG91 SMS gateway integration, Mock Email endpoints, OTP inspection outboxes,
 * and chaos injection scenarios using the mock third-party API server.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdentityMockApiServerIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(IdentityMockApiServerIntegrationTest.class);
    private static final int MOCK_SERVER_PORT = 8099;

    private static GenericContainer<?> mockServerContainer;
    private static String mockServerBaseUrl;
    private static boolean serverAvailable = false;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeAll
    static void initMockServer() {
        // 1. Check if local mock server is already running on port 8099
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(800)).build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:8099/health"))
                    .timeout(Duration.ofMillis(800))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                mockServerBaseUrl = "http://127.0.0.1:8099";
                serverAvailable = true;
                log.info("Found existing Mock API Server running at {}", mockServerBaseUrl);
                return;
            }
        } catch (Exception ignored) {
            // Local instance not running, proceed to Testcontainers
        }

        // 2. Launch via Testcontainers
        try {
            try {
                mockServerContainer = new GenericContainer<>(DockerImageName.parse("nag/mock-api-server:test"))
                        .withExposedPorts(MOCK_SERVER_PORT)
                        .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("mock-server"))
                        .waitingFor(Wait.forHttp("/health").forStatusCode(200).withStartupTimeout(Duration.ofSeconds(60)));
                mockServerContainer.start();
            } catch (Throwable t) {
                log.info("Prebuilt image nag/mock-api-server:test not found, building from Dockerfile: {}", t.getMessage());
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
            serverAvailable = true;
            log.info("Mock API Server Testcontainer started successfully at {}", mockServerBaseUrl);
        } catch (Throwable t) {
            log.warn("Testcontainers / Docker is not available in current test environment: {}", t.getMessage());
            serverAvailable = false;
        }
    }

    @BeforeEach
    void resetMockServerState() {
        if (!serverAvailable) return;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(mockServerBaseUrl + "/mock/reset"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();
            httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.warn("Failed to reset mock server state: {}", e.getMessage());
        }
    }

    // =========================================================================
    // 1. Health & Server Info
    // =========================================================================
    @Test
    @DisplayName("Health endpoint confirms UP status and registered DPI services")
    void testHealthEndpoint() throws Exception {
        if (!serverAvailable) {
            log.info("Skipping test: Mock server not available");
            return;
        }

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(mockServerBaseUrl + "/health"))
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).contains("\"status\":\"UP\"");
        assertThat(resp.body()).contains("\"msg91Sms\":\"HEALTHY\"");
        assertThat(resp.body()).contains("\"emailGateway\":\"HEALTHY\"");
    }

    // =========================================================================
    // 2. MSG91 SMS Service Integration
    // =========================================================================
    @Nested
    @DisplayName("MSG91 SMS Gateway Integration Tests")
    class Msg91SmsGatewayTests {

        @Test
        @DisplayName("+ve: Msg91SmsService successfully delivers SMS OTP to mock server and updates outbox")
        void testSendSmsOtpToMockServer() throws Exception {
            if (!serverAvailable) return;

            SmsProperties properties = new SmsProperties();
            properties.setEnabled(true);
            properties.getMsg91().setAuthKey("test-authkey-12345");
            properties.getMsg91().setTemplateId("NAG_AUTH_OTP");
            properties.getMsg91().setSenderId("NAGDPI");
            properties.getMsg91().setApiUrl(mockServerBaseUrl + "/api/v5/otp");

            OtpVerificationRepository otpRepo = mock(OtpVerificationRepository.class);
            Msg91SmsService smsService = new Msg91SmsService(properties, otpRepo);

            String rawMobile = "+919876543210";
            String otpCode = "543210";

            boolean success = smsService.sendSmsOtp(rawMobile, otpCode);
            assertThat(success).isTrue();

            // Assert mock server outbox contains the message
            HttpRequest inspectReq = HttpRequest.newBuilder()
                    .uri(URI.create(mockServerBaseUrl + "/mock/sms/latest?mobile=9876543210"))
                    .GET()
                    .build();
            HttpResponse<String> inspectResp = httpClient.send(inspectReq, HttpResponse.BodyHandlers.ofString());

            assertThat(inspectResp.statusCode()).isEqualTo(200);
            assertThat(inspectResp.body()).contains("\"otpCode\":\"543210\"");
            assertThat(inspectResp.body()).contains("9876543210");
        }

        @Test
        @DisplayName("+ve: Verify OTP against MSG91 mock verification endpoint")
        void testVerifyOtpOnMockServer() throws Exception {
            if (!serverAvailable) return;

            // 1. Dispatch custom OTP
            HttpRequest dispatchReq = HttpRequest.newBuilder()
                    .uri(URI.create(mockServerBaseUrl + "/api/v5/otp?template_id=NAG_OTP&mobile=919988776655&authkey=auth123&otp=876543"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> dispatchResp = httpClient.send(dispatchReq, HttpResponse.BodyHandlers.ofString());
            assertThat(dispatchResp.statusCode()).isEqualTo(200);

            // 2. Validate OTP
            HttpRequest verifyReq = HttpRequest.newBuilder()
                    .uri(URI.create(mockServerBaseUrl + "/api/v5/otp/verify?mobile=919988776655&otp=876543"))
                    .GET()
                    .build();
            HttpResponse<String> verifyResp = httpClient.send(verifyReq, HttpResponse.BodyHandlers.ofString());
            assertThat(verifyResp.statusCode()).isEqualTo(200);
            assertThat(verifyResp.body()).contains("\"type\":\"success\"");
        }

        @Test
        @DisplayName("+ve: Clear SMS outbox via mock inspector API")
        void testClearSmsOutbox() throws Exception {
            if (!serverAvailable) return;

            // Send an SMS first
            HttpRequest dispatchReq = HttpRequest.newBuilder()
                    .uri(URI.create(mockServerBaseUrl + "/api/v5/otp?template_id=NAG_OTP&mobile=9876543210&authkey=auth123&otp=123456"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            httpClient.send(dispatchReq, HttpResponse.BodyHandlers.ofString());

            // Clear
            HttpRequest clearReq = HttpRequest.newBuilder()
                    .uri(URI.create(mockServerBaseUrl + "/mock/sms/clear"))
                    .DELETE()
                    .build();
            HttpResponse<String> clearResp = httpClient.send(clearReq, HttpResponse.BodyHandlers.ofString());
            assertThat(clearResp.statusCode()).isEqualTo(200);

            // Inspect should be 404 (empty)
            HttpRequest inspectReq = HttpRequest.newBuilder()
                    .uri(URI.create(mockServerBaseUrl + "/mock/sms/latest"))
                    .GET()
                    .build();
            HttpResponse<String> inspectResp = httpClient.send(inspectReq, HttpResponse.BodyHandlers.ofString());
            assertThat(inspectResp.statusCode()).isEqualTo(404);
        }
    }

    // =========================================================================
    // 3. Mock Email Gateway Integration
    // =========================================================================
    @Nested
    @DisplayName("Email Gateway Mock Integration Tests")
    class EmailGatewayMockTests {

        @Test
        @DisplayName("+ve: Email dispatch records OTP code to mock outbox and allows automated retrieval")
        void testEmailSendAndInspect() throws Exception {
            if (!serverAvailable) return;

            String candidateEmail = "recovery.candidate@natassgrid.gov.in";
            String jsonPayload = """
                    {
                        "to": "recovery.candidate@natassgrid.gov.in",
                        "subject": "National Assessment Grid - Email Verification OTP",
                        "text": "Your NAG account verification code is 789456. This code is valid for 10 minutes.",
                        "otp": "789456"
                    }
                    """;

            HttpRequest sendReq = HttpRequest.newBuilder()
                    .uri(URI.create(mockServerBaseUrl + "/email/send"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            HttpResponse<String> sendResp = httpClient.send(sendReq, HttpResponse.BodyHandlers.ofString());

            assertThat(sendResp.statusCode()).isEqualTo(200);
            assertThat(sendResp.body()).contains("\"type\":\"success\"");
            assertThat(sendResp.body()).contains("\"otpCode\":\"789456\"");

            // Retrieve from outbox
            HttpRequest inspectReq = HttpRequest.newBuilder()
                    .uri(URI.create(mockServerBaseUrl + "/mock/email/latest?email=" + candidateEmail))
                    .GET()
                    .build();
            HttpResponse<String> inspectResp = httpClient.send(inspectReq, HttpResponse.BodyHandlers.ofString());

            assertThat(inspectResp.statusCode()).isEqualTo(200);
            assertThat(inspectResp.body()).contains("\"otpCode\":\"789456\"");
            assertThat(inspectResp.body()).contains(candidateEmail);
        }

        @Test
        @DisplayName("+ve: Clear email outbox via mock inspector API")
        void testClearEmailOutbox() throws Exception {
            if (!serverAvailable) return;

            HttpRequest clearReq = HttpRequest.newBuilder()
                    .uri(URI.create(mockServerBaseUrl + "/mock/email/clear"))
                    .DELETE()
                    .build();
            HttpResponse<String> clearResp = httpClient.send(clearReq, HttpResponse.BodyHandlers.ofString());
            assertThat(clearResp.statusCode()).isEqualTo(200);

            HttpRequest inspectReq = HttpRequest.newBuilder()
                    .uri(URI.create(mockServerBaseUrl + "/mock/email/latest"))
                    .GET()
                    .build();
            HttpResponse<String> inspectResp = httpClient.send(inspectReq, HttpResponse.BodyHandlers.ofString());
            assertThat(inspectResp.statusCode()).isEqualTo(404);
        }
    }

    // =========================================================================
    // 4. Chaos & Outage Simulation Tests
    // =========================================================================
    @Nested
    @DisplayName("Chaos & Outage Simulation Tests")
    class ChaosSimulationTests {

        @Test
        @DisplayName("-ve: Msg91SmsService gracefully handles upstream 503 SMS gateway outage")
        void testMsg91ServiceHandlesUpstreamOutage() throws Exception {
            if (!serverAvailable) return;

            // Inject 503 error on MSG91
            HttpRequest chaosReq = HttpRequest.newBuilder()
                    .uri(URI.create(mockServerBaseUrl + "/mock/chaos"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"msg91FailureStatus\": 503}"))
                    .build();
            HttpResponse<String> chaosResp = httpClient.send(chaosReq, HttpResponse.BodyHandlers.ofString());
            assertThat(chaosResp.statusCode()).isEqualTo(200);

            SmsProperties properties = new SmsProperties();
            properties.setEnabled(true);
            properties.getMsg91().setAuthKey("test-authkey");
            properties.getMsg91().setTemplateId("NAG_OTP");
            properties.getMsg91().setApiUrl(mockServerBaseUrl + "/api/v5/otp");

            OtpVerificationRepository otpRepo = mock(OtpVerificationRepository.class);
            Msg91SmsService smsService = new Msg91SmsService(properties, otpRepo);

            boolean result = smsService.sendSmsOtp("+919876543210", "123456");
            // Must return false without throwing unhandled runtime exception
            assertThat(result).isFalse();

            // Reset chaos
            HttpRequest resetChaosReq = HttpRequest.newBuilder()
                    .uri(URI.create(mockServerBaseUrl + "/mock/chaos"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"reset\": true}"))
                    .build();
            httpClient.send(resetChaosReq, HttpResponse.BodyHandlers.ofString());
        }
    }
}
