package com.examplatform.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for DDoS mitigation.
 * Controls the per-origin rate threshold and the sliding window duration.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "gateway.ddos")
public class DDoSProperties {

    /**
     * Maximum number of requests allowed from a single origin (IP) within the window.
     * Default: 10,000 requests per second.
     */
    private int threshold = 10000;

    /**
     * Duration of the sliding window in seconds.
     * Default: 1 second.
     */
    private int windowSeconds = 1;
}
