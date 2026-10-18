// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.controller;

import com.examplatform.practice.dto.PracticeSetDto;
import com.examplatform.practice.service.PracticeSetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/practice/sets")
@RequiredArgsConstructor
public class PracticeSetController {

    private final PracticeSetService practiceSetService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PracticeSetDto>> listSets(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long subjectId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long topicId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long subtopicId) {
        if (subjectId == null && topicId == null && subtopicId == null) {
            return ResponseEntity.ok(practiceSetService.getPublishedSets());
        }
        return ResponseEntity.ok(practiceSetService.getPublishedSets(subjectId, topicId, subtopicId));
    }

    @GetMapping("/{setId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PracticeSetDto> getSet(@PathVariable UUID setId) {
        return ResponseEntity.ok(practiceSetService.getSet(setId));
    }
}
