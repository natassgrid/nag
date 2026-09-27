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
import com.examplatform.examination.domain.enums.CalculatorPolicy;
import com.examplatform.examination.domain.enums.NavigationPolicy;
import com.examplatform.examination.dto.CreateExaminationRequest;
import com.examplatform.examination.dto.ExaminationResponse;
import com.examplatform.examination.exception.ExaminationNotFoundException;
import com.examplatform.examination.exception.SectionMarksValidationException;
import com.examplatform.examination.repository.ExaminationRepository;
import com.examplatform.shared.messaging.EventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final EventPublisher eventPublisher;

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
     * Lists examinations with server-side pagination, optional search filter, and dynamic sorting.
     */
    public Page<ExaminationResponse> listByTenantPaged(
            String tenantId, String search, String sort, String order, int page, int size) {
        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProp = resolveExamSortProperty(sort);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProp));

        Page<Examination> examPage;
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

    public Page<ExaminationResponse> listByTenantPaged(
            String tenantId, String search, int page, int size) {
        return listByTenantPaged(tenantId, search, null, "desc", page, size);
    }

    /**
     * Lists PUBLISHED examinations for the given tenant with pagination, search, and category filters.
     * Used by the candidate-facing public endpoint — no admin role required.
     *
     * @param tenantId tenant identifier
     * @param search   optional search term matched against exam name
     * @param category optional examination category (e.g. "ENGINEERING", "CIVIL_SERVICES")
     * @param page     zero-based page number
     * @param size     page size
     * @return paginated list of published examinations
     */
    public Page<ExaminationResponse> listPublishedPaged(
            String tenantId, String search, String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        boolean hasSearch = search != null && !search.isBlank();
        boolean hasCategory = category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL");

        Page<Examination> examPage;
        if (hasSearch && hasCategory) {
            examPage = examinationRepository.findByStatusAndTenantIdAndCategoryIgnoreCaseAndNameContainingIgnoreCase(
                    "PUBLISHED", tenantId, category.trim(), search.trim(), pageable);
        } else if (hasCategory) {
            examPage = examinationRepository.findByStatusAndTenantIdAndCategoryIgnoreCase(
                    "PUBLISHED", tenantId, category.trim(), pageable);
        } else if (hasSearch) {
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

    public Page<ExaminationResponse> listPublishedPaged(
            String tenantId, String search, int page, int size) {
        return listPublishedPaged(tenantId, search, null, page, size);
    }

    /**
     * Creates a new examination in DRAFT status.
     * Validates that sum(section.marksPerQuestion × section.questionCount) == totalMarks.
     */
    public ExaminationResponse create(CreateExaminationRequest request, String tenantId) {
        validateSectionMarks(request);

        String sectionsJson = serializeSections(request.getSections());

        boolean isNegMarking = Boolean.TRUE.equals(request.getNegativeMarkingEnabled());
        double negValue = (isNegMarking && request.getNegativeMarkingValue() != null)
                ? request.getNegativeMarkingValue()
                : 0.0;

        Examination examination = Examination.builder()
                .name(request.getName())
                .code(request.getCode())
                .conductingAuthority(request.getConductingAuthority())
                .category(request.getCategory())
                .examinationType(request.getExaminationType())
                .academicYear(request.getAcademicYear())
                .examinationMode(request.getExaminationMode())
                .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 0)
                .totalMarks(request.getTotalMarks() != null ? request.getTotalMarks() : 0)
                .negativeMarkingEnabled(isNegMarking)
                .negativeMarkingValue(negValue)
                .navigationPolicy(request.getNavigationPolicy() != null ? request.getNavigationPolicy().name() : NavigationPolicy.FLEXIBLE.name())
                .calculatorPolicy(request.getCalculatorPolicy() != null ? request.getCalculatorPolicy().name() : CalculatorPolicy.NONE.name())
                .reviewFlagEnabled(Boolean.TRUE.equals(request.getReviewFlagEnabled()))
                .isPractice(Boolean.TRUE.equals(request.getIsPractice()))
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

        boolean isNegMarking = Boolean.TRUE.equals(request.getNegativeMarkingEnabled());
        double negValue = (isNegMarking && request.getNegativeMarkingValue() != null)
                ? request.getNegativeMarkingValue()
                : 0.0;

        examination.setName(request.getName());
        examination.setCode(request.getCode());
        examination.setConductingAuthority(request.getConductingAuthority());
        examination.setCategory(request.getCategory());
        examination.setExaminationType(request.getExaminationType());
        examination.setAcademicYear(request.getAcademicYear());
        examination.setExaminationMode(request.getExaminationMode());
        examination.setDurationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 0);
        examination.setTotalMarks(request.getTotalMarks() != null ? request.getTotalMarks() : 0);
        examination.setNegativeMarkingEnabled(isNegMarking);
        examination.setNegativeMarkingValue(negValue);
        examination.setNavigationPolicy(request.getNavigationPolicy() != null ? request.getNavigationPolicy().name() : NavigationPolicy.FLEXIBLE.name());
        examination.setCalculatorPolicy(request.getCalculatorPolicy() != null ? request.getCalculatorPolicy().name() : CalculatorPolicy.NONE.name());
        examination.setReviewFlagEnabled(Boolean.TRUE.equals(request.getReviewFlagEnabled()));
        if (request.getIsPractice() != null) {
            examination.setPractice(request.getIsPractice());
        }
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
            event.put("tenantId", tenantId);
            event.put("timestamp", Instant.now().toEpochMilli());
            eventPublisher.publish(AUDIT_TOPIC, saved.getId().toString(), event);
        } catch (Exception ex) {
            log.warn("Failed to publish EXAM_PUBLISHED audit event for examId={}: {}", saved.getId(), ex.getMessage());
        }

        List<Section> sections = deserializeSections(saved.getSectionsJson());
        return toResponse(saved, sections);
    }

    /**
     * Closes an examination: sets status to CLOSED.
     */
    public ExaminationResponse close(UUID examId, String tenantId) {
        Examination examination = examinationRepository.findById(examId)
                .orElseThrow(() -> new ExaminationNotFoundException(examId));

        if (!examination.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Cannot close examination belonging to another tenant");
        }

        examination.setStatus("CLOSED");
        Examination saved = examinationRepository.save(examination);
        log.info("Closed examination id={} for tenant={}", saved.getId(), tenantId);

        List<Section> sections = deserializeSections(saved.getSectionsJson());
        return toResponse(saved, sections);
    }

    private void validateSectionMarks(CreateExaminationRequest request) {
        if (request.getSections() == null || request.getSections().isEmpty()) {
            return;
        }

        double computedTotal = request.getSections().stream()
                .mapToDouble(s -> {
                    double mpq = s.getMarksPerQuestion() != null ? s.getMarksPerQuestion() : 0.0;
                    int qc = s.getQuestionCount() != null ? s.getQuestionCount() : 0;
                    return mpq * qc;
                })
                .sum();

        int declaredTotal = request.getTotalMarks() != null ? request.getTotalMarks() : 0;

        if (Math.abs(computedTotal - declaredTotal) > 0.001) {
            throw new SectionMarksValidationException((int) Math.round(computedTotal), declaredTotal);
        }
    }

    private String serializeSections(List<Section> sections) {
        if (sections == null || sections.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(sections);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize sections to JSON", e);
        }
    }

    private List<Section> deserializeSections(String sectionsJson) {
        if (sectionsJson == null || sectionsJson.isBlank() || "[]".equals(sectionsJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(sectionsJson, new TypeReference<List<Section>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize sectionsJson: {}", e.getMessage());
            return List.of();
        }
    }

    private String resolveExamSortProperty(String sort) {
        if (sort == null || sort.isBlank()) {
            return "createdAt";
        }
        return switch (sort.toLowerCase()) {
            case "name" -> "name";
            case "code" -> "code";
            case "duration", "durationminutes" -> "durationMinutes";
            case "marks", "totalmarks" -> "totalMarks";
            case "status" -> "status";
            case "category" -> "category";
            default -> "createdAt";
        };
    }

    private ExaminationResponse toResponse(Examination exam, List<Section> sections) {
        return ExaminationResponse.builder()
                .id(exam.getId())
                .name(exam.getName())
                .code(exam.getCode())
                .conductingAuthority(exam.getConductingAuthority())
                .category(exam.getCategory())
                .examinationType(exam.getExaminationType())
                .academicYear(exam.getAcademicYear())
                .examinationMode(exam.getExaminationMode())
                .durationMinutes(exam.getDurationMinutes())
                .totalMarks(exam.getTotalMarks())
                .negativeMarkingEnabled(exam.isNegativeMarkingEnabled())
                .negativeMarkingValue(exam.getNegativeMarkingValue())
                .navigationPolicy(exam.getNavigationPolicy())
                .calculatorPolicy(exam.getCalculatorPolicy())
                .reviewFlagEnabled(exam.isReviewFlagEnabled())
                .isPractice(exam.isPractice())
                .sections(sections != null ? sections : List.of())
                .status(exam.getStatus())
                .build();
    }
}
