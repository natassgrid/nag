package com.examplatform.response.service;

import com.examplatform.response.domain.Response;
import com.examplatform.response.dto.BulkSaveRequest;
import com.examplatform.response.dto.SaveResponseRequest;
import com.examplatform.response.dto.SaveResponseResponse;
import com.examplatform.response.repository.ResponseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BulkSaveService — offline buffer reconciliation.
 *
 * Validates: Requirements 10.4
 */
@ExtendWith(MockitoExtension.class)
class BulkSaveServiceTest {

    @Mock
    private ResponseRepository responseRepository;

    private BulkSaveService bulkSaveService;

    private UUID sessionId;
    private UUID candidateId;
    private UUID questionId1;
    private UUID questionId2;
    private String tenantId;

    @BeforeEach
    void setUp() {
        bulkSaveService = new BulkSaveService(responseRepository);

        sessionId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        questionId1 = UUID.randomUUID();
        questionId2 = UUID.randomUUID();
        tenantId = "tenant-board-1";
    }

    @Test
    @DisplayName("Saves new responses when revisionSequence > server max")
    void savesNewResponses() {
        // Server has no existing responses for questionId1
        when(responseRepository.findBySessionIdAndQuestionIdOrderByRevisionSequenceDesc(sessionId, questionId1))
                .thenReturn(List.of());

        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> {
            Response r = invocation.getArgument(0);
            // Simulate prePersist setting id and createdAt
            try {
                var idField = r.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(r, UUID.randomUUID());
                var createdField = r.getClass().getSuperclass().getDeclaredField("createdAt");
                createdField.setAccessible(true);
                createdField.set(r, Instant.now());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return r;
        });

        BulkSaveRequest request = BulkSaveRequest.builder()
                .responses(List.of(
                        SaveResponseRequest.builder()
                                .questionId(questionId1)
                                .selectedOptionIds("[\"A\"]")
                                .timestamp(Instant.now())
                                .cumulativeTimeSpentMs(5000)
                                .saveSource("OFFLINE")
                                .revisionSequence(1)
                                .build()
                ))
                .build();

        List<SaveResponseResponse> results = bulkSaveService.bulkSave(sessionId, request, candidateId, tenantId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRevisionSequence()).isEqualTo(1);
        assertThat(results.get(0).getSessionId()).isEqualTo(sessionId);
        verify(responseRepository, times(1)).save(any(Response.class));
    }

    @Test
    @DisplayName("Skips already-persisted responses when revisionSequence <= server max")
    void skipsAlreadySavedResponses() {
        // Server already has revision 3 for questionId1
        Response existingResponse = Response.builder()
                .sessionId(sessionId)
                .questionId(questionId1)
                .candidateId(candidateId)
                .revisionSequence(3)
                .saveSource("AUTO")
                .timestamp(Instant.now())
                .cumulativeTimeSpentMs(3000)
                .isFinal(false)
                .build();

        when(responseRepository.findBySessionIdAndQuestionIdOrderByRevisionSequenceDesc(sessionId, questionId1))
                .thenReturn(List.of(existingResponse));

        BulkSaveRequest request = BulkSaveRequest.builder()
                .responses(List.of(
                        SaveResponseRequest.builder()
                                .questionId(questionId1)
                                .selectedOptionIds("[\"A\"]")
                                .timestamp(Instant.now())
                                .cumulativeTimeSpentMs(2000)
                                .saveSource("OFFLINE")
                                .revisionSequence(2) // <= server max of 3
                                .build()
                ))
                .build();

        List<SaveResponseResponse> results = bulkSaveService.bulkSave(sessionId, request, candidateId, tenantId);

        assertThat(results).isEmpty();
        verify(responseRepository, never()).save(any(Response.class));
    }

    @Test
    @DisplayName("Reconciles correctly: saves new, skips already persisted")
    void reconcilesMixedResponses() {
        // questionId1: server has revision 2
        Response existing1 = Response.builder()
                .sessionId(sessionId)
                .questionId(questionId1)
                .candidateId(candidateId)
                .revisionSequence(2)
                .saveSource("AUTO")
                .timestamp(Instant.now())
                .cumulativeTimeSpentMs(1000)
                .isFinal(false)
                .build();
        when(responseRepository.findBySessionIdAndQuestionIdOrderByRevisionSequenceDesc(sessionId, questionId1))
                .thenReturn(List.of(existing1));

        // questionId2: server has no responses
        when(responseRepository.findBySessionIdAndQuestionIdOrderByRevisionSequenceDesc(sessionId, questionId2))
                .thenReturn(List.of());

        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> {
            Response r = invocation.getArgument(0);
            try {
                var idField = r.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(r, UUID.randomUUID());
                var createdField = r.getClass().getSuperclass().getDeclaredField("createdAt");
                createdField.setAccessible(true);
                createdField.set(r, Instant.now());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return r;
        });

        BulkSaveRequest request = BulkSaveRequest.builder()
                .responses(List.of(
                        // This should be SKIPPED (revision 1 <= server max 2)
                        SaveResponseRequest.builder()
                                .questionId(questionId1)
                                .selectedOptionIds("[\"B\"]")
                                .timestamp(Instant.now())
                                .cumulativeTimeSpentMs(1000)
                                .saveSource("OFFLINE")
                                .revisionSequence(1)
                                .build(),
                        // This should be SAVED (revision 3 > server max 2)
                        SaveResponseRequest.builder()
                                .questionId(questionId1)
                                .selectedOptionIds("[\"C\"]")
                                .timestamp(Instant.now())
                                .cumulativeTimeSpentMs(2000)
                                .saveSource("OFFLINE")
                                .revisionSequence(3)
                                .build(),
                        // This should be SAVED (revision 1 > server max 0)
                        SaveResponseRequest.builder()
                                .questionId(questionId2)
                                .enteredValue("42")
                                .timestamp(Instant.now())
                                .cumulativeTimeSpentMs(500)
                                .saveSource("OFFLINE")
                                .revisionSequence(1)
                                .build()
                ))
                .build();

        List<SaveResponseResponse> results = bulkSaveService.bulkSave(sessionId, request, candidateId, tenantId);

        // 2 saved, 1 skipped
        assertThat(results).hasSize(2);
        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(responseRepository, times(2)).save(captor.capture());

        List<Response> savedResponses = captor.getAllValues();
        assertThat(savedResponses.get(0).getRevisionSequence()).isEqualTo(3);
        assertThat(savedResponses.get(1).getQuestionId()).isEqualTo(questionId2);
    }
}
