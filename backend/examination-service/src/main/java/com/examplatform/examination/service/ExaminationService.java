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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
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

    private final ExaminationRepository examinationRepository;
    private final ObjectMapper objectMapper;

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
