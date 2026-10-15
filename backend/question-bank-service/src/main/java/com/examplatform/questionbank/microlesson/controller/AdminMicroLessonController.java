// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.controller;

import com.examplatform.questionbank.microlesson.ai.MicroLessonBatchRunner;
import com.examplatform.questionbank.microlesson.dto.BatchGenerateMicroLessonsRequest;
import com.examplatform.questionbank.microlesson.dto.GenerateMicroLessonRequest;
import com.examplatform.questionbank.microlesson.dto.BatchGenerateResponse;
import com.examplatform.questionbank.microlesson.dto.MicroLessonResponse;
import com.examplatform.questionbank.microlesson.dto.ReviewMicroLessonRequest;
import com.examplatform.questionbank.microlesson.service.MicroLessonService;
import com.examplatform.shared.constants.SharedConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/micro-lessons")
@RequiredArgsConstructor
public class AdminMicroLessonController {

    private final MicroLessonService microLessonService;
    private final MicroLessonBatchRunner batchRunner;

    /**
     * Paginated review list for faculty and content moderators.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<MicroLessonResponse>> listForReview(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(microLessonService.listForReview(status, subjectId, pageRequest));
    }

    /**
     * Faculty/Admin approves, rejects, or edits a remedial micro-lesson.
     */
    @PutMapping("/{id}/review")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroLessonResponse> reviewMicroLesson(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ReviewMicroLessonRequest request) {

        UUID reviewerId = null;
        if (jwt != null && jwt.getSubject() != null) {
            try {
                reviewerId = UUID.fromString(jwt.getSubject());
            } catch (IllegalArgumentException ignored) {}
        }

        return ResponseEntity.ok(microLessonService.reviewMicroLesson(id, reviewerId, request));
    }

    /**
     * Admin trigger for asynchronous or syllabus subtopic batch generation.
     */
    @PostMapping("/generate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroLessonResponse> generateMicroLesson(
            @RequestBody GenerateMicroLessonRequest request) {
        MicroLessonResponse response = microLessonService.generateSingleMicroLesson(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch-generate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BatchGenerateResponse> triggerBatchGenerate(
            @RequestBody(required = false) BatchGenerateMicroLessonsRequest request) {
        BatchGenerateResponse response = batchRunner.runBatchGeneration(request);
        return ResponseEntity.ok(response);
    }
}
