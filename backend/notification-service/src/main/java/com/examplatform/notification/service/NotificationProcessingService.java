package com.examplatform.notification.service;

import com.examplatform.notification.domain.Notification;
import com.examplatform.notification.domain.Notification.NotificationStatus;
import com.examplatform.notification.domain.Notification.NotificationType;
import com.examplatform.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Processes incoming Kafka notification events.
 * <p>
 * Parses the event JSON, determines the notification channel, builds
 * a message body using ONLY identifiers and action links (no PII or
 * question content), persists a PENDING notification, and dispatches
 * to the appropriate delivery service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProcessingService {

    private final NotificationRepository notificationRepository;
    private final EmailDeliveryService emailDeliveryService;
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
            UUID userId = UUID.fromString(event.path("userId").asText());
            String recipientEmail = event.path("recipientEmail").asText(null);
            String channelStr = event.path("channel").asText("EMAIL");
            String tenantId = event.path("tenantId").asText("default");
            String referenceId = event.path("referenceId").asText("");
            String actionLink = event.path("actionLink").asText("");

            NotificationType channel = parseChannel(channelStr);
            String subject = buildSubject(eventType);
            String body = buildBody(eventType, referenceId, actionLink);

            Notification notification = Notification.builder()
                    .userId(userId)
                    .recipientEmail(recipientEmail)
                    .type(channel)
                    .subject(subject)
                    .body(body)
                    .status(NotificationStatus.PENDING)
                    .retryCount(0)
                    .build();
            notification.setTenantId(tenantId);

            notificationRepository.save(notification);
            log.info("Created notification {} for event type {} targeting user {}",
                    notification.getId(), eventType, userId);

            if (channel == NotificationType.EMAIL && recipientEmail != null) {
                emailDeliveryService.deliver(notification);
            }

        } catch (Exception e) {
            log.error("Failed to process notification event: {}", e.getMessage(), e);
        }
    }

    /**
     * Builds the email subject line based on event type.
     * Contains no PII — only describes the action category.
     */
    private String buildSubject(String eventType) {
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
     *
     * @param eventType   the type of notification event
     * @param referenceId an opaque reference identifier
     * @param actionLink  the action URL for the user
     * @return the safe message body
     */
    private String buildBody(String eventType, String referenceId, String actionLink) {
        String baseMessage = switch (eventType) {
            case "ACCOUNT_LOCKED" ->
                    "Your account has been locked due to multiple failed login attempts. " +
                    "Contact support with reference: ACC-" + referenceId;
            case "SESSION_SUBMITTED" ->
                    "Your exam session has been submitted successfully. " +
                    "Reference: SESSION-" + referenceId;
            case "RESULT_PUBLISHED" ->
                    "Your examination result is now available. " +
                    "Reference: RESULT-" + referenceId;
            case "EVALUATION_COMPLETE" ->
                    "Evaluation has been completed for assignment. " +
                    "Reference: EVAL-" + referenceId;
            case "QUESTION_REVIEW" ->
                    "A question has been assigned to you for review. " +
                    "Reference: QR-" + referenceId;
            case "QUESTION_APPROVED" ->
                    "Your question has been approved. " +
                    "Reference: QA-" + referenceId;
            case "TRANSLATION_ASSIGNED" ->
                    "A translation task has been assigned to you. " +
                    "Reference: TRANS-" + referenceId;
            case "PASSWORD_RESET" ->
                    "A password reset has been requested for your account. " +
                    "Reference: PR-" + referenceId;
            default ->
                    "You have a new notification. " +
                    "Reference: REF-" + referenceId;
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
