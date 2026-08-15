/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.questionbank.controller;

import com.examplatform.questionbank.domain.Subject;
import com.examplatform.questionbank.domain.Subtopic;
import com.examplatform.questionbank.domain.Topic;
import com.examplatform.questionbank.dto.CreateSubjectRequest;
import com.examplatform.questionbank.dto.CreateSubtopicRequest;
import com.examplatform.questionbank.dto.CreateTopicRequest;
import com.examplatform.questionbank.dto.SubjectHierarchyResponse;
import com.examplatform.questionbank.service.SubjectTopicService;
import com.examplatform.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Subject → Topic → Subtopic management.
 * All endpoints require appropriate exam platform roles and a tenant header.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
public class SubjectTopicController {

    private final SubjectTopicService subjectTopicService;

    // -----------------------------------------------------------------------
    // Subjects
    // -----------------------------------------------------------------------

    /**
     * List all subjects for the given tenant.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<Subject>>> listSubjects(
            @RequestHeader("X-Tenant-Id") String tenantId) {

        List<Subject> subjects = subjectTopicService.listSubjects(tenantId);
        return ResponseEntity.ok(ApiResponse.success(subjects, "Subjects retrieved successfully"));
    }

    /**
     * Create a new subject.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Subject>> createSubject(
            @Valid @RequestBody CreateSubjectRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        log.info("Creating subject: name={}, tenant={}", request.getName(), tenantId);

        Subject subject = subjectTopicService.createSubject(
                request.getName(), request.getCode(), request.getDescription(), tenantId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(subject, "Subject created successfully"));
    }

    // -----------------------------------------------------------------------
    // Topics
    // -----------------------------------------------------------------------

    /**
     * List all topics for a given subject.
     */
    @GetMapping("/{subjectId}/topics")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<Topic>>> listTopics(
            @PathVariable UUID subjectId,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        List<Topic> topics = subjectTopicService.listTopics(subjectId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(topics, "Topics retrieved successfully"));
    }

    /**
     * Create a new topic under a subject.
     */
    @PostMapping("/{subjectId}/topics")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Topic>> createTopic(
            @PathVariable UUID subjectId,
            @Valid @RequestBody CreateTopicRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        log.info("Creating topic: name={}, subjectId={}, tenant={}", request.getName(), subjectId, tenantId);

        Topic topic = subjectTopicService.createTopic(
                subjectId, request.getName(), request.getDescription(), tenantId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(topic, "Topic created successfully"));
    }

    // -----------------------------------------------------------------------
    // Subtopics
    // -----------------------------------------------------------------------

    /**
     * List all subtopics for a given topic.
     */
    @GetMapping("/{subjectId}/topics/{topicId}/subtopics")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<Subtopic>>> listSubtopics(
            @PathVariable UUID subjectId,
            @PathVariable UUID topicId,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        List<Subtopic> subtopics = subjectTopicService.listSubtopics(topicId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(subtopics, "Subtopics retrieved successfully"));
    }

    /**
     * Create a new subtopic under a topic.
     */
    @PostMapping("/{subjectId}/topics/{topicId}/subtopics")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Subtopic>> createSubtopic(
            @PathVariable UUID subjectId,
            @PathVariable UUID topicId,
            @Valid @RequestBody CreateSubtopicRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        log.info("Creating subtopic: name={}, topicId={}, tenant={}", request.getName(), topicId, tenantId);

        Subtopic subtopic = subjectTopicService.createSubtopic(
                topicId, request.getName(), request.getDescription(), tenantId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(subtopic, "Subtopic created successfully"));
    }

    // -----------------------------------------------------------------------
    // Hierarchy
    // -----------------------------------------------------------------------

    /**
     * Returns the full Subject → Topic → Subtopic tree for populating cascading dropdowns.
     */
    @GetMapping("/hierarchy")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<SubjectHierarchyResponse>>> getHierarchy(
            @RequestHeader("X-Tenant-Id") String tenantId) {

        List<SubjectHierarchyResponse> hierarchy = subjectTopicService.getHierarchy(tenantId);
        return ResponseEntity.ok(ApiResponse.success(hierarchy, "Hierarchy retrieved successfully"));
    }
}
