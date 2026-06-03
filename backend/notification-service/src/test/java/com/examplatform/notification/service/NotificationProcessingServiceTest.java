package com.examplatform.notification.service;

import com.examplatform.notification.domain.Notification;
import com.examplatform.notification.domain.Notification.NotificationStatus;
import com.examplatform.notification.domain.Notification.NotificationType;
import com.examplatform.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationProcessingService")
class NotificationProcessingServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailDeliveryService emailDeliveryService;

    private NotificationProcessingService processingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        processingService = new NotificationProcessingService(
                notificationRepository, emailDeliveryService, objectMapper);
    }

    @Test
    @DisplayName("Processes valid event → creates Notification with PENDING status and calls deliver")
    void processEvent_validEvent_createsNotificationAndDelivers() {
        UUID userId = UUID.randomUUID();
        String eventPayload = """
                {
                    "eventType": "SESSION_SUBMITTED",
                    "userId": "%s",
                    "recipientEmail": "candidate@example.com",
                    "channel": "EMAIL",
                    "tenantId": "gov-exam-authority",
                    "referenceId": "abc-123",
                    "actionLink": "https://portal.exam-platform.gov.in/sessions/abc-123"
                }
                """.formatted(userId);

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        processingService.processEvent(eventPayload);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getRecipientEmail()).isEqualTo("candidate@example.com");
        assertThat(saved.getType()).isEqualTo(NotificationType.EMAIL);
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(saved.getRetryCount()).isZero();
        assertThat(saved.getSubject()).isEqualTo("Exam Session Confirmation");
        assertThat(saved.getBody()).contains("SESSION-abc-123");
        assertThat(saved.getBody()).contains("https://portal.exam-platform.gov.in/sessions/abc-123");

        verify(emailDeliveryService).deliver(saved);
    }

    @Test
    @DisplayName("ACCOUNT_LOCKED event → message body uses identifier only, no name/email in body")
    void processEvent_accountLocked_bodyContainsOnlyIdentifierNoNameOrEmail() {
        UUID userId = UUID.randomUUID();
        String eventPayload = """
                {
                    "eventType": "ACCOUNT_LOCKED",
                    "userId": "%s",
                    "recipientEmail": "john.doe@personal.com",
                    "channel": "EMAIL",
                    "tenantId": "upsc",
                    "referenceId": "usr-456",
                    "actionLink": ""
                }
                """.formatted(userId);

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        processingService.processEvent(eventPayload);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        String body = saved.getBody();

        // Body should contain ONLY the identifier reference
        assertThat(body).contains("ACC-usr-456");
        // Body must NOT contain any PII (no name, no email in body text)
        assertThat(body).doesNotContain("john.doe");
        assertThat(body).doesNotContain("john.doe@personal.com");
        assertThat(body).doesNotContain("John");
        // Subject should be a generic security alert
        assertThat(saved.getSubject()).isEqualTo("Account Security Alert");
    }

    @Test
    @DisplayName("Invalid JSON payload → logs error, does not throw")
    void processEvent_invalidJson_logsErrorDoesNotThrow() {
        String invalidPayload = "not valid json {{{";

        processingService.processEvent(invalidPayload);

        verify(notificationRepository, never()).save(any());
        verify(emailDeliveryService, never()).deliver(any());
    }

    @Test
    @DisplayName("IN_APP channel → creates notification but does not trigger email delivery")
    void processEvent_inAppChannel_doesNotTriggerEmailDelivery() {
        UUID userId = UUID.randomUUID();
        String eventPayload = """
                {
                    "eventType": "RESULT_PUBLISHED",
                    "userId": "%s",
                    "recipientEmail": "user@example.com",
                    "channel": "IN_APP",
                    "tenantId": "ssc",
                    "referenceId": "res-789",
                    "actionLink": ""
                }
                """.formatted(userId);

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        processingService.processEvent(eventPayload);

        verify(notificationRepository).save(any(Notification.class));
        verify(emailDeliveryService, never()).deliver(any());
    }
}
