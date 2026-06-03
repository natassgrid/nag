package com.examplatform.audit.service;

import com.examplatform.audit.config.AuditWalProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Wraps AuditIngestionService to provide a Write-Ahead Log (WAL) buffer.
 * When Kafka is unavailable, audit events are written to local WAL files
 * instead of blocking exam operations. A scheduled task replays buffered
 * entries when Kafka reconnects.
 *
 * Validates: design error-handling
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditWalBufferService {

    private final AuditIngestionService auditIngestionService;
    private final AuditWalProperties walProperties;
    private final ObjectMapper objectMapper;

    /**
     * Buffers an audit event to the WAL when Kafka ingestion fails.
     * This ensures exam operations are never blocked by audit failures.
     *
     * @param eventPayload      raw JSON payload
     * @param eventType         type of audit event
     * @param actorId           ID of the actor
     * @param resource          resource being acted upon
     * @param ipAddress         IP address (nullable)
     * @param deviceFingerprint device fingerprint (nullable)
     * @param tenantId          tenant identifier
     */
    public void bufferEvent(String eventPayload, String eventType, String actorId,
                            String resource, String ipAddress, String deviceFingerprint,
                            String tenantId) {
        if (!walProperties.isEnabled()) {
            log.warn("WAL buffer is disabled; audit event dropped: type={}, actor={}", eventType, actorId);
            return;
        }

        try {
            WalEntry entry = new WalEntry(eventPayload, eventType, actorId,
                    resource, ipAddress, deviceFingerprint, tenantId);
            String json = objectMapper.writeValueAsString(entry);
            writeToWal(json);
            log.info("Buffered audit event to WAL: type={}, actor={}", eventType, actorId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize WAL entry: {}", e.getMessage());
        }
    }

    /**
     * Scheduled task that replays WAL entries when Kafka reconnects.
     * Runs every 30 seconds and attempts to re-ingest buffered events.
     */
    @Scheduled(fixedDelay = 30_000)
    public void replayWalEntries() {
        if (!walProperties.isEnabled()) {
            return;
        }

        Path walDir = Paths.get(walProperties.getWalPath());
        if (!Files.exists(walDir)) {
            return;
        }

        List<Path> walFiles = listWalFiles(walDir);
        if (walFiles.isEmpty()) {
            return;
        }

        log.info("Replaying {} WAL file(s)", walFiles.size());

        for (Path walFile : walFiles) {
            try {
                List<String> lines = Files.readAllLines(walFile);
                boolean allReplayed = true;

                for (String line : lines) {
                    if (line.isBlank()) continue;
                    try {
                        WalEntry entry = objectMapper.readValue(line, WalEntry.class);
                        auditIngestionService.ingest(
                                entry.eventPayload(), entry.eventType(), entry.actorId(),
                                entry.resource(), entry.ipAddress(), entry.deviceFingerprint(),
                                entry.tenantId());
                    } catch (Exception e) {
                        log.warn("Failed to replay WAL entry, will retry later: {}", e.getMessage());
                        allReplayed = false;
                        break;
                    }
                }

                if (allReplayed) {
                    Files.deleteIfExists(walFile);
                    log.info("Successfully replayed and deleted WAL file: {}", walFile.getFileName());
                }
            } catch (IOException e) {
                log.error("Error reading WAL file {}: {}", walFile, e.getMessage());
            }
        }
    }

    /**
     * Writes a JSON line to the current WAL file.
     */
    private void writeToWal(String json) {
        try {
            Path walDir = Paths.get(walProperties.getWalPath());
            Files.createDirectories(walDir);

            Path walFile = walDir.resolve("wal-current.log");

            // Check file size and rotate if needed
            if (Files.exists(walFile) && Files.size(walFile) >= walProperties.getMaxFileSize()) {
                Path rotated = walDir.resolve("wal-" + System.currentTimeMillis() + ".log");
                Files.move(walFile, rotated);
            }

            Files.writeString(walFile, json + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Failed to write to WAL file: {}", e.getMessage());
        }
    }

    /**
     * Lists all WAL files in the directory, sorted by name.
     */
    private List<Path> listWalFiles(Path walDir) {
        try (Stream<Path> paths = Files.list(walDir)) {
            return paths
                    .filter(p -> p.toString().endsWith(".log"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            log.error("Failed to list WAL files: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Internal record representing a buffered WAL entry.
     */
    record WalEntry(
            String eventPayload,
            String eventType,
            String actorId,
            String resource,
            String ipAddress,
            String deviceFingerprint,
            String tenantId
    ) {}
}
