package com.examplatform.notification.service;

import com.examplatform.notification.domain.Notification;
import com.examplatform.notification.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for in-app notification operations.
 * Queries notifications by userId and tenantId, supports marking as read.
 *
 * Validates: Requirements 14.3
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InAppNotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Retrieves paginated notifications for the authenticated user within a tenant.
     *
     * @param userId   the user's ID
     * @param tenantId the tenant identifier
     * @param pageable pagination parameters
     * @return page of notifications ordered by createdAt descending
     */
    @Transactional(readOnly = true)
    public Page<Notification> getNotifications(UUID userId, String tenantId, Pageable pageable) {
        log.debug("Fetching notifications for userId={}, tenantId={}", userId, tenantId);
        return notificationRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(
                userId, tenantId, pageable);
    }

    /**
     * Marks a notification as read.
     *
     * @param notificationId the notification ID
     * @param userId         the user's ID (for ownership verification)
     * @param tenantId       the tenant identifier
     * @return the updated notification
     */
    public Notification markAsRead(UUID notificationId, UUID userId, String tenantId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Notification not found: " + notificationId));

        // Verify ownership
        if (!notification.getUserId().equals(userId) || !notification.getTenantId().equals(tenantId)) {
            throw new EntityNotFoundException("Notification not found: " + notificationId);
        }

        notification.setRead(true);
        notification.setReadAt(Instant.now());
        Notification saved = notificationRepository.save(notification);

        log.info("Marked notification {} as read for userId={}", notificationId, userId);
        return saved;
    }
}
