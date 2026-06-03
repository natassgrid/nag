package com.examplatform.notification.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens for outbound notification events on the
 * {@code exam.notifications.outbound} topic. Triggers notification
 * delivery via the appropriate channel (Email, Push, In-App).
 */
@Slf4j
@Component
public class NotificationEventConsumer {

    /**
     * Stub listener for notification events. Will be implemented to parse
     * the notification payload and dispatch to the appropriate delivery channel.
     *
     * @param message the raw event payload
     */
    @KafkaListener(topics = "exam.notifications.outbound", groupId = "notification-service")
    public void onNotificationEvent(String message) {
        log.info("Received notification event: {}", message);
        // TODO: Deserialize, determine channel, dispatch to email/push/in-app handler
    }
}
