package com.examplatform.audit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the Audit WAL (Write-Ahead Log) buffer.
 * Controls the local file-based buffer used when Kafka is unavailable.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "audit.wal")
public class AuditWalProperties {

    /**
     * Path where WAL files are stored.
     */
    private String walPath = "./audit-wal/";

    /**
     * Whether the WAL buffer is enabled.
     */
    private boolean enabled = true;

    /**
     * Maximum size of a single WAL file in bytes (default: 10MB).
     */
    private long maxFileSize = 10 * 1024 * 1024L;
}
