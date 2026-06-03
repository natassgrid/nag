package com.examplatform.notification.service;

import com.examplatform.notification.domain.Notification;
import com.examplatform.notification.domain.Notification.NotificationStatus;
import com.examplatform.notification.domain.Notification.NotificationType;
import com.examplatform.notification.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for InAppNotificationService.
 * Verifies that notifications are returned only for the authenticated user.
 *
 * Validates: Requirements 14.3
 */
@ExtendWith(MockitoExtension.class)
class InAppNotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private InAppNotificationService inAppNotificationService;

    private UUID userId;
    private UUID otherUserId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        tenantId = "tenant-1";
    }

    @Test
    @DisplayName("Returns only notifications for the authenticated user")
    void getNotifications_returnsOnlyUserNotifications() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Notification userNotification = buildNotification(userId, tenantId, "Your result is ready");
        Page<Notification> expectedPage = new PageImpl<>(List.of(userNotification));

        when(notificationRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(userId, tenantId, pageable))
                .thenReturn(expectedPage);

        // When
        Page<Notification> result = inAppNotificationService.getNotifications(userId, tenantId, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserId()).isEqualTo(userId);
        assertThat(result.getContent().get(0).getBody()).isEqualTo("Your result is ready");
    }

    @Test
    @DisplayName("Returns empty page when user has no notifications")
    void getNotifications_noNotifications_returnsEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Notification> emptyPage = Page.empty(pageable);

        when(notificationRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(userId, tenantId, pageable))
                .thenReturn(emptyPage);

        // When
        Page<Notification> result = inAppNotificationService.getNotifications(userId, tenantId, pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Mark as read sets read flag and readAt timestamp")
    void markAsRead_setsReadFlagAndTimestamp() {
        // Given
        UUID notificationId = UUID.randomUUID();
        Notification notification = buildNotification(userId, tenantId, "Test notification");
        setEntityIdAndTenant(notification, notificationId, tenantId);

        when(notificationRepository.findById(notificationId))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // When
        Notification result = inAppNotificationService.markAsRead(notificationId, userId, tenantId);

        // Then
        assertThat(result.isRead()).isTrue();
        assertThat(result.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("Mark as read throws EntityNotFoundException for wrong user")
    void markAsRead_wrongUser_throwsException() {
        // Given
        UUID notificationId = UUID.randomUUID();
        Notification notification = buildNotification(otherUserId, tenantId, "Other user's notification");
        setEntityIdAndTenant(notification, notificationId, tenantId);

        when(notificationRepository.findById(notificationId))
                .thenReturn(Optional.of(notification));

        // When/Then
        assertThatThrownBy(() -> inAppNotificationService.markAsRead(notificationId, userId, tenantId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Mark as read throws EntityNotFoundException for non-existent notification")
    void markAsRead_notFound_throwsException() {
        UUID notificationId = UUID.randomUUID();
        when(notificationRepository.findById(notificationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> inAppNotificationService.markAsRead(notificationId, userId, tenantId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── Helper methods ───────────────────────────────────────────────────────

    private Notification buildNotification(UUID userId, String tenantId, String body) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(NotificationType.IN_APP)
                .subject("Test Subject")
                .body(body)
                .status(NotificationStatus.SENT)
                .retryCount(0)
                .read(false)
                .build();
        notification.setTenantId(tenantId);
        return notification;
    }

    private void setEntityIdAndTenant(Notification notification, UUID id, String tenantId) {
        try {
            var idField = notification.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(notification, id);
            notification.setTenantId(tenantId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set entity ID via reflection", e);
        }
    }
}
