package com.examplatform.papergenerator.controller;

import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.dto.PaperGenerationRequest;
import com.examplatform.papergenerator.service.PaperAssemblyService;
import com.examplatform.papergenerator.service.PaperSerializer;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for paper generation endpoints.
 * Accepts blueprint-driven paper generation requests and returns
 * 202 Accepted with the generated paper ID.
 *
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4, 28.1, 28.2, 28.3, 28.5
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperAssemblyService paperAssemblyService;
    private final PaperSerializer paperSerializer;

    /**
     * Submits an async paper generation job.
     * Requires the EXAM_CONTROLLER role.
     *
     * @param request the paper generation request with blueprint rules
     * @param jwt     the authenticated user's JWT
     * @return 202 Accepted with the paper ID
     */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('EXAM_CONTROLLER')")
    public ResponseEntity<Map<String, Object>> generatePaper(
            @Valid @RequestBody PaperGenerationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID generatedBy = UUID.fromString(jwt.getSubject());
        String tenantId = TenantContext.get() != null ? TenantContext.get() : "default";

        log.info("Paper generation requested by user={}, examId={}, shiftId={}",
                generatedBy, request.getExamId(), request.getShiftId());

        Paper paper = paperAssemblyService.generatePaper(request, generatedBy, tenantId);

        Map<String, Object> response = Map.of(
                "paperId", paper.getId(),
                "status", paper.getStatus(),
                "message", "Paper generation submitted successfully"
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Validates a paper JSON document against the expected schema.
     * Returns 200 OK if valid, or 422 Unprocessable Entity with validation errors.
     *
     * @param json the paper JSON string to validate
     * @return 200 OK or 422 with validation error details
     */
    @PostMapping("/validate")
    @PreAuthorize("hasRole('EXAM_CONTROLLER')")
    public ResponseEntity<Map<String, Object>> validatePaper(@RequestBody String json) {
        log.info("Paper schema validation requested");

        List<String> errors = paperSerializer.validate(json);

        if (errors.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "message", "Paper document is valid"
            ));
        }

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                "valid", false,
                "errors", errors
        ));
    }
}
