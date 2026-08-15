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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Handles email delivery for notifications with retry logic.
 * <p>
 * Retries up to 3 times on failure. On third failure, marks the notification
 * as UNDELIVERED and logs an alert. Message bodies contain ONLY identifiers
 * and action links — no PII or question content.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailDeliveryService {

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;

    @Value("${spring.mail.from:no-reply@exam-platform.gov.in}")
    private String fromAddress;

    private static final int MAX_RETRIES = 3;

    /**
     * Attempt to send an email notification. Retries up to 3 times.
     * On third failure, marks as UNDELIVERED and logs the error.
     * Message body contains ONLY identifiers and action links — no PII.
     */
    @Async
    public void deliver(Notification notification) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromAddress);
                message.setTo(notification.getRecipientEmail());
                message.setSubject(notification.getSubject());
                message.setText(notification.getBody());

                mailSender.send(message);

                notification.setStatus(Notification.NotificationStatus.SENT);
                notification.setSentAt(Instant.now());
                notification.setRetryCount(attempt);
                notificationRepository.save(notification);
                log.info("Email sent successfully for notification {} on attempt {}",
                        notification.getId(), attempt);
                return;

            } catch (Exception e) {
                notification.setRetryCount(attempt);
                log.warn("Email delivery failed for notification {} attempt {}/{}: {}",
                        notification.getId(), attempt, MAX_RETRIES, e.getMessage());

                if (attempt == MAX_RETRIES) {
                    notification.setStatus(Notification.NotificationStatus.UNDELIVERED);
                    notificationRepository.save(notification);
                    log.error("ALERT: Email permanently UNDELIVERED after {} attempts for notification {}",
                            MAX_RETRIES, notification.getId());
                }
            }
        }
    }
}
