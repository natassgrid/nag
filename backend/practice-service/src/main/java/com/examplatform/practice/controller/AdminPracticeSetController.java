// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.controller;

import com.examplatform.practice.domain.PracticeSet;
import com.examplatform.practice.dto.CreatePracticeSetRequest;
import com.examplatform.practice.dto.UpdatePracticeSetRequest;
import com.examplatform.practice.repository.PracticeSetRepository;
import com.examplatform.practice.service.PracticeSetService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin/practice/sets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER', 'SUPER_ADMIN', 'EXAM_CONTROLLER')")
public class AdminPracticeSetController {

    private final PracticeSetRepository practiceSetRepository;
    private final PracticeSetService practiceSetService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<PracticeSet>> listAll() {
        return ResponseEntity.ok(practiceSetRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<PracticeSet> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePracticeSetRequest request) {

        PracticeSet practiceSet = new PracticeSet();
        practiceSet.setName(request.name());
        practiceSet.setDescription(request.description());
        practiceSet.setDurationMinutes(request.durationMinutes());
        practiceSet.setSubjectSlug(request.subjectSlug());
        practiceSet.setSource(request.source() != null && !request.source().isBlank() ? request.source() : "MANUAL");

        if (request.questionIds() != null && !request.questionIds().isEmpty()) {
            practiceSet.setTotalQuestions(request.questionIds().size());
            try {
                practiceSet.setQuestionIds(objectMapper.writeValueAsString(request.questionIds()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize questionIds for practice set {}: {}", request.name(), e.getMessage());
                practiceSet.setQuestionIds("[]");
            }
        } else {
            practiceSet.setTotalQuestions(request.totalQuestions() != null ? request.totalQuestions() : 0);
            practiceSet.setQuestionIds("[]");
        }

        if (jwt != null && jwt.getSubject() != null) {
            try {
                practiceSet.setCreatedBy(UUID.fromString(jwt.getSubject()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(practiceSetRepository.save(practiceSet));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PracticeSet> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePracticeSetRequest update) {
        return practiceSetRepository.findById(id)
                .map(existing -> {
                    existing.setName(update.name());
                    existing.setDescription(update.description());
                    existing.setDurationMinutes(update.durationMinutes());
                    if (update.subjectSlug() != null) {
                        existing.setSubjectSlug(update.subjectSlug());
                    }
                    if (update.questionIds() != null) {
                        existing.setTotalQuestions(update.questionIds().size());
                        try {
                            existing.setQuestionIds(objectMapper.writeValueAsString(update.questionIds()));
                        } catch (JsonProcessingException e) {
                            log.warn("Failed to serialize questionIds on update for practice set {}: {}", id, e.getMessage());
                            existing.setQuestionIds("[]");
                        }
                    } else if (update.totalQuestions() != null) {
                        existing.setTotalQuestions(update.totalQuestions());
                    }
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
