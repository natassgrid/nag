// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.controller;

import com.examplatform.questionbank.microlesson.ai.MicroLessonBatchRunner;
import com.examplatform.questionbank.microlesson.dto.*;
import com.examplatform.questionbank.microlesson.service.MicroLessonService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.examplatform.shared.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/micro-lessons")
@RequiredArgsConstructor
@Tag(name = "Admin Micro-Lessons", description = "Endpoints for faculty and admin review and authoring of micro-lessons")
public class AdminMicroLessonController {

    private final MicroLessonService microLessonService;
    private final MicroLessonBatchRunner batchRunner;

    /**
     * Lists micro-lessons awaiting review with pagination, optional search, and taxonomy/status filters.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<MicroLessonResponse>> listForReview(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String targetExam,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<MicroLessonResponse> page = microLessonService.listForReview(
                status, subjectId, subject, topic, targetExam, query, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Aggregated statistics for the Micro-Lesson Moderation Desk header ribbon.
     */
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroLessonStatsResponse> getStats() {
        return ResponseEntity.ok(microLessonService.getStats());
    }

    /**
     * Retrieves unique subjects, topics, and target exams present in micro-lessons for dynamic dropdown filters.
     */
    @GetMapping("/filter-options")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroLessonFilterOptionsResponse> getFilterOptions(
            @RequestParam(required = false) String subject) {
        return ResponseEntity.ok(microLessonService.getFilterOptions(subject));
    }

    /**
     * Retrieves a single micro-lesson by ID for viewing or editing in the Authoring Studio.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroLessonResponse> getMicroLessonById(@PathVariable UUID id) {
        return ResponseEntity.ok(microLessonService.getMicroLessonById(id));
    }

    /**
     * Creates a manually authored micro-lesson.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroLessonResponse> createMicroLesson(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateMicroLessonRequest request) {
        UUID authorId = extractUserId(jwt);
        MicroLessonResponse response = microLessonService.createManualMicroLesson(request, authorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing micro-lesson by ID and increments its content version.
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroLessonResponse> updateMicroLesson(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateMicroLessonRequest request) {
        UUID editorId = extractUserId(jwt);
        MicroLessonResponse response = microLessonService.updateMicroLesson(id, request, editorId);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a single micro-lesson by ID.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMicroLesson(@PathVariable UUID id) {
        microLessonService.deleteMicroLesson(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Bulk deletes multiple micro-lessons by IDs.
     */
    @PostMapping("/bulk-delete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BulkDeleteResponse> bulkDeleteMicroLessons(
            @RequestBody BulkDeleteMicroLessonsRequest request) {
        BulkDeleteResponse response = microLessonService.bulkDelete(request != null ? request.ids() : List.of());
        return ResponseEntity.ok(response);
    }

    /**
     * Approves, rejects, or requests revision for a micro-lesson.
     */
    @PutMapping("/{id}/review")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroLessonResponse> reviewMicroLesson(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReviewMicroLessonRequest request) {

        UUID reviewerId = extractUserId(jwt);
        MicroLessonResponse response = microLessonService.reviewMicroLesson(id, reviewerId, request);
        return ResponseEntity.ok(response);
    }

    private UUID extractUserId(Jwt jwt) {
        UUID userId = UserContext.resolveUserId(jwt);
        if (userId != null) {
            return userId;
        }
        if (jwt != null && jwt.getSubject() != null && !jwt.getSubject().isBlank()) {
            return UUID.nameUUIDFromBytes(jwt.getSubject().getBytes(StandardCharsets.UTF_8));
        }
        return null;
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
        BatchGenerateResponse response = batchRunner.startBatchJob(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/batch-jobs/{batchId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BatchGenerateResponse> getBatchJobStatus(@PathVariable String batchId) {
        return batchRunner.getJobStatus(batchId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/batch-jobs/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BatchGenerateResponse> getActiveBatchJob() {
        BatchGenerateResponse active = batchRunner.getActiveJob();
        if (active == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(active);
    }

    @PostMapping("/batch-jobs/{batchId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BatchGenerateResponse> cancelBatchJob(@PathVariable String batchId) {
        return batchRunner.cancelJob(batchId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
