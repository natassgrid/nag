package com.examplatform.notification.consumer;

import com.examplatform.notification.service.NotificationProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens for outbound notification events on the
 * {@code exam.notifications.outbound} topic. Delegates processing to
 * {@link NotificationProcessingService} which handles notification creation
 * and delivery dispatch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationProcessingService notificationProcessingService;

    /**
     * Consumes notification events from Kafka and delegates to the processing service.
     * The processing service handles parsing, notification creation, and delivery dispatch.
     *
     * @param message the raw event payload (JSON)
     */
    @KafkaListener(topics = "exam.notifications.outbound", groupId = "notification-service")
    public void onNotificationEvent(String message) {
        log.info("Received notification event: {}", message);
        notificationProcessingService.processEvent(message);
    }
}
