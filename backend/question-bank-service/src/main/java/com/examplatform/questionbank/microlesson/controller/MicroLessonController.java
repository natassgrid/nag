// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.controller;

import com.examplatform.questionbank.microlesson.dto.LessonProgressRequest;
import com.examplatform.questionbank.microlesson.dto.MicroLessonResponse;
import com.examplatform.questionbank.microlesson.service.MicroLessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/micro-lessons")
@RequiredArgsConstructor
public class MicroLessonController {

    private final MicroLessonService microLessonService;

    /**
     * Candidate-facing endpoint returning list of micro-lessons for a subtopic with filters.
     * Supports multiple lessons per subtopic (v2) filtered by lessonType, difficulty, and language.
     */
    @GetMapping("/subtopic/{subtopicId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MicroLessonResponse>> getMicroLessonsForSubtopic(
            @PathVariable Long subtopicId,
            @RequestParam(required = false) String lessonType,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String language,
            @RequestParam(name = "lang", required = false) String lang,
            @RequestParam(required = false) String targetExam) {

        String resolvedLanguage = (lang != null && !lang.isBlank())
                ? lang.trim()
                : ((language != null && !language.isBlank()) ? language.trim() : "en");

        List<MicroLessonResponse> lessons = microLessonService.getMicroLessonsForSubtopic(
                subtopicId, lessonType, difficulty, resolvedLanguage);
        return ResponseEntity.ok(lessons);
    }

    /**
     * Learner lesson-progress and effectiveness hook.
     */
    @PostMapping("/{id}/progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> recordProgress(
            @PathVariable UUID id,
            @RequestBody(required = false) LessonProgressRequest request) {

        LessonProgressRequest req = request != null ? request : new LessonProgressRequest();
        req.setLessonId(id);
        microLessonService.recordProgress(req);
        return ResponseEntity.ok().build();
    }
}
