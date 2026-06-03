package com.examplatform.notification.repository;

import com.examplatform.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for notifications.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Retrieves all notifications for a given user within a tenant.
     */
    List<Notification> findByUserIdAndTenantId(UUID userId, String tenantId);

    /**
     * Retrieves paginated notifications for a given user within a tenant, ordered by creation time descending.
     */
    Page<Notification> findByUserIdAndTenantIdOrderByCreatedAtDesc(UUID userId, String tenantId, Pageable pageable);

    /**
     * Retrieves notifications by status within a tenant (for retry processing).
     */
    List<Notification> findByStatusAndTenantId(Notification.NotificationStatus status, String tenantId);

    /**
     * Retrieves pending notifications that need to be retried.
     */
    List<Notification> findByStatusAndRetryCountLessThan(Notification.NotificationStatus status, Integer maxRetries);
}
