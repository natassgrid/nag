// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.controller;

import com.examplatform.practice.domain.PracticeSet;
import com.examplatform.practice.dto.PracticeSetDto;
import com.examplatform.practice.repository.PracticeSetRepository;
import com.examplatform.practice.service.PracticeSetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/practice/sets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('CONTENT_MANAGER')")
public class AdminPracticeSetController {

    private final PracticeSetRepository practiceSetRepository;
    private final PracticeSetService practiceSetService;

    @GetMapping
    public ResponseEntity<List<PracticeSet>> listAll() {
        return ResponseEntity.ok(practiceSetRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<PracticeSet> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody PracticeSet practiceSet) {
        practiceSet.setCreatedBy(UUID.fromString(jwt.getSubject()));
        return ResponseEntity.status(HttpStatus.CREATED).body(practiceSetRepository.save(practiceSet));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PracticeSet> update(@PathVariable UUID id, @RequestBody PracticeSet update) {
        return practiceSetRepository.findById(id)
                .map(existing -> {
                    existing.setName(update.getName());
                    existing.setDescription(update.getDescription());
                    existing.setDurationMinutes(update.getDurationMinutes());
                    existing.setQuestionIds(update.getQuestionIds());
                    existing.setTotalQuestions(update.getTotalQuestions());
                    existing.setSubjectSlug(update.getSubjectSlug());
                    return ResponseEntity.ok(practiceSetRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        practiceSetRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Void> publish(@PathVariable UUID id) {
        practiceSetRepository.findById(id).ifPresent(s -> {
            s.setPublished(true);
            practiceSetRepository.save(s);
        });
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<Void> unpublish(@PathVariable UUID id) {
        practiceSetRepository.findById(id).ifPresent(s -> {
            s.setPublished(false);
            practiceSetRepository.save(s);
        });
        return ResponseEntity.noContent().build();
    }
}
