// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.client;

import com.examplatform.practice.dto.explanation.ExplanationIrDto;
import com.examplatform.practice.dto.explanation.GenerateExplanationRequestDto;
import com.examplatform.practice.dto.explanation.QuestionDetailsDto;
import com.examplatform.practice.service.ExplanationCacheService;
import com.examplatform.shared.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

/**
 * Resilient implementation of AnvayaPrajnaClient equipped with Resilience4j
 * Circuit Breaker, Retry, and Redis L2 Caching (Issue #348).
 */
@Slf4j
@Component
public class AnvayaPrajnaClientImpl implements AnvayaPrajnaClient {

    private final RestClient restClient;
    private final ExplanationCacheService cacheService;
    private final QuestionBankClient questionBankClient;
    private final ObjectMapper objectMapper;

    public AnvayaPrajnaClientImpl(
            @Value("${anvaya-prajna.base-url:http://localhost:8080}") String baseUrl,
            @Autowired(required = false) RestClient.Builder restClientBuilder,
            ExplanationCacheService cacheService,
            @Autowired(required = false) QuestionBankClient questionBankClient,
            @Autowired(required = false) ObjectMapper objectMapper) {
        RestClient.Builder builder = (restClientBuilder != null) ? restClientBuilder : RestClient.builder();
        this.restClient = builder.baseUrl(baseUrl).build();
        this.cacheService = cacheService;
        this.questionBankClient = questionBankClient;
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
    }

    @Override
    @CircuitBreaker(name = "anvayaPrajna", fallbackMethod = "fallbackGetExplanation")
    @Retry(name = "anvayaPrajna")
    public Optional<ExplanationIrDto> getExplanation(UUID questionId) {
        if (questionId == null) {
            return Optional.empty();
        }

        // 1. Check Redis L2 Cache
        Optional<ExplanationIrDto> cached = cacheService.getCachedExplanation(questionId.toString());
        if (cached.isPresent()) {
            log.debug("Found explanation in cache for question {}", questionId);
            return cached;
        }

        // 2. Fetch from downstream Anvaya-Prajna engine
        try {
            ExplanationIrDto explanation = restClient.get()
                    .uri("/api/v1/explanations/{questionId}", questionId)
                    .headers(this::applyAuthHeaders)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(ExplanationIrDto.class);

            if (explanation != null) {
                cacheService.cacheExplanation(questionId.toString(), explanation);
                return Optional.of(explanation);
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.info("Explanation not pre-computed for question: {}, attempting on-demand generation", questionId);
        }

        // 3. Fallback: On-demand generation if question statement can be resolved
        GenerateExplanationRequestDto genReq = buildGenerateRequest(questionId);
        if (genReq != null) {
            return generateExplanation(genReq);
        }

        return Optional.empty();
    }

    @Override
    @CircuitBreaker(name = "anvayaPrajna", fallbackMethod = "fallbackGenerateExplanation")
    @Retry(name = "anvayaPrajna")
    public Optional<ExplanationIrDto> generateExplanation(GenerateExplanationRequestDto request) {
        if (request == null || request.getQuestion() == null) {
            return Optional.empty();
        }

        String qId = request.getQuestion().getQuestionId();

        // Check cache first
        if (qId != null) {
            Optional<ExplanationIrDto> cached = cacheService.getCachedExplanation(qId);
            if (cached.isPresent()) {
                return cached;
            }
        }

        try {
            ExplanationIrDto response = restClient.post()
                    .uri("/api/v1/explanations/generate")
                    .headers(this::applyAuthHeaders)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ExplanationIrDto.class);

            if (response != null && qId != null) {
                cacheService.cacheExplanation(qId, response);
            }
            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.warn("Downstream explanation generation failed: {}", e.getMessage());
            throw e;
        }
    }

    public Optional<ExplanationIrDto> fallbackGetExplanation(UUID questionId, Throwable t) {
        log.warn("Anvaya-Prajna engine fallback triggered for question {}: {}",
                questionId, t != null ? t.getMessage() : "Unknown");
        if (questionId != null) {
            return cacheService.getCachedExplanation(questionId.toString());
        }
        return Optional.empty();
    }

    public Optional<ExplanationIrDto> fallbackGenerateExplanation(GenerateExplanationRequestDto request, Throwable t) {
        log.warn("Anvaya-Prajna generation fallback triggered: {}",
                t != null ? t.getMessage() : "Unknown");
        if (request != null && request.getQuestion() != null && request.getQuestion().getQuestionId() != null) {
            return cacheService.getCachedExplanation(request.getQuestion().getQuestionId());
        }
        return Optional.empty();
    }

    private void applyAuthHeaders(HttpHeaders headers) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            headers.setBearerAuth(jwtAuth.getToken().getTokenValue());
        }
        String tenant = TenantContext.getTenantId();
        if (tenant != null && !tenant.isBlank()) {
            headers.set("X-Tenant-Id", tenant);
        }
    }

    private GenerateExplanationRequestDto buildGenerateRequest(UUID questionId) {
        if (questionBankClient != null) {
            try {
                var answerKeys = questionBankClient.getAnswerKeys(java.util.List.of(questionId));
                if (answerKeys != null && answerKeys.containsKey(questionId)) {
                    var ak = answerKeys.get(questionId);
                    String statement = (ak.content() != null && !ak.content().isBlank())
                            ? ak.content()
                            : (ak.explanation() != null ? ak.explanation() : "Solve question " + questionId);
                    QuestionDetailsDto q = QuestionDetailsDto.builder()
                            .questionId(questionId.toString())
                            .statement(statement)
                            .authoritativeAnswer(ak.answerKey())
                            .domain(ak.subject() != null ? ak.subject().toUpperCase() : "GENERAL_EXAM")
                            .difficulty(ak.difficulty())
                            .build();
                    return GenerateExplanationRequestDto.builder()
                            .question(q)
                            .build();
                }
            } catch (Exception e) {
                log.debug("Could not resolve question from questionBankClient: {}", e.getMessage());
            }
        }
        return null;
    }
}
