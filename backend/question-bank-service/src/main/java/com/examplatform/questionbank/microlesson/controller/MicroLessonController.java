// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.controller;

import com.examplatform.questionbank.microlesson.dto.MicroLessonResponse;
import com.examplatform.questionbank.microlesson.service.MicroLessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping({"/api/micro-lessons", "/api/v1/micro-lessons"})
@RequiredArgsConstructor
public class MicroLessonController {

    private final MicroLessonService microLessonService;

    /**
     * Candidate-facing read-only endpoint returning verified PUBLISHED micro-lesson.
     * Backed by Redis cache (TTL 7 days) for &lt;20ms retrieval latency.
     */
    @GetMapping("/subtopic/{subtopicId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroLessonResponse> getPublishedMicroLesson(
            @PathVariable Long subtopicId,
            @RequestParam(required = false) String targetExam) {

        return microLessonService.getPublishedMicroLesson(subtopicId, targetExam)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Published micro-lesson not available for subtopic " + subtopicId));
    }
}
