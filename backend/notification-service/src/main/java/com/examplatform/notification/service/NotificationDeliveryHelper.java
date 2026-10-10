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

package com.examplatform.notification.service;

import com.examplatform.notification.domain.Notification;
import com.examplatform.notification.domain.Notification.NotificationStatus;
import com.examplatform.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Shared helper methods for notification delivery services (SMS, Push, WhatsApp).
 * Consolidates duplicated entity lookup, status recording, retry handling, and message ID extraction across delivery channels.
 */
@Slf4j
@UtilityClass
public class NotificationDeliveryHelper {

    /**
     * Resolves the current persisted notification record from repository, or returns fallback.
     */
    public static Notification findCurrent(NotificationRepository repository, UUID notificationId, Notification fallback) {
        if (notificationId != null && repository != null) {
            return repository.findById(notificationId).orElse(fallback);
        }
        return fallback;
    }

    /**
     * Records a successful delivery and persists status update.
     */
    public static Notification recordSuccess(
            NotificationRepository repository,
            UUID notificationId,
            Notification fallback,
            int attempt,
            String externalId
    ) {
        return recordSuccess(repository, notificationId, fallback, attempt, externalId, null);
    }

    /**
     * Records a successful delivery with custom template identifier and persists status update.
     */
    public static Notification recordSuccess(
            NotificationRepository repository,
            UUID notificationId,
            Notification fallback,
            int attempt,
            String externalId,
            String templateId
    ) {
        Notification current = findCurrent(repository, notificationId, fallback);
        current.setStatus(NotificationStatus.SENT);
        current.setSentAt(Instant.now());
        current.setRetryCount(attempt);
        current.setExternalMessageId(externalId);
        if (templateId != null) {
            current.setTemplateId(templateId);
        }
        return repository.save(current);
    }

    /**
     * Records a permanent undelivered failure after exhaustion of retries.
     */
    public static Notification recordFailure(
            NotificationRepository repository,
            UUID notificationId,
            Notification fallback,
            int maxRetries
    ) {
        Notification current = findCurrent(repository, notificationId, fallback);
        current.setStatus(NotificationStatus.UNDELIVERED);
        current.setRetryCount(maxRetries);
        return repository.save(current);
    }

    /**
     * Executes an HTTP notification delivery action with retry logic and logs status updates.
     *
     * @param repository         notification repository for persisting state
     * @param notification       target notification
     * @param channelName        human-readable channel name for logging (e.g. "Push notification", "WhatsApp", "SMS")
     * @param maxRetries         maximum retry attempts
     * @param deliveryAction     supplier executing the HTTP request
     * @param messageIdExtractor function extracting the external message ID from the response body
     * @param templateId         optional template ID to record on success
     */
    public static void executeHttpDeliveryWithRetry(
            NotificationRepository repository,
            Notification notification,
            String channelName,
            int maxRetries,
            Supplier<ResponseEntity<String>> deliveryAction,
            Function<String, String> messageIdExtractor,
            String templateId
    ) {
        UUID notificationId = notification.getId();
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ResponseEntity<String> response = deliveryAction.get();
                if (response != null && response.getStatusCode().is2xxSuccessful()) {
                    String externalId = messageIdExtractor != null
                            ? messageIdExtractor.apply(response.getBody())
                            : null;
                    recordSuccess(repository, notificationId, notification, attempt, externalId, templateId);
                    log.info("{} delivered successfully for notification {} on attempt {}, externalId={}",
                            channelName, notificationId, attempt, externalId);
                    return;
                } else {
                    var status = response != null ? response.getStatusCode() : "UNKNOWN";
                    throw new IllegalStateException(channelName + " Gateway returned HTTP " + status);
                }
            } catch (Exception e) {
                notification.setRetryCount(attempt);
                log.warn("{} delivery failed for notification {} on attempt {}/{}: {}",
                        channelName, notificationId, attempt, maxRetries, e.getMessage());

                if (attempt == maxRetries) {
                    recordFailure(repository, notificationId, notification, maxRetries);
                    log.error("ALERT: {} permanently UNDELIVERED after {} attempts for notification {}",
                            channelName, maxRetries, notificationId);
                }
            }
        }
    }

    /**
     * Extracts an external message identifier from a gateway JSON response body with fallback generation.
     *
     * @param objectMapper   JSON object mapper
     * @param responseBody   gateway response string
     * @param fallbackPrefix prefix for timestamp-based fallback (e.g. "PUSH", "WA", "SMS")
     * @return the extracted or generated external message identifier
     */
    public static String extractMessageId(
            ObjectMapper objectMapper,
            String responseBody,
            String fallbackPrefix
    ) {
        if (responseBody == null || responseBody.isBlank()) {
            return fallbackPrefix + "-" + Instant.now().toEpochMilli();
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("results") && root.get("results").isArray() && !root.get("results").isEmpty()) {
                JsonNode first = root.get("results").get(0);
                if (first.has("message_id")) {
                    return first.get("message_id").asText();
                }
                if (first.has("id")) {
                    return first.get("id").asText();
                }
            }
            if (root.has("messages") && root.get("messages").isArray() && !root.get("messages").isEmpty()) {
                JsonNode first = root.get("messages").get(0);
                if (first.has("id")) {
                    return first.get("id").asText();
                }
                if (first.has("message_id")) {
                    return first.get("message_id").asText();
                }
            }
            if (root.has("request_id")) {
                return root.get("request_id").asText();
            }
            if (root.has("message_id")) {
                return root.get("message_id").asText();
            }
            if (root.has("messageId")) {
                return root.get("messageId").asText();
            }
            if (root.has("id")) {
                return root.get("id").asText();
            }
            if (root.has("name")) {
                return root.get("name").asText();
            }
        } catch (Exception ignored) {
        }
        return fallbackPrefix + "-" + Instant.now().toEpochMilli();
    }
}
