// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.controller;

import com.examplatform.practice.dto.*;
import com.examplatform.practice.service.PracticeResultService;
import com.examplatform.practice.service.PracticeSessionService;
import com.examplatform.shared.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeSessionController {

    private final PracticeSessionService practiceSessionService;
    private final PracticeResultService practiceResultService;

    @PostMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PracticeSessionDto> startSession(
            @Valid @RequestBody StartSessionRequest request) {
        UUID candidateId = UserContext.getRequiredUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(practiceSessionService.startSession(candidateId, request));
    }

    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PracticeSessionDto> getSession(
            @PathVariable UUID sessionId) {
        UUID candidateId = UserContext.getRequiredUserId();
        return ResponseEntity.ok(practiceSessionService.getSession(sessionId, candidateId));
    }

    @GetMapping("/sessions/{sessionId}/questions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PracticeQuestionDto>> getSessionQuestions(
            @PathVariable UUID sessionId,
            @RequestParam(required = false) String lang) {
        UUID candidateId = UserContext.getRequiredUserId();
        return ResponseEntity.ok(practiceSessionService.getSessionQuestions(sessionId, candidateId, lang));
    }

    @PutMapping("/sessions/{sessionId}/response")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> saveResponse(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SaveResponseRequest request) {
        UUID candidateId = UserContext.getRequiredUserId();
        practiceSessionService.saveResponse(sessionId, candidateId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/{sessionId}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PracticeResultDto> submitSession(
            @PathVariable UUID sessionId) {
        UUID candidateId = UserContext.getRequiredUserId();
        return ResponseEntity.ok(practiceSessionService.submitSession(sessionId, candidateId));
    }

    @GetMapping("/sessions/{sessionId}/result")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PracticeResultDto> getResult(
            @PathVariable UUID sessionId,
            @RequestParam(required = false) String lang) {
        UUID candidateId = UserContext.getRequiredUserId();
        return ResponseEntity.ok(practiceResultService.getResult(sessionId, candidateId, lang));
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PracticeHistoryItemDto>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID candidateId = UserContext.getRequiredUserId();
        return ResponseEntity.ok(practiceSessionService.getHistory(candidateId, PageRequest.of(page, size)));
    }
}
