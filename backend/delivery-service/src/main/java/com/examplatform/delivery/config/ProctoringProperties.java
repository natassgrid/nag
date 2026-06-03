package com.examplatform.delivery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the proctoring subsystem.
 *
 * Validates: Requirements 11.1, 11.2
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.proctoring")
public class ProctoringProperties {

    /**
     * Minimum interval between webcam captures in seconds.
     * Must be at least 30 seconds to limit bandwidth and storage.
     */
    private int captureIntervalSeconds = 30;

    /**
     * Number of days to retain proctoring snapshots before deletion.
     */
    private int retentionDays = 90;

    /**
     * Maximum number of full-screen exits before the session is flagged.
     */
    private int maxFullScreenExits = 3;
}
