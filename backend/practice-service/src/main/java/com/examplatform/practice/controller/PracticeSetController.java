// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.controller;

import com.examplatform.practice.dto.PracticeSetDto;
import com.examplatform.practice.service.PracticeSetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/practice/sets")
@RequiredArgsConstructor
public class PracticeSetController {

    private final PracticeSetService practiceSetService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PracticeSetDto>> listSets() {
        return ResponseEntity.ok(practiceSetService.getPublishedSets());
    }

    @GetMapping("/{setId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PracticeSetDto> getSet(@PathVariable UUID setId) {
        return ResponseEntity.ok(practiceSetService.getSet(setId));
    }
}
