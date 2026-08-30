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

package com.examplatform.examination.service;

import com.examplatform.examination.domain.Examination;
import com.examplatform.examination.domain.Section;
import com.examplatform.examination.dto.CreateExaminationRequest;
import com.examplatform.examination.dto.ExaminationResponse;
import com.examplatform.examination.exception.ExaminationNotFoundException;
import com.examplatform.examination.exception.SectionMarksValidationException;
import com.examplatform.examination.repository.ExaminationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service layer for examination CRUD operations with section marks validation.
 *
 * Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5, 7.6
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ExaminationService {

    private static final String AUDIT_TOPIC = "exam.audit.events";

    private final ExaminationRepository examinationRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Lists all examinations belonging to the given tenant.
     */
    public List<ExaminationResponse> listByTenant(String tenantId) {
        List<Examination> examinations = examinationRepository.findByTenantId(tenantId);
        return examinations.stream()
                .map(exam -> {
                    List<Section> sections = deserializeSections(exam.getSectionsJson());
                    return toResponse(exam, sections);
                })
                .toList();
    }

    /**
     * Lists examinations with server-side pagination and optional search filter.
     */
    public org.springframework.data.domain.Page<ExaminationResponse> listByTenantPaged(
            String tenantId, String search, int page, int size) {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size,
                        org.springframework.data.domain.Sort.by("createdAt").descending());

        org.springframework.data.domain.Page<Examination> examPage;
        if (search != null && !search.isBlank()) {
            examPage = examinationRepository.findByTenantIdAndNameContainingIgnoreCase(tenantId, search.trim(), pageable);
        } else {
            examPage = examinationRepository.findByTenantId(tenantId, pageable);
        }

        return examPage.map(exam -> {
            List<Section> sections = deserializeSections(exam.getSectionsJson());
            return toResponse(exam, sections);
        });
    }

    /**
     * Lists PUBLISHED examinations for the given tenant with pagination.
     * Used by the candidate-facing public endpoint — no admin role required.
     *
     * @param tenantId tenant identifier
     * @param search   optional search term matched against exam name
     * @param page     zero-based page number
     * @param size     page size
     * @return paginated list of published examinations
     */
    public org.springframework.data.domain.Page<ExaminationResponse> listPublishedPaged(
            String tenantId, String search, int page, int size) {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size,
                        org.springframework.data.domain.Sort.by("createdAt").descending());

        org.springframework.data.domain.Page<Examination> examPage;
        if (search != null && !search.isBlank()) {
            examPage = examinationRepository.findByStatusAndTenantIdAndNameContainingIgnoreCase(
                    "PUBLISHED", tenantId, search.trim(), pageable);
        } else {
            examPage = examinationRepository.findByStatusAndTenantId("PUBLISHED", tenantId, pageable);
        }

        return examPage.map(exam -> {
            List<Section> sections = deserializeSections(exam.getSectionsJson());
            return toResponse(exam, sections);
        });
    }

    /**
     * Creates a new examination in DRAFT status.
     * Validates that sum(section.marksPerQuestion × section.questionCount) == totalMarks.
     */
    public ExaminationResponse create(CreateExaminationRequest request, String tenantId) {
        validateSectionMarks(request);

        String sectionsJson = serializeSections(request.getSections());

        Examination examination = Examination.builder()
                .name(request.getName())
                .durationMinutes(request.getDurationMinutes())
                .totalMarks(request.getTotalMarks())
                .negativeMarkingEnabled(request.isNegativeMarkingEnabled())
                .negativeMarkingValue(request.isNegativeMarkingEnabled() ? request.getNegativeMarkingValue() : 0.0)
                .navigationPolicy(request.getNavigationPolicy().name())
                .calculatorPolicy(request.getCalculatorPolicy().name())
                .reviewFlagEnabled(request.isReviewFlagEnabled())
                .sectionsJson(sectionsJson)
                .status("DRAFT")
                .build();

        examination.setTenantId(tenantId);

        Examination saved = examinationRepository.save(examination);
        log.info("Created examination '{}' with id={} for tenant={}", saved.getName(), saved.getId(), tenantId);

        return toResponse(saved, request.getSections());
    }

    /**
     * Updates an existing examination. Re-validates section marks.
     * Throws if not found or tenant mismatch.
     */
    public ExaminationResponse update(UUID examId, CreateExaminationRequest request, String tenantId) {
        Examination examination = examinationRepository.findById(examId)
                .orElseThrow(() -> new ExaminationNotFoundException(examId));

        if (!examination.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Cannot update examination belonging to another tenant");
        }

        validateSectionMarks(request);

        String sectionsJson = serializeSections(request.getSections());

        examination.setName(request.getName());
        examination.setDurationMinutes(request.getDurationMinutes());
        examination.setTotalMarks(request.getTotalMarks());
        examination.setNegativeMarkingEnabled(request.isNegativeMarkingEnabled());
        examination.setNegativeMarkingValue(request.isNegativeMarkingEnabled() ? request.getNegativeMarkingValue() : 0.0);
        examination.setNavigationPolicy(request.getNavigationPolicy().name());
        examination.setCalculatorPolicy(request.getCalculatorPolicy().name());
        examination.setReviewFlagEnabled(request.isReviewFlagEnabled());
        examination.setSectionsJson(sectionsJson);

        Examination saved = examinationRepository.save(examination);
        log.info("Updated examination id={} for tenant={}", saved.getId(), tenantId);

        return toResponse(saved, request.getSections());
    }

    /**
     * Retrieves an examination by its identifier.
     * Deserializes sectionsJson back into a List of Section objects.
     */
    public ExaminationResponse getById(UUID examId) {
        Examination examination = examinationRepository.findById(examId)
                .orElseThrow(() -> new ExaminationNotFoundException(examId));

        List<Section> sections = deserializeSections(examination.getSectionsJson());
        return toResponse(examination, sections);
    }

    /**
     * Publishes an examination: sets status to PUBLISHED and emits EXAM_PUBLISHED audit event.
     *
     * @param examId   the exam UUID
     * @param tenantId the tenant identifier
     * @return the updated examination response
     */
    public ExaminationResponse publish(UUID examId, String tenantId) {
        Examination examination = examinationRepository.findById(examId)
                .orElseThrow(() -> new ExaminationNotFoundException(examId));

        if (!examination.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Cannot publish examination belonging to another tenant");
        }

        examination.setStatus("PUBLISHED");
        Examination saved = examinationRepository.save(examination);
        log.info("Published examination id={} for tenant={}", saved.getId(), tenantId);

        // Publish EXAM_PUBLISHED audit event (fire-and-forget)
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "EXAM_PUBLISHED");
            event.put("examId", saved.getId().toString());
            event.put("examName", saved.getName());
            event.put("tenantId", tenantId);
            event.put("occurredAt", Instant.now().toString());

            kafkaTemplate.send(AUDIT_TOPIC, saved.getId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish EXAM_PUBLISHED audit event for exam [{}]: {}",
                                    examId, ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Unexpected error publishing EXAM_PUBLISHED audit event: {}", e.getMessage());
        }

        List<Section> sections = deserializeSections(saved.getSectionsJson());
        return toResponse(saved, sections);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void validateSectionMarks(CreateExaminationRequest request) {
        double actualTotal = request.getSections().stream()
                .mapToDouble(section -> section.getMarksPerQuestion() * section.getQuestionCount())
                .sum();

        if (Math.abs(actualTotal - request.getTotalMarks()) > 0.001) {
            throw new SectionMarksValidationException(request.getTotalMarks(), actualTotal);
        }
    }

    private String serializeSections(List<Section> sections) {
        try {
            return objectMapper.writeValueAsString(sections);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize sections to JSON", e);
        }
    }

    private List<Section> deserializeSections(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize sections from JSON", e);
        }
    }

    private ExaminationResponse toResponse(Examination examination, List<Section> sections) {
        return ExaminationResponse.builder()
                .id(examination.getId())
                .name(examination.getName())
                .durationMinutes(examination.getDurationMinutes())
                .totalMarks(examination.getTotalMarks())
                .negativeMarkingEnabled(examination.isNegativeMarkingEnabled())
                .negativeMarkingValue(examination.getNegativeMarkingValue())
                .navigationPolicy(examination.getNavigationPolicy())
                .calculatorPolicy(examination.getCalculatorPolicy())
                .reviewFlagEnabled(examination.isReviewFlagEnabled())
                .sections(sections)
                .status(examination.getStatus())
                .createdAt(examination.getCreatedAt() != null
                        ? LocalDateTime.ofInstant(examination.getCreatedAt(), ZoneOffset.UTC)
                        : null)
                .build();
    }
}
