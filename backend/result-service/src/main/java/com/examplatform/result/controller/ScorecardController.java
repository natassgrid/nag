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
import com.examplatform.result.repository.ResultRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for downloading scorecard PDFs.
 *
 * Validates: Requirements 13.3, 13.4
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
public class ScorecardController {

    private final ResultRepository resultRepository;

    /**
     * Downloads the scorecard PDF for a given candidate.
     *
     * @param candidateId the candidate's UUID
     * @param tenantId    the tenant identifier from header
     * @return the PDF file as a downloadable response
     */
    @GetMapping("/{candidateId}/scorecard")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<Resource> downloadScorecard(
            @PathVariable UUID candidateId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {

        List<Result> results = resultRepository.findByCandidateIdAndTenantId(candidateId, tenantId);

        if (results.isEmpty()) {
            throw new EntityNotFoundException("No results found for candidate: " + candidateId);
        }

        // Get the most recent result with a scorecard
        Result result = results.stream()
                .filter(r -> r.getScorecardPdfRef() != null)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "No scorecard available for candidate: " + candidateId));

        File pdfFile = new File(result.getScorecardPdfRef());
        if (!pdfFile.exists()) {
            throw new EntityNotFoundException("Scorecard PDF file not found on disk");
        }

        Resource resource = new FileSystemResource(pdfFile);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"scorecard-" + candidateId + ".pdf\"")
                .body(resource);
    }
}
