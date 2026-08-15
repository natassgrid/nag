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

package com.examplatform.delivery.service;

import com.examplatform.delivery.domain.ExamSession;
import com.examplatform.delivery.domain.ExamSession.ExamSessionStatus;
import com.examplatform.delivery.repository.ExamSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages session timer expiration and provides time-remaining queries.
 * Runs a scheduled task every 10 seconds to expire overdue ACTIVE sessions.
 *
 * Validates: Requirements 9.3, 9.6, 9.8, 22.6
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionTimerService {

    private static final String TOPIC_SESSION_EVENTS = "exam.session.events";

    private final ExamSessionRepository examSessionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Scheduled task that runs every 10 seconds to find and expire ACTIVE sessions
     * that have passed their scheduledEndAt time.
     */
    @Scheduled(fixedDelay = 10_000)
    @Transactional
    public void expireOverdueSessions() {
        Instant now = Instant.now();
        List<ExamSession> activeSessions = examSessionRepository.findByStatus(ExamSessionStatus.ACTIVE);

        for (ExamSession session : activeSessions) {
            if (session.getScheduledEndAt().isBefore(now)) {
                session.setStatus(ExamSessionStatus.EXPIRED);
                examSessionRepository.save(session);

                // Publish SESSION_EXPIRED event to Kafka
                Map<String, Object> event = Map.of(
                        "eventType", "SESSION_EXPIRED",
                        "sessionId", session.getSessionId().toString(),
                        "candidateId", session.getCandidateId().toString(),
                        "examId", session.getExamId().toString(),
                        "expiredAt", now.toString(),
                        "tenantId", session.getTenantId()
                );

                kafkaTemplate.send(TOPIC_SESSION_EVENTS, session.getSessionId().toString(), event);

                log.info("Session expired: sessionId={}, candidateId={}, scheduledEndAt={}",
                        session.getSessionId(), session.getCandidateId(), session.getScheduledEndAt());
            }
        }
    }

    /**
     * Returns the remaining time for a given session.
     *
     * @param sessionId the exam session ID
     * @return remaining duration (zero or negative if expired)
     * @throws jakarta.persistence.EntityNotFoundException if session not found
     */
    public Duration getTimeRemaining(UUID sessionId) {
        ExamSession session = examSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Session not found: " + sessionId));
        return Duration.between(Instant.now(), session.getScheduledEndAt());
    }

    /**
     * Retrieves the session status details for a candidate-facing status endpoint.
     *
     * @param sessionId the exam session ID
     * @return the exam session entity
     */
    public ExamSession getSession(UUID sessionId) {
        return examSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Session not found: " + sessionId));
    }
}
