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
import com.examplatform.notification.domain.Notification.NotificationType;
import com.examplatform.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailDeliveryService")
class EmailDeliveryServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private EmailDeliveryService emailDeliveryService;

    private Notification notification;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailDeliveryService, "fromAddress", "no-reply@exam-platform.gov.in");

        notification = Notification.builder()
                .userId(UUID.randomUUID())
                .recipientEmail("user@example.com")
                .type(NotificationType.EMAIL)
                .subject("Account Security Alert")
                .body("Your account has been locked. Contact support with reference: ACC-12345")
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();
    }

    @Test
    @DisplayName("Successful send on first attempt sets status=SENT and retryCount=1")
    void deliver_successOnFirstAttempt_statusSentRetryCount1() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        emailDeliveryService.deliver(notification);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getRetryCount()).isEqualTo(1);
        assertThat(notification.getSentAt()).isNotNull();
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    @DisplayName("Failure on first two attempts, success on third → status=SENT, retryCount=3")
    void deliver_failFirstTwoSuccessThird_statusSentRetryCount3() {
        doThrow(new MailSendException("SMTP timeout"))
                .doThrow(new MailSendException("SMTP timeout"))
                .doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        emailDeliveryService.deliver(notification);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getRetryCount()).isEqualTo(3);
        assertThat(notification.getSentAt()).isNotNull();
        verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    @DisplayName("Failure on all 3 attempts → status=UNDELIVERED, retryCount=3")
    void deliver_failAllAttempts_statusUndeliveredRetryCount3() {
        doThrow(new MailSendException("SMTP timeout"))
                .when(mailSender).send(any(SimpleMailMessage.class));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        emailDeliveryService.deliver(notification);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.UNDELIVERED);
        assertThat(notification.getRetryCount()).isEqualTo(3);
        assertThat(notification.getSentAt()).isNull();
        verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    @DisplayName("Message body contains no PII - only identifiers and action links")
    void deliver_messageBodyContainsNoPII() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        emailDeliveryService.deliver(notification);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        String body = sentMessage.getText();

        // Body should contain only the reference identifier, not PII
        assertThat(body).contains("ACC-12345");
        assertThat(body).doesNotContain("user@example.com");
        // Verify from address is set correctly
        assertThat(sentMessage.getFrom()).isEqualTo("no-reply@exam-platform.gov.in");
        assertThat(sentMessage.getTo()).containsExactly("user@example.com");
        assertThat(sentMessage.getSubject()).isEqualTo("Account Security Alert");
    }
}
