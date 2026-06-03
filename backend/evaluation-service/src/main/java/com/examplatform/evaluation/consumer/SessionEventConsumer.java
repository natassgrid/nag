package com.examplatform.evaluation.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens for session-submitted events on the
 * {@code exam.session.events} topic. Triggers evaluation workflow
 * when a candidate submits their exam session.
 */
@Slf4j
@Component
public class SessionEventConsumer {

    /**
     * Stub listener for session events. Will be implemented to trigger
     * auto-evaluation for objective questions and queue manual evaluation
     * for subjective questions.
     *
     * @param message the raw event payload
     */
    @KafkaListener(topics = "exam.session.events", groupId = "evaluation-service")
    public void onSessionEvent(String message) {
        log.info("Received session event: {}", message);
        // TODO: Parse event, determine if session-submitted, trigger evaluation pipeline
    }
}
