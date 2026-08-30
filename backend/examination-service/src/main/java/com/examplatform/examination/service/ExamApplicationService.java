/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.examination.service;

import com.examplatform.examination.domain.ExamApplication;
import com.examplatform.examination.domain.Examination;
import com.examplatform.examination.dto.ExamApplicationResponse;
import com.examplatform.examination.repository.ExamApplicationRepository;
import com.examplatform.examination.repository.ExaminationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles candidate exam applications: apply, check eligibility, list applied exams.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamApplicationService {

    private final ExamApplicationRepository applicationRepository;
    private final ExaminationRepository examinationRepository;

    /**
     * Apply a candidate for a specific examination.
     * Throws DuplicateKeyException if already applied (results in HTTP 409).
     *
     * @param examId      the examination UUID
     * @param candidateId the candidate UUID (from JWT subject)
     * @param tenantId    tenant identifier
     * @return the created application response
     */
    @Transactional
    public ExamApplicationResponse apply(UUID examId, UUID candidateId, String tenantId) {
        // Check exam exists and is PUBLISHED
        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new EntityNotFoundException("Examination not found: " + examId));

        if (!"PUBLISHED".equalsIgnoreCase(exam.getStatus()) && !"ACTIVE".equalsIgnoreCase(exam.getStatus())) {
            throw new IllegalStateException("Exam is not open for applications. Status: " + exam.getStatus());
        }

        // Prevent duplicate application
        if (applicationRepository.existsByCandidateIdAndExaminationIdAndTenantId(candidateId, examId, tenantId)) {
            throw new DuplicateKeyException("Candidate " + candidateId + " has already applied for exam " + examId);
        }

        ExamApplication application = ExamApplication.builder()
                .candidateId(candidateId)
                .examinationId(examId)
                .tenantId(tenantId)
                .status("APPLIED")
                .build();

        application = applicationRepository.save(application);

        log.info("Candidate {} applied for exam {} (tenant {})", candidateId, examId, tenantId);

        return toResponse(application, exam.getName());
    }

    /**
     * List all exams the candidate has applied for.
     *
     * @param candidateId the candidate UUID
     * @param tenantId    tenant identifier
     * @return list of application responses
     */
    @Transactional(readOnly = true)
    public List<ExamApplicationResponse> getMyApplications(UUID candidateId, String tenantId) {
        return applicationRepository.findByCandidateIdAndTenantId(candidateId, tenantId)
                .stream()
                .map(app -> {
                    String examName = examinationRepository.findById(app.getExaminationId())
                            .map(Examination::getName)
                            .orElse("Unknown Exam");
                    return toResponse(app, examName);
                })
                .collect(Collectors.toList());
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private ExamApplicationResponse toResponse(ExamApplication app, String examName) {
        return ExamApplicationResponse.builder()
                .applicationId(app.getId())
                .examId(app.getExaminationId())
                .candidateId(app.getCandidateId())
                .status(app.getStatus())
                .applicationDate(app.getAppliedAt())
                .hallTicketNumber(app.getHallTicketNumber())
                .examName(examName)
                .build();
    }
}
