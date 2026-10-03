// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.controller;

import com.examplatform.practice.dto.*;
import com.examplatform.practice.service.PracticeResultService;
import com.examplatform.practice.service.PracticeSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody StartSessionRequest request) {
        UUID candidateId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(practiceSessionService.startSession(candidateId, request));
    }

    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PracticeSessionDto> getSession(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId) {
        UUID candidateId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(practiceSessionService.getSession(sessionId, candidateId));
    }

    @GetMapping("/sessions/{sessionId}/questions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PracticeQuestionDto>> getSessionQuestions(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId) {
        UUID candidateId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(practiceSessionService.getSessionQuestions(sessionId, candidateId));
    }

    @PutMapping("/sessions/{sessionId}/response")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> saveResponse(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId,
            @Valid @RequestBody SaveResponseRequest request) {
        UUID candidateId = UUID.fromString(jwt.getSubject());
        practiceSessionService.saveResponse(sessionId, candidateId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/{sessionId}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PracticeResultDto> submitSession(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId) {
        UUID candidateId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(practiceSessionService.submitSession(sessionId, candidateId));
    }

    @GetMapping("/sessions/{sessionId}/result")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PracticeResultDto> getResult(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId) {
        UUID candidateId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(practiceResultService.getResult(sessionId, candidateId));
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PracticeHistoryItemDto>> getHistory(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID candidateId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(practiceSessionService.getHistory(candidateId, PageRequest.of(page, size)));
    }
}
