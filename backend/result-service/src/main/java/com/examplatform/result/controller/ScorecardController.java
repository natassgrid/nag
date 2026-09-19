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

package com.examplatform.result.controller;

import com.examplatform.result.domain.Result;
import com.examplatform.result.dto.ScorecardUrlResponse;
import com.examplatform.result.repository.ResultRepository;
import com.examplatform.result.storage.ScorecardStorageProperties;
import com.examplatform.result.storage.ScorecardStorageProvider;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for candidate scorecards, supporting both direct downloads
 * and time-limited presigned URLs (S3/MinIO/Local).
 *
 * Validates: Requirements 13.3, 13.4, Issue #118
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
public class ScorecardController {

    private final ResultRepository resultRepository;
    private final ScorecardStorageProvider storageProvider;
    private final ScorecardStorageProperties storageProperties;

    /**
     * Retrieves scorecard presigned URL or direct binary stream for a candidate or result ID.
     *
     * <p>If the client requests {@code Accept: application/pdf} or provides {@code download=true},
     * the binary PDF stream is returned directly. Otherwise, returns a JSON response containing
     * a secure, 15-minute presigned download URL.
     *
     * @param id          the candidate UUID or result UUID
     * @param download    optional query parameter to force direct binary stream
     * @param acceptHeader Accept header from client
     * @param tenantId    the tenant identifier from header
     * @param auth        the authenticated user principal
     * @return presigned URL JSON or direct PDF binary
     */
    @GetMapping("/{id}/scorecard")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN', 'SUPER_ADMIN', 'EXAM_CONTROLLER')")
    public ResponseEntity<?> getScorecard(
            @PathVariable UUID id,
            @RequestParam(value = "download", defaultValue = "false") boolean download,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acceptHeader,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            Authentication auth) {

        Result result = findResult(id, tenantId);
        validateAccess(result, auth);

        if (result.getScorecardPdfRef() == null || result.getScorecardPdfRef().isBlank()) {
            throw new EntityNotFoundException("No scorecard available for ID: " + id);
        }

        // Direct PDF stream requested
        boolean requestsPdf = (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_PDF_VALUE)) || download;
        if (requestsPdf) {
            return streamScorecard(result);
        }

        // Return time-limited presigned URL
        return ResponseEntity.ok(buildPresignedUrlResponse(result));
    }

    /**
     * Explicit endpoint to retrieve a 15-minute presigned download URL for a candidate or result.
     *
     * @param id       the candidate UUID or result UUID
     * @param tenantId the tenant identifier
     * @param auth     the authenticated user principal
     * @return ScorecardUrlResponse containing the presigned URL
     */
    @GetMapping("/{id}/scorecard/presigned")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN', 'SUPER_ADMIN', 'EXAM_CONTROLLER')")
    public ResponseEntity<ScorecardUrlResponse> getPresignedUrl(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            Authentication auth) {

        Result result = findResult(id, tenantId);
        validateAccess(result, auth);

        if (result.getScorecardPdfRef() == null || result.getScorecardPdfRef().isBlank()) {
            throw new EntityNotFoundException("No scorecard available for ID: " + id);
        }

        return ResponseEntity.ok(buildPresignedUrlResponse(result));
    }

    /**
     * Direct binary download endpoint for scorecard PDF.
     *
     * @param id       the candidate UUID or result UUID
     * @param tenantId the tenant identifier
     * @param auth     the authenticated user principal
     * @return PDF binary resource
     */
    @GetMapping("/{id}/scorecard/download")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN', 'SUPER_ADMIN', 'EXAM_CONTROLLER')")
    public ResponseEntity<Resource> downloadDirect(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            Authentication auth) {

        Result result = findResult(id, tenantId);
        validateAccess(result, auth);

        if (result.getScorecardPdfRef() == null || result.getScorecardPdfRef().isBlank()) {
            throw new EntityNotFoundException("No scorecard available for ID: " + id);
        }

        return streamScorecard(result);
    }

    private ResponseEntity<Resource> streamScorecard(Result result) {
        Optional<InputStream> streamOpt = storageProvider.download(result.getScorecardPdfRef());
        if (streamOpt.isEmpty()) {
            throw new EntityNotFoundException("Scorecard PDF file not found in storage: " + result.getScorecardPdfRef());
        }

        InputStreamResource resource = new InputStreamResource(streamOpt.get());
        String filename = "scorecard-" + result.getCandidateId() + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    private ScorecardUrlResponse buildPresignedUrlResponse(Result result) {
        int durationMinutes = storageProperties.getS3().getPresignedUrlDurationMinutes() > 0
                ? storageProperties.getS3().getPresignedUrlDurationMinutes()
                : 15;
        Duration duration = Duration.ofMinutes(durationMinutes);
        String presignedUrl = storageProvider.generatePresignedUrl(result.getScorecardPdfRef(), duration);

        return ScorecardUrlResponse.builder()
                .resultId(result.getId())
                .candidateId(result.getCandidateId())
                .examId(result.getExamId())
                .downloadUrl(presignedUrl)
                .expiresInSeconds(duration.toSeconds())
                .expiresAt(Instant.now().plus(duration))
                .storageProvider(storageProvider.name())
                .storageKey(result.getScorecardPdfRef())
                .build();
    }

    private Result findResult(UUID id, String tenantId) {
        // Attempt 1: Lookup by Result ID
        Optional<Result> byResultId = resultRepository.findById(id);
        if (byResultId.isPresent()) {
            return byResultId.get();
        }

        // Attempt 2: Lookup by Candidate ID
        List<Result> results = resultRepository.findByCandidateIdAndTenantId(id, tenantId);
        if (results.isEmpty()) {
            throw new EntityNotFoundException("No results found for ID: " + id);
        }

        return results.stream()
                .filter(r -> r.getScorecardPdfRef() != null)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("No scorecard available for candidate: " + id));
    }

    private void validateAccess(Result result, Authentication auth) {
        if (auth == null) {
            return;
        }

        boolean isElevated = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SUPER_ADMIN")
                        || a.getAuthority().equals("ROLE_EXAM_CONTROLLER"));

        if (!isElevated && auth.getPrincipal() instanceof Jwt jwt) {
            String userId = jwt.getSubject();
            if (userId != null && !userId.equals(result.getCandidateId().toString())) {
                throw new AccessDeniedException("Candidates can only access their own scorecards");
            }
        }
    }
}
