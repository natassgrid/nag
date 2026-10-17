// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.controller;

import com.examplatform.questionbank.microlesson.dto.MicroLessonResponse;
import com.examplatform.questionbank.microlesson.service.MicroLessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/micro-lessons")
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
            @RequestParam(defaultValue = "JEE_MAIN") String targetExam,
            @RequestParam(required = false) String subtopicName,
            @RequestParam(required = false) String topicName,
            @RequestParam(required = false) String subjectName,
            @RequestParam(required = false, defaultValue = "en") String lang) {

        return microLessonService.getPublishedMicroLesson(subtopicId, targetExam, subjectName, topicName, subtopicName, lang)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Published micro-lesson not available for subtopic " + subtopicId));
    }
}
