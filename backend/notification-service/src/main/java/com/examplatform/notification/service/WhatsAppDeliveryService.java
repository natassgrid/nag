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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.examplatform.notification.service.NotificationDeliveryHelper.findCurrent;

/**
 * Handles WhatsApp Business API delivery with 3-attempt retry logic
 * and fallback to UNDELIVERED on permanent failure.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppDeliveryService {

    private final NotificationRepository notificationRepository;

    @Setter
    private RestTemplate restTemplate = new RestTemplate();

    @Setter
    private ObjectMapper objectMapper = new ObjectMapper();

    @Value("${notification.whatsapp.endpoint:http://localhost:3000/whatsapp/v1/messages}")
    private String whatsappEndpoint;

    @Value("${notification.whatsapp.access-token:mock-wa-access-token}")
    private String accessToken;

    private static final int MAX_RETRIES = 3;

    /**
     * Attempt WhatsApp notification delivery with up to 3 retries.
     */
    @Async
    public void deliver(Notification notification) {
        String phone = notification.getRecipientPhone();
        UUID notificationId = notification.getId();

        if (phone == null || phone.isBlank()) {
            log.warn("Cannot deliver WhatsApp for notification {}: missing recipient phone", notificationId);
            Notification current = findCurrent(notificationId, notification);
            current.setStatus(NotificationStatus.UNDELIVERED);
            notificationRepository.save(current);
            return;
        }

        // Normalize phone number (strip whitespace, + prefix)
        String cleanPhone = phone.replaceAll("[\\s\\-\\+]", "");

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(accessToken);

                Map<String, Object> payload = new HashMap<>();
                payload.put("messaging_product", "whatsapp");
                payload.put("to", cleanPhone);
                payload.put("type", "template");

                Map<String, Object> templateMap = new HashMap<>();
                templateMap.put("name", notification.getTemplateId() != null ? notification.getTemplateId() : "nag_generic_notification");
                templateMap.put("language", Map.of("code", "en"));
                templateMap.put("components", List.of(
                        Map.of(
                                "type", "body",
                                "parameters", List.of(
                                        Map.of("type", "text", "text", notification.getBody())
                                )
                        )
                ));
                payload.put("template", templateMap);

                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(whatsappEndpoint, requestEntity, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    String externalId = extractMessageId(response.getBody());
                    Notification current = findCurrent(notificationId, notification);
                    current.setStatus(NotificationStatus.SENT);
                    current.setSentAt(Instant.now());
                    current.setRetryCount(attempt);
                    current.setExternalMessageId(externalId);
                    notificationRepository.save(current);
                    log.info("WhatsApp delivered successfully for notification {} on attempt {}, externalId={}",
                            notificationId, attempt, externalId);
                    return;
                } else {
                    throw new RuntimeException("WhatsApp Gateway returned HTTP " + response.getStatusCode());
                }

            } catch (Exception e) {
                notification.setRetryCount(attempt);
                log.warn("WhatsApp delivery failed for notification {} on attempt {}/{}: {}",
                        notificationId, attempt, MAX_RETRIES, e.getMessage());

                if (attempt == MAX_RETRIES) {
                    Notification current = findCurrent(notificationId, notification);
                    current.setStatus(NotificationStatus.UNDELIVERED);
                    current.setRetryCount(MAX_RETRIES);
                    notificationRepository.save(current);
                    log.error("ALERT: WhatsApp permanently UNDELIVERED after {} attempts for notification {}",
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
            return "WA-" + Instant.now().toEpochMilli();
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("messages") && root.get("messages").isArray() && !root.get("messages").isEmpty()) {
                return root.get("messages").get(0).path("id").asText();
            }
            if (root.has("messageId")) {
                return root.get("messageId").asText();
            }
            if (root.has("id")) {
                return root.get("id").asText();
            }
        } catch (Exception ignored) {
        }
        return "WA-" + Instant.now().toEpochMilli();
    }
}
