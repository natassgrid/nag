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

import com.examplatform.notification.domain.DeviceToken;
import com.examplatform.notification.domain.Notification;
import com.examplatform.notification.domain.Notification.NotificationStatus;
import com.examplatform.notification.domain.Notification.NotificationType;
import com.examplatform.notification.domain.NotificationPreference;
import com.examplatform.notification.dto.NotificationSendRequest;
import com.examplatform.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Processes incoming Kafka notification events and direct dispatch requests.
 * <p>
 * Parses the event JSON, determines the notification channel using intelligent
 * candidate preference routing, builds safe message bodies using ONLY identifiers
 * and action links (no PII or question content), persists PENDING notifications,
 * and dispatches across multi-channel delivery services (Email, SMS, WhatsApp, Push, In-App).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProcessingService {

    private final NotificationRepository notificationRepository;
    private final EmailDeliveryService emailDeliveryService;
    private final SmsDeliveryService smsDeliveryService;
    private final WhatsAppDeliveryService whatsAppDeliveryService;
    private final PushNotificationService pushNotificationService;
    private final NotificationPreferenceService preferenceService;
    private final DeviceTokenService deviceTokenService;
    private final ObjectMapper objectMapper;

    /**
     * Processes a raw notification event payload from Kafka.
     *
     * @param eventPayload the JSON event payload
     */
    @Transactional
    public void processEvent(String eventPayload) {
        try {
            JsonNode event = objectMapper.readTree(eventPayload);

            String eventType = event.path("eventType").asText("UNKNOWN");
            String userIdStr = event.path("userId").asText(null);
            UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : UUID.randomUUID();
            String recipientEmail = event.path("recipientEmail").asText(null);
            String recipientPhone = event.path("recipientPhone").asText(null);
            String fcmToken = event.path("fcmToken").asText(null);
            String channelStr = event.path("channel").asText(null);
            String tenantId = event.path("tenantId").asText("default");
            String referenceId = event.path("referenceId").asText("");
            String actionLink = event.path("actionLink").asText("");
            String templateId = event.path("templateId").asText(null);

            NotificationSendRequest request = NotificationSendRequest.builder()
                    .userId(userId)
                    .recipientEmail(recipientEmail)
                    .recipientPhone(recipientPhone)
                    .fcmToken(fcmToken)
                    .channel(channelStr != null ? parseChannel(channelStr) : null)
                    .eventType(eventType)
                    .referenceId(referenceId)
                    .actionLink(actionLink)
                    .templateId(templateId)
                    .tenantId(tenantId)
                    .build();

            sendNotification(request);

        } catch (Exception e) {
            log.error("Failed to process notification event: {}", e.getMessage(), e);
        }
    }

    /**
     * Creates and dispatches a multi-channel notification according to candidate preferences
     * and destination attributes.
     *
     * @param request the notification request
     * @return the persisted notification
     */
    @Transactional
    public Notification sendNotification(NotificationSendRequest request) {
        UUID userId = request.getUserId() != null ? request.getUserId() : UUID.randomUUID();
        String tenantId = request.getTenantId() != null ? request.getTenantId() : "default";

        // Fetch candidate preferences for fallback & routing
        NotificationPreference preference = preferenceService.getPreferences(userId, tenantId);

        String email = request.getRecipientEmail() != null ? request.getRecipientEmail() : preference.getEmail();
        String phone = request.getRecipientPhone() != null ? request.getRecipientPhone() : preference.getPhoneNumber();
        String token = request.getFcmToken() != null ? request.getFcmToken() : preference.getFcmToken();

        // If no token in request/preference, check registered device tokens
        if (token == null || token.isBlank()) {
            List<DeviceToken> activeTokens = deviceTokenService.getActiveTokens(userId);
            if (!activeTokens.isEmpty()) {
                token = activeTokens.get(0).getToken();
            }
        }

        NotificationType channel = request.getChannel();
        if (channel == null) {
            channel = resolvePreferredChannel(preference, email, phone, token);
        }

        String eventType = request.getEventType() != null ? request.getEventType() : "GENERIC";
        String subject = request.getSubject() != null ? request.getSubject() : buildSubject(eventType);
        String body = request.getBody() != null ? request.getBody() : buildBody(eventType, request.getReferenceId(), request.getActionLink());

        Notification notification = Notification.builder()
                .userId(userId)
                .recipientEmail(email)
                .recipientPhone(phone)
                .fcmToken(token)
                .type(channel)
                .channel(channel.name())
                .templateId(request.getTemplateId())
                .subject(subject)
                .body(body)
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();
        notification.setTenantId(tenantId);

        Notification saved = notificationRepository.save(notification);
        log.info("Created notification {} [channel={}] for event {} targeting userId={}",
                saved.getId(), channel, eventType, userId);

        dispatchToChannel(saved);
        return saved;
    }

    /**
     * Routes the notification to the corresponding delivery service.
     */
    public void dispatchToChannel(Notification notification) {
        switch (notification.getType()) {
            case EMAIL -> {
                if (notification.getRecipientEmail() != null && !notification.getRecipientEmail().isBlank()) {
                    emailDeliveryService.deliver(notification);
                } else {
                    markUndelivered(notification, "Missing recipient email address");
                }
            }
            case SMS -> {
                if (notification.getRecipientPhone() != null && !notification.getRecipientPhone().isBlank()) {
                    smsDeliveryService.deliver(notification);
                } else {
                    markUndelivered(notification, "Missing recipient phone number for SMS");
                }
            }
            case WHATSAPP -> {
                if (notification.getRecipientPhone() != null && !notification.getRecipientPhone().isBlank()) {
                    whatsAppDeliveryService.deliver(notification);
                } else {
                    markUndelivered(notification, "Missing recipient phone number for WhatsApp");
                }
            }
            case PUSH -> {
                if (notification.getFcmToken() != null && !notification.getFcmToken().isBlank()) {
                    pushNotificationService.deliver(notification);
                } else {
                    markUndelivered(notification, "Missing FCM / device token for Push notification");
                }
            }
            case IN_APP -> {
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(Instant.now());
                notificationRepository.save(notification);
                log.info("In-app notification {} stored for userId={}", notification.getId(), notification.getUserId());
            }
        }
    }

    private void markUndelivered(Notification notification, String reason) {
        log.warn("Notification {} cannot be delivered via {}: {}", notification.getId(), notification.getType(), reason);
        notification.setStatus(NotificationStatus.UNDELIVERED);
        notificationRepository.save(notification);
    }

    private NotificationType resolvePreferredChannel(NotificationPreference preference, String email, String phone, String token) {
        String preferred = preference.getPreferredChannel() != null ? preference.getPreferredChannel().toUpperCase() : "EMAIL";

        if ("WHATSAPP".equals(preferred) && preference.isWhatsappEnabled() && phone != null && !phone.isBlank()) {
            return NotificationType.WHATSAPP;
        }
        if ("SMS".equals(preferred) && preference.isSmsEnabled() && phone != null && !phone.isBlank()) {
            return NotificationType.SMS;
        }
        if ("PUSH".equals(preferred) && preference.isPushEnabled() && token != null && !token.isBlank()) {
            return NotificationType.PUSH;
        }
        if ("IN_APP".equals(preferred) && preference.isInAppEnabled()) {
            return NotificationType.IN_APP;
        }
        if (email != null && !email.isBlank() && preference.isEmailEnabled()) {
            return NotificationType.EMAIL;
        }
        if (phone != null && !phone.isBlank() && preference.isSmsEnabled()) {
            return NotificationType.SMS;
        }
        if (token != null && !token.isBlank() && preference.isPushEnabled()) {
            return NotificationType.PUSH;
        }
        return NotificationType.EMAIL;
    }

    /**
     * Builds the email subject line based on event type.
     * Contains no PII — only describes the action category.
     */
    public String buildSubject(String eventType) {
        return switch (eventType) {
            case "ACCOUNT_LOCKED" -> "Account Security Alert";
            case "SESSION_SUBMITTED" -> "Exam Session Confirmation";
            case "RESULT_PUBLISHED" -> "Result Available";
            case "EVALUATION_COMPLETE" -> "Evaluation Complete - Action Required";
            case "QUESTION_REVIEW" -> "Question Review Assigned";
            case "QUESTION_APPROVED" -> "Question Status Update";
            case "TRANSLATION_ASSIGNED" -> "Translation Task Assigned";
            case "PASSWORD_RESET" -> "Password Reset Request";
            default -> "Notification from Exam Platform";
        };
    }

    /**
     * Builds the message body using ONLY identifiers and action links.
     * Never includes PII (name, email, phone) or question content in the body.
     */
    public String buildBody(String eventType, String referenceId, String actionLink) {
        String ref = referenceId != null && !referenceId.isBlank() ? referenceId : "NAG-REF";
        String baseMessage = switch (eventType) {
            case "ACCOUNT_LOCKED" ->
                    "Your account has been locked due to multiple failed login attempts. " +
                    "Contact support with reference: ACC-" + ref;
            case "SESSION_SUBMITTED" ->
                    "Your exam session has been submitted successfully. " +
                    "Reference: SESSION-" + ref;
            case "RESULT_PUBLISHED" ->
                    "Your examination result is now available. " +
                    "Reference: RESULT-" + ref;
            case "EVALUATION_COMPLETE" ->
                    "Evaluation has been completed for assignment. " +
                    "Reference: EVAL-" + ref;
            case "QUESTION_REVIEW" ->
                    "A question has been assigned to you for review. " +
                    "Reference: QR-" + ref;
            case "QUESTION_APPROVED" ->
                    "Your question has been approved. " +
                    "Reference: QA-" + ref;
            case "TRANSLATION_ASSIGNED" ->
                    "A translation task has been assigned to you. " +
                    "Reference: TRANS-" + ref;
            case "PASSWORD_RESET" ->
                    "A password reset has been requested for your account. " +
                    "Reference: PR-" + ref;
            default ->
                    "You have a new notification. " +
                    "Reference: REF-" + ref;
        };

        if (actionLink != null && !actionLink.isBlank()) {
            return baseMessage + "\n\nAction: " + actionLink;
        }
        return baseMessage;
    }

    private NotificationType parseChannel(String channel) {
        try {
            return NotificationType.valueOf(channel.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NotificationType.EMAIL;
        }
    }
}
