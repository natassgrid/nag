// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.controller;

import com.examplatform.practice.client.AnvayaPrajnaClient;
import com.examplatform.practice.dto.explanation.ExplanationIrDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller exposing REST endpoints for question explanation retrieval (Issue #348).
 */
@Slf4j
@RestController
@RequestMapping("/api/practice/questions")
@RequiredArgsConstructor
public class PracticeExplanationController {

    private final AnvayaPrajnaClient anvayaPrajnaClient;

    @GetMapping("/{questionId}/explanation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExplanationIrDto> getExplanation(@PathVariable UUID questionId) {
        log.debug("Fetching explanation for question: {}", questionId);
        return anvayaPrajnaClient.getExplanation(questionId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
