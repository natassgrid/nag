package com.examplatform.audit.service;

import com.examplatform.audit.config.AuditWalProperties;
import com.examplatform.audit.domain.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuditWalBufferService.
 * Validates: design error-handling
 */
@ExtendWith(MockitoExtension.class)
class AuditWalBufferServiceTest {

    @Mock
    private AuditIngestionService auditIngestionService;

    @TempDir
    Path tempDir;

    private AuditWalProperties walProperties;
    private ObjectMapper objectMapper;
    private AuditWalBufferService walBufferService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        walProperties = new AuditWalProperties();
        walProperties.setWalPath(tempDir.toString());
        walProperties.setEnabled(true);
        walProperties.setMaxFileSize(10 * 1024 * 1024L);

        walBufferService = new AuditWalBufferService(auditIngestionService, walProperties, objectMapper);
    }

    @Test
    @DisplayName("bufferEvent writes event to WAL file when enabled")
    void bufferEvent_writesToWalFile() throws IOException {
        walBufferService.bufferEvent(
                "{\"action\":\"LOGIN\"}", "USER_LOGIN",
                UUID.randomUUID().toString(), "auth:session",
                "192.168.1.1", "fp-abc123", "tenant-test"
        );

        Path walFile = tempDir.resolve("wal-current.log");
        assertThat(walFile).exists();

        String content = Files.readString(walFile);
        assertThat(content).contains("USER_LOGIN");
        assertThat(content).contains("auth:session");
        assertThat(content).contains("tenant-test");
    }

    @Test
    @DisplayName("bufferEvent does not write when WAL is disabled")
    void bufferEvent_doesNotWrite_whenDisabled() {
        walProperties.setEnabled(false);

        walBufferService.bufferEvent(
                "{\"action\":\"LOGIN\"}", "USER_LOGIN",
                UUID.randomUUID().toString(), "auth:session",
                null, null, "tenant-test"
        );

        Path walFile = tempDir.resolve("wal-current.log");
        assertThat(walFile).doesNotExist();
    }

    @Test
    @DisplayName("replayWalEntries ingests buffered events and removes WAL file")
    void replayWalEntries_ingestsAndCleansUp() throws IOException {
        String actorId = UUID.randomUUID().toString();
        AuditEvent mockEvent = AuditEvent.builder()
                .eventType("SESSION_STARTED")
                .actorId(UUID.fromString(actorId))
                .resource("exam:session:123")
                .occurredAt(Instant.now())
                .tenantId("tenant-test")
                .build();

        when(auditIngestionService.ingest(
                anyString(), eq("SESSION_STARTED"), eq(actorId),
                eq("exam:session:123"), eq("10.0.0.1"), eq("fp-xyz"), eq("tenant-test")))
                .thenReturn(mockEvent);

        // Write a WAL entry manually
        walBufferService.bufferEvent(
                "{\"sessionId\":\"123\"}", "SESSION_STARTED",
                actorId, "exam:session:123",
                "10.0.0.1", "fp-xyz", "tenant-test"
        );

        Path walFile = tempDir.resolve("wal-current.log");
        assertThat(walFile).exists();

        // Replay
        walBufferService.replayWalEntries();

        // Verify the event was ingested
        verify(auditIngestionService).ingest(
                eq("{\"sessionId\":\"123\"}"), eq("SESSION_STARTED"), eq(actorId),
                eq("exam:session:123"), eq("10.0.0.1"), eq("fp-xyz"), eq("tenant-test"));

        // WAL file should be deleted after successful replay
        assertThat(walFile).doesNotExist();
    }

    @Test
    @DisplayName("replayWalEntries does nothing when WAL is disabled")
    void replayWalEntries_doesNothing_whenDisabled() {
        walProperties.setEnabled(false);

        walBufferService.replayWalEntries();

        verify(auditIngestionService, never()).ingest(
                anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("replayWalEntries does nothing when no WAL files exist")
    void replayWalEntries_doesNothing_whenNoFiles() {
        walBufferService.replayWalEntries();

        verify(auditIngestionService, never()).ingest(
                anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString());
    }
}
