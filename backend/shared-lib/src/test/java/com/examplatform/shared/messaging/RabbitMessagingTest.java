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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.\n */

package com.examplatform.shared.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitMessagingTest")
class RabbitMessagingTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("RabbitEventPublisher publishes to exam.events exchange with topic routing key")
    void testRabbitEventPublisherPublish() {
        RabbitEventPublisher publisher = new RabbitEventPublisher(rabbitTemplate);
        Map<String, Object> payload = Map.of("eventType", "TEST_EVENT", "value", 42);

        publisher.publish("exam.evaluation.completed", "key-123", payload);

        verify(rabbitTemplate).convertAndSend("exam.events", "exam.evaluation.completed", payload);
    }

    @Test
    @DisplayName("Rabbit topology constants are properly configured")
    void testRabbitTopologyConstants() {
        assertThat(RabbitEventPublisher.EXCHANGE_NAME).isEqualTo("exam.events");
        assertThat(RabbitEventPublisher.DEAD_LETTER_EXCHANGE).isEqualTo("exam.events.dlx");
        assertThat(RabbitEventPublisher.DEAD_LETTER_QUEUE).isEqualTo("exam.events.dlq");
    }
}
