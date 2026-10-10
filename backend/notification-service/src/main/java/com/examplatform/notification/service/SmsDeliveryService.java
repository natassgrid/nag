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

/**
 * Handles SMS notification delivery with Indian DLT-compliant templates
 * and 3-attempt retry logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsDeliveryService {

    private final NotificationRepository notificationRepository;

    @Setter
    private RestTemplate restTemplate = new RestTemplate();

    @Setter
    private ObjectMapper objectMapper = new ObjectMapper();

    @Value("${notification.sms.endpoint:http://localhost:3000/sms/send}")
    private String smsEndpoint;

    @Value("${notification.sms.authkey:mock-sms-authkey}")
    private String authKey;

    @Value("${notification.sms.sender:EXMPLT}")
    private String senderId;

    private static final int MAX_RETRIES = 3;

    // DLT Template ID mapping by notification purpose (Telecom Regulatory Authority of India compliant)
    private static final Map<String, String> DLT_TEMPLATES = Map.of(
            "OTP", "1107161829384910291",
            "EXAM_SCHEDULED", "1107161829384910292",
            "RESULT_PUBLISHED", "1107161829384910293",
            "PASSWORD_RESET", "1107161829384910294",
            "DEFAULT", "1107161829384910290"
    );

    /**
     * Attempt SMS notification delivery with up to 3 retries.
     */
    @Async
    public void deliver(Notification notification) {
        String phone = notification.getRecipientPhone();
        UUID notificationId = notification.getId();

        if (phone == null || phone.isBlank()) {
            log.warn("Cannot deliver SMS for notification {}: missing recipient phone", notificationId);
            NotificationDeliveryHelper.recordFailure(notificationRepository, notificationId, notification, 0);
            return;
        }

        String dltTemplateId = resolveDltTemplateId(notification.getTemplateId(), notification.getSubject());
        notification.setTemplateId(dltTemplateId);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("authkey", authKey);

                Map<String, Object> payload = new HashMap<>();
                payload.put("mobile", phone);
                payload.put("template_id", dltTemplateId);
                payload.put("sender", senderId);
                payload.put("message", notification.getBody());
                payload.put("otp", "000000"); // Static mock fallback for verification

                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(smsEndpoint, requestEntity, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    String externalId = extractMessageId(response.getBody());
                    NotificationDeliveryHelper.recordSuccess(
                            notificationRepository, notificationId, notification, attempt, externalId, dltTemplateId
                    );
                    log.info("SMS delivered successfully for notification {} on attempt {}, externalId={}",
                            notificationId, attempt, externalId);
                    return;
                } else {
                    throw new RuntimeException("SMS Gateway returned HTTP " + response.getStatusCode());
                }

            } catch (Exception e) {
                notification.setRetryCount(attempt);
                log.warn("SMS delivery failed for notification {} on attempt {}/{}: {}",
                        notificationId, attempt, MAX_RETRIES, e.getMessage());

                if (attempt == MAX_RETRIES) {
                    NotificationDeliveryHelper.recordFailure(notificationRepository, notificationId, notification, MAX_RETRIES);
                    log.error("ALERT: SMS permanently UNDELIVERED after {} attempts for notification {}",
                            MAX_RETRIES, notificationId);
                }
            }
        }
    }

    private String resolveDltTemplateId(String customTemplateId, String subject) {
        if (customTemplateId != null && !customTemplateId.isBlank()) {
            return customTemplateId;
        }
        if (subject != null) {
            for (Map.Entry<String, String> entry : DLT_TEMPLATES.entrySet()) {
                if (subject.toUpperCase().contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return DLT_TEMPLATES.getOrDefault("DEFAULT", "1107161829384910290");
    }

    private String extractMessageId(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "SMS-" + Instant.now().toEpochMilli();
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
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
        } catch (Exception ignored) {
        }
        return "SMS-" + Instant.now().toEpochMilli();
    }
}
