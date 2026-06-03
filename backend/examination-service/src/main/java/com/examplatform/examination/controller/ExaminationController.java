package com.examplatform.examination.controller;

import com.examplatform.examination.dto.CreateExaminationRequest;
import com.examplatform.examination.dto.ExaminationResponse;
import com.examplatform.examination.service.ExaminationService;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for examination CRUD operations.
 *
 * Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5, 7.6
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/examinations")
@RequiredArgsConstructor
public class ExaminationController {

    private final ExaminationService examinationService;

    /**
     * Create a new examination. Requires EXAM_CONTROLLER role.
     */
    @PostMapping
    @PreAuthorize("hasRole('EXAM_CONTROLLER')")
    public ResponseEntity<ExaminationResponse> create(
            @Valid @RequestBody CreateExaminationRequest request) {
        String tenantId = TenantContext.get();
        ExaminationResponse response = examinationService.create(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing examination. Requires EXAM_CONTROLLER role.
     */
    @PutMapping("/{examId}")
    @PreAuthorize("hasRole('EXAM_CONTROLLER')")
    public ResponseEntity<ExaminationResponse> update(
            @PathVariable UUID examId,
            @Valid @RequestBody CreateExaminationRequest request) {
        String tenantId = TenantContext.get();
        ExaminationResponse response = examinationService.update(examId, request, tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve an examination by ID. Requires EXAM_CONTROLLER or SUPER_ADMIN role.
     */
    @GetMapping("/{examId}")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<ExaminationResponse> getById(@PathVariable UUID examId) {
        ExaminationResponse response = examinationService.getById(examId);
        return ResponseEntity.ok(response);
    }
}
