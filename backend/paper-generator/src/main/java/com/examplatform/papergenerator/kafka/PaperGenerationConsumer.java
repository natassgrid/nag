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
