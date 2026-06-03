package com.examplatform.response.service;

import com.examplatform.response.domain.Response;
import com.examplatform.response.repository.ResponseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ResponseHistoryService — revision history retrieval.
 *
 * Validates: Requirements 10.5
 */
@ExtendWith(MockitoExtension.class)
class ResponseHistoryServiceTest {

    @Mock
    private ResponseRepository responseRepository;

    private ResponseHistoryService responseHistoryService;

    private UUID sessionId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        responseHistoryService = new ResponseHistoryService(responseRepository);
        sessionId = UUID.randomUUID();
        tenantId = "tenant-board-1";
    }

    @Test
    @DisplayName("Returns all revisions ordered by questionId + revisionSequence")
    void returnsAllRevisionsInCorrectOrder() {
        UUID questionA = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID questionB = UUID.fromString("00000000-0000-0000-0000-000000000002");

        // Create responses in random order
        List<Response> unorderedResponses = new ArrayList<>(List.of(
                buildResponse(questionB, 2),
                buildResponse(questionA, 3),
                buildResponse(questionA, 1),
                buildResponse(questionB, 1),
                buildResponse(questionA, 2)
        ));

        when(responseRepository.findBySessionIdAndTenantId(sessionId, tenantId))
                .thenReturn(unorderedResponses);

        List<Response> results = responseHistoryService.getSessionResponses(sessionId, tenantId);

        assertThat(results).hasSize(5);

        // First 3 should be questionA, ordered by revision
        assertThat(results.get(0).getQuestionId()).isEqualTo(questionA);
        assertThat(results.get(0).getRevisionSequence()).isEqualTo(1);
        assertThat(results.get(1).getQuestionId()).isEqualTo(questionA);
        assertThat(results.get(1).getRevisionSequence()).isEqualTo(2);
        assertThat(results.get(2).getQuestionId()).isEqualTo(questionA);
        assertThat(results.get(2).getRevisionSequence()).isEqualTo(3);

        // Last 2 should be questionB, ordered by revision
        assertThat(results.get(3).getQuestionId()).isEqualTo(questionB);
        assertThat(results.get(3).getRevisionSequence()).isEqualTo(1);
        assertThat(results.get(4).getQuestionId()).isEqualTo(questionB);
        assertThat(results.get(4).getRevisionSequence()).isEqualTo(2);
    }

    @Test
    @DisplayName("Returns empty list when no responses exist for the session")
    void returnsEmptyListWhenNoResponses() {
        when(responseRepository.findBySessionIdAndTenantId(sessionId, tenantId))
                .thenReturn(List.of());

        List<Response> results = responseHistoryService.getSessionResponses(sessionId, tenantId);

        assertThat(results).isEmpty();
    }

    private Response buildResponse(UUID questionId, int revisionSequence) {
        return Response.builder()
                .sessionId(sessionId)
                .questionId(questionId)
                .candidateId(UUID.randomUUID())
                .revisionSequence(revisionSequence)
                .saveSource("AUTO")
                .timestamp(Instant.now())
                .cumulativeTimeSpentMs(1000L * revisionSequence)
                .isFinal(false)
                .build();
    }
}
