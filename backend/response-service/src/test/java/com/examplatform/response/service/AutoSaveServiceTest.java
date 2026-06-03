package com.examplatform.response.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AutoSaveService.
 * Validates: Requirements 10.2, 10.3
 */
@ExtendWith(MockitoExtension.class)
class AutoSaveServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ResponseSaveService responseSaveService;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AutoSaveService autoSaveService;

    @Test
    @DisplayName("Auto-save triggered on schedule when pending changes exist")
    void autoSavePendingChanges_withPendingChanges_triggersSave() {
        UUID sessionId = UUID.randomUUID();
        String pendingKey = "pending-changes:" + sessionId;

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("active-sessions")).thenReturn(Set.of(sessionId.toString()));
        when(redisTemplate.hasKey(pendingKey)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(pendingKey)).thenReturn("pending-data");
        when(redisTemplate.delete(pendingKey)).thenReturn(true);

        autoSaveService.autoSavePendingChanges();

        verify(redisTemplate).delete(pendingKey);
    }

    @Test
    @DisplayName("Auto-save does nothing when no active sessions exist")
    void autoSavePendingChanges_noActiveSessions_doesNothing() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("active-sessions")).thenReturn(Set.of());

        autoSaveService.autoSavePendingChanges();

        verify(redisTemplate, never()).hasKey(anyString());
    }

    @Test
    @DisplayName("Auto-save does nothing when no pending changes")
    void autoSavePendingChanges_noPendingChanges_doesNothing() {
        UUID sessionId = UUID.randomUUID();
        String pendingKey = "pending-changes:" + sessionId;

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("active-sessions")).thenReturn(Set.of(sessionId.toString()));
        when(redisTemplate.hasKey(pendingKey)).thenReturn(false);

        autoSaveService.autoSavePendingChanges();

        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("triggerSaveForSession returns true when pending changes found")
    void triggerSaveForSession_pendingExists_returnsTrue() {
        UUID sessionId = UUID.randomUUID();
        String pendingKey = "pending-changes:" + sessionId;

        when(redisTemplate.hasKey(pendingKey)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(pendingKey)).thenReturn("data");
        when(redisTemplate.delete(pendingKey)).thenReturn(true);

        boolean result = autoSaveService.triggerSaveForSession(sessionId);

        assertThat(result).isTrue();
        verify(redisTemplate).delete(pendingKey);
    }

    @Test
    @DisplayName("triggerSaveForSession returns false when no pending changes")
    void triggerSaveForSession_noPending_returnsFalse() {
        UUID sessionId = UUID.randomUUID();
        String pendingKey = "pending-changes:" + sessionId;

        when(redisTemplate.hasKey(pendingKey)).thenReturn(false);

        boolean result = autoSaveService.triggerSaveForSession(sessionId);

        assertThat(result).isFalse();
    }
}
