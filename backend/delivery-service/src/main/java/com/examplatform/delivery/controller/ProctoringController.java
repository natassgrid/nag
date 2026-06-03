package com.examplatform.delivery.controller;

import com.examplatform.delivery.service.ProctoringService;
import com.examplatform.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for proctoring operations during an active exam session.
 *
 * Validates: Requirements 11.1, 11.2, 11.6, 11.7
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/proctoring")
@RequiredArgsConstructor
public class ProctoringController {

    private final ProctoringService proctoringService;

    /**
     * Captures a webcam snapshot for AI proctoring analysis.
     *
     * @param sessionId the exam session UUID
     * @param imageData raw image bytes from the webcam
     * @param tenantId  tenant identifier from header
     * @return 200 OK on successful capture
     */
    @PostMapping("/snapshot")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<Void>> captureSnapshot(
            @PathVariable UUID sessionId,
            @RequestBody(required = false) byte[] imageData,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        log.debug("Snapshot capture request for session={}", sessionId);
        proctoringService.captureSnapshot(sessionId, imageData, tenantId);
        return ResponseEntity.ok(ApiResponse.success("Snapshot captured successfully"));
    }

    /**
     * Records a full-screen exit event for the given session.
     * The session is flagged after 3 exits.
     *
     * @param sessionId the exam session UUID
     * @return 200 OK on successful recording
     */
    @PostMapping("/fullscreen-exit")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<Void>> recordFullScreenExit(
            @PathVariable UUID sessionId) {

        log.debug("Full-screen exit recorded for session={}", sessionId);
        proctoringService.recordFullScreenExit(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Full-screen exit recorded"));
    }
}
