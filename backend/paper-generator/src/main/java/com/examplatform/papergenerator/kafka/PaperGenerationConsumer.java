package com.examplatform.papergenerator.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Async Kafka consumer for paper generation request jobs.
 * Listens on topic {@code exam.paper.events} and triggers paper generation
 * workflows when a request is received.
 *
 * Validates: Requirements 8.7
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaperGenerationConsumer {

    @KafkaListener(topics = "exam.paper.events", groupId = "paper-generator")
    public void onPaperGenerationRequest(ConsumerRecord<String, String> record) {
        log.info("Paper generation request received: key={}", record.key());
        // Stub — full implementation in task 7.2
    }
}
