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
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.examplatform.notification.service.NotificationDeliveryHelper.findCurrent;

/**
 * Handles Web Push and FCM push notification delivery with 3-attempt retry logic
 * and fallback to UNDELIVERED on permanent failure.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final NotificationRepository notificationRepository;

    @Setter
    private RestTemplate restTemplate = new RestTemplate();

    @Setter
    private ObjectMapper objectMapper = new ObjectMapper();

    @Value("${notification.push.endpoint:http://localhost:3000/fcm/send}")
    private String pushEndpoint;

    @Value("${notification.push.server-key:mock-fcm-server-key}")
    private String serverKey;

    private static final int MAX_RETRIES = 3;

    /**
     * Attempt Push notification delivery with up to 3 retries.
     */
    @Async
    public void deliver(Notification notification) {
        String token = notification.getFcmToken();
        UUID notificationId = notification.getId();

        if (token == null || token.isBlank()) {
            log.warn("Cannot deliver Push notification {}: missing FCM / device token", notificationId);
            Notification current = findCurrent(notificationId, notification);
            current.setStatus(NotificationStatus.UNDELIVERED);
            notificationRepository.save(current);
            return;
        }

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "key=" + serverKey);

                Map<String, Object> payload = new HashMap<>();
                payload.put("to", token);

                Map<String, String> notificationMap = new HashMap<>();
                notificationMap.put("title", notification.getSubject() != null ? notification.getSubject() : "Exam Platform Alert");
                notificationMap.put("body", notification.getBody());
                payload.put("notification", notificationMap);

                Map<String, String> dataMap = new HashMap<>();
                dataMap.put("notificationId", notificationId != null ? notificationId.toString() : "");
                dataMap.put("type", notification.getType() != null ? notification.getType().name() : "PUSH");
                payload.put("data", dataMap);

                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(pushEndpoint, requestEntity, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    String externalId = extractMessageId(response.getBody());
                    Notification current = findCurrent(notificationId, notification);
                    current.setStatus(NotificationStatus.SENT);
                    current.setSentAt(Instant.now());
                    current.setRetryCount(attempt);
                    current.setExternalMessageId(externalId);
                    notificationRepository.save(current);
                    log.info("Push notification delivered successfully for notification {} on attempt {}, externalId={}",
                            notificationId, attempt, externalId);
                    return;
                } else {
                    throw new RuntimeException("Push Gateway returned HTTP " + response.getStatusCode());
                }

            } catch (Exception e) {
                notification.setRetryCount(attempt);
                log.warn("Push notification delivery failed for notification {} on attempt {}/{}: {}",
                        notificationId, attempt, MAX_RETRIES, e.getMessage());

                if (attempt == MAX_RETRIES) {
                    Notification current = findCurrent(notificationId, notification);
                    current.setStatus(NotificationStatus.UNDELIVERED);
                    current.setRetryCount(MAX_RETRIES);
                    notificationRepository.save(current);
                    log.error("ALERT: Push notification permanently UNDELIVERED after {} attempts for notification {}",
                            MAX_RETRIES, notificationId);
                }
            }
        }
    }

    private Notification findCurrent(UUID notificationId, Notification fallback) {
        if (notificationId != null) {
            return notificationRepository.findById(notificationId).orElse(fallback);
        }
        return fallback;
    }

    private String extractMessageId(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "PUSH-" + Instant.now().toEpochMilli();
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("results") && root.get("results").isArray() && !root.get("results").isEmpty()) {
                return root.get("results").get(0).path("message_id").asText();
            }
            if (root.has("messageId")) {
                return root.get("messageId").asText();
            }
            if (root.has("name")) {
                return root.get("name").asText();
            }
        } catch (Exception ignored) {
        }
        return "PUSH-" + Instant.now().toEpochMilli();
    }
}
