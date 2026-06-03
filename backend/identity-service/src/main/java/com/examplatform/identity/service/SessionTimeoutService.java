package com.examplatform.identity.service;

import com.examplatform.identity.domain.ActiveSession;
import com.examplatform.identity.repository.ActiveSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service that invalidates idle sessions whose {@code expiresAt}
 * timestamp has passed. Runs every 60 seconds.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SessionTimeoutService {

    private final ActiveSessionRepository activeSessionRepository;

    /**
     * Runs every 60 seconds. Finds and deletes all sessions whose expiresAt
     * is before the current time (i.e., idle sessions that have timed out).
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void invalidateExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        List<ActiveSession> expired = activeSessionRepository.findAllByExpiresAtBefore(now);
        if (!expired.isEmpty()) {
            log.info("Invalidating {} expired sessions", expired.size());
            activeSessionRepository.deleteAll(expired);
        }
    }
}
