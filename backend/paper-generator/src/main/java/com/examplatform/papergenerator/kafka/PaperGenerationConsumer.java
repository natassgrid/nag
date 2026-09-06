/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.papergenerator.kafka;

import com.examplatform.shared.messaging.GenericDomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Async consumer for paper generation request jobs.
 * Listens on topic {@code exam.paper.events} and triggers paper generation
 * workflows when a request is received.
 * Supports Kafka, RabbitMQ, and in-memory Spring events.
 *
 * Validates: Requirements 8.7
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaperGenerationConsumer {

    public static final String PAPER_EVENTS_TOPIC = "exam.paper.events";

    @KafkaListener(topics = PAPER_EVENTS_TOPIC, groupId = "paper-generator")
    public void onKafkaPaperGenerationRequest(ConsumerRecord<String, String> record) {
        log.info("Paper generation request received via Kafka: key={}", record.key());
        // Stub — full implementation in task 7.2
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "paper.generation.queue", durable = "true"),
                    exchange = @Exchange(value = "exam.events", type = ExchangeTypes.TOPIC),
                    key = PAPER_EVENTS_TOPIC
            )
    )
    public void onRabbitPaperGenerationRequest(Object message) {
        log.info("Paper generation request received via RabbitMQ: {}", message);
        // Stub — full implementation in task 7.2
    }

    @EventListener
    public void onSpringPaperGenerationRequest(GenericDomainEvent event) {
        if (PAPER_EVENTS_TOPIC.equals(event.topic())) {
            log.info("Paper generation request received via Spring in-memory event: key={}, payload={}",
                    event.key(), event.payload());
        }
    }
}
