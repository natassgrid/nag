// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.client;

import com.examplatform.practice.dto.AnswerKeyDto;
import com.examplatform.practice.dto.explanation.ExplanationIrDto;
import com.examplatform.practice.dto.explanation.GenerateExplanationRequestDto;
import com.examplatform.practice.dto.explanation.QuestionDetailsDto;
import com.examplatform.practice.service.ExplanationCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(MockitoExtension.class)
class AnvayaPrajnaClientTest {

    @Mock
    private ExplanationCacheService cacheService;

    @Mock
    private QuestionBankClient questionBankClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private AnvayaPrajnaClientImpl client;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = new AnvayaPrajnaClientImpl(
                "http://localhost:8080",
                restClientBuilder,
                cacheService,
                questionBankClient,
                objectMapper
        );
    }

    @Test
    @DisplayName("getExplanation - Returns cached explanation directly without HTTP call")
    void testGetExplanationCacheHit() {
        UUID qId = UUID.randomUUID();
        ExplanationIrDto cached = ExplanationIrDto.builder()
                .explanationId("exp-cached-1")
                .questionId(qId.toString())
                .status("APPROVED")
                .build();

        when(cacheService.getCachedExplanation(qId.toString())).thenReturn(Optional.of(cached));

        Optional<ExplanationIrDto> result = client.getExplanation(qId);

        assertThat(result).isPresent();
        assertThat(result.get().getExplanationId()).isEqualTo("exp-cached-1");
        verify(cacheService).getCachedExplanation(qId.toString());
        mockServer.verify(); // No HTTP requests made
    }

    @Test
    @DisplayName("getExplanation - Fetches downstream on cache miss and caches result")
    void testGetExplanationCacheMissHttpSuccess() throws Exception {
        UUID qId = UUID.randomUUID();
        when(cacheService.getCachedExplanation(qId.toString())).thenReturn(Optional.empty());

        ExplanationIrDto responseDto = ExplanationIrDto.builder()
                .explanationId("exp-downstream-1")
                .questionId(qId.toString())
                .status("APPROVED")
                .build();

        mockServer.expect(requestTo("http://localhost:8080/api/v1/explanations/" + qId))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseDto), MediaType.APPLICATION_JSON));

        Optional<ExplanationIrDto> result = client.getExplanation(qId);

        assertThat(result).isPresent();
        assertThat(result.get().getExplanationId()).isEqualTo("exp-downstream-1");
        verify(cacheService).cacheExplanation(eq(qId.toString()), any(ExplanationIrDto.class));
        mockServer.verify();
    }

    @Test
    @DisplayName("getExplanation - Falls back to on-demand generation when 404 returned")
    void testGetExplanationFallbackToGenerateOn404() throws Exception {
        UUID qId = UUID.randomUUID();
        when(cacheService.getCachedExplanation(qId.toString())).thenReturn(Optional.empty());

        AnswerKeyDto answerKey = new AnswerKeyDto(
                qId,
                "42",
                "NUMERICAL",
                "topic-1",
                "Algebra",
                "MEDIUM",
                4,
                "What is 20 + 22?",
                null,
                "Simple addition problem",
                "MATHEMATICS"
        );
        when(questionBankClient.getAnswerKeys(List.of(qId))).thenReturn(Map.of(qId, answerKey));

        // 1st call: GET returns 404
        mockServer.expect(requestTo("http://localhost:8080/api/v1/explanations/" + qId))
                .andRespond(withResourceNotFound());

        // 2nd call: POST /api/v1/explanations/generate returns generated explanation
        ExplanationIrDto generated = ExplanationIrDto.builder()
                .explanationId("exp-generated-1")
                .questionId(qId.toString())
                .status("APPROVED")
                .build();

        mockServer.expect(requestTo("http://localhost:8080/api/v1/explanations/generate"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(generated), MediaType.APPLICATION_JSON));

        Optional<ExplanationIrDto> result = client.getExplanation(qId);

        assertThat(result).isPresent();
        assertThat(result.get().getExplanationId()).isEqualTo("exp-generated-1");
        mockServer.verify();
    }

    @Test
    @DisplayName("fallbackGetExplanation - Gracefully falls back to cache or empty on downstream outage")
    void testFallbackGetExplanation() {
        UUID qId = UUID.randomUUID();
        ExplanationIrDto cached = ExplanationIrDto.builder().explanationId("exp-fallback").build();
        when(cacheService.getCachedExplanation(qId.toString())).thenReturn(Optional.of(cached));

        Optional<ExplanationIrDto> result = client.fallbackGetExplanation(qId, new RuntimeException("Engine 503"));
        assertThat(result).isPresent();
        assertThat(result.get().getExplanationId()).isEqualTo("exp-fallback");

        // When cache is empty
        when(cacheService.getCachedExplanation(qId.toString())).thenReturn(Optional.empty());
        Optional<ExplanationIrDto> emptyResult = client.fallbackGetExplanation(qId, new RuntimeException("Engine 503"));
        assertThat(emptyResult).isEmpty();
    }

    @Test
    @DisplayName("fallbackGenerateExplanation - Gracefully falls back to cache or empty on generation failure")
    void testFallbackGenerateExplanation() {
        GenerateExplanationRequestDto req = GenerateExplanationRequestDto.builder()
                .question(QuestionDetailsDto.builder().questionId("q-test").build())
                .build();

        when(cacheService.getCachedExplanation("q-test")).thenReturn(Optional.empty());
        Optional<ExplanationIrDto> result = client.fallbackGenerateExplanation(req, new RuntimeException("Timeout"));
        assertThat(result).isEmpty();
    }
}
