package com.examplatform.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables async method execution for notification delivery.
 * Email delivery uses @Async to avoid blocking the Kafka consumer thread.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
