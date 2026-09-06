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

package com.examplatform.shared.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Auto-configuration for platform messaging abstraction.
 * Automatically wires the appropriate {@link EventPublisher} based on active broker configuration.
 */
@AutoConfiguration
public class MessagingAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
    @ConditionalOnProperty(name = "platform.messaging.broker", havingValue = "rabbit")
    static class RabbitMessagingConfiguration {

        @Bean
        @ConditionalOnMissingBean(EventPublisher.class)
        public EventPublisher rabbitEventPublisher(RabbitTemplate rabbitTemplate) {
            return new RabbitEventPublisher(rabbitTemplate);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
    @ConditionalOnProperty(name = "platform.messaging.broker", havingValue = "kafka", matchIfMissing = true)
    static class KafkaMessagingConfiguration {

        @Bean
        @ConditionalOnMissingBean(EventPublisher.class)
        public EventPublisher kafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
            return new KafkaEventPublisher(kafkaTemplate);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class FallbackMessagingConfiguration {

        @Bean
        @ConditionalOnMissingBean(EventPublisher.class)
        public EventPublisher springEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
            return new SpringEventPublisher(applicationEventPublisher);
        }
    }
}
