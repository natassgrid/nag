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

import com.examplatform.questionbank.io.ImportResult;
import com.examplatform.questionbank.io.QuestionExportService;
import com.examplatform.questionbank.io.QuestionImportService;
import com.examplatform.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

/**
 * REST controller for bulk question import/export.
 *
 * <p>Export streams a ZIP archive of batch files (100 questions each) in JSON or
 * CSV form. Import accepts such a ZIP and creates the questions through the
 * standard creation path.
 *
 * Validates: bulk question interchange (JSON/CSV, batched, compressed).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionIOController {

    /** 50 MB cap on uploaded archives to bound memory use. */
    private static final long MAX_IMPORT_BYTES = 50L * 1024 * 1024;

    private final QuestionExportService exportService;
    private final QuestionImportService importService;

    /**
     * Export questions matching the given filters as a downloadable ZIP archive.
     * Large result sets are split into multiple batch files inside the archive.
     *
     * @param format     "json" (default) or "csv"
     * @param subject    optional subject-name filter
     * @param topic      optional topic-name filter
     * @param difficulty optional difficulty filter
     * @param state      optional lifecycle-state filter
     * @param search     optional free-text filter
     * @param tenantId   tenant identifier from the X-Tenant-Id header
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<StreamingResponseBody> exportQuestions(
            @RequestParam(defaultValue = "json") String format,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String search,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        String fileName = String.format("questions-export-%s-%s.zip",
                format.toLowerCase(), LocalDate.now());

        log.info("Exporting questions: format={}, subject={}, state={}, tenant={}",
                format, subject, state, tenantId);

        StreamingResponseBody body = out -> {
            try {
                exportService.exportToZip(format, subject, topic, difficulty, state, search, tenantId, out);
            } catch (IOException e) {
                log.error("Export failed for tenant {}: {}", tenantId, e.getMessage());
                throw e;
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(body);
    }

    /**
     * Import questions from a ZIP archive of JSON/CSV batch files.
     *
     * @param file     the uploaded ZIP archive (multipart form field "file")
     * @param jwt      the authenticated principal (becomes the author)
     * @param tenantId tenant identifier from the X-Tenant-Id header
     * @return summary of the import (counts + per-record failures)
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ImportResult>> importQuestions(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) throws IOException {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Upload file is required"));
        }
        if (file.getSize() > MAX_IMPORT_BYTES) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Upload exceeds the 50 MB limit"));
        }

        UUID authorId = UUID.fromString(jwt.getSubject());
        log.info("Importing questions: file={}, size={} bytes, author={}, tenant={}",
                file.getOriginalFilename(), file.getSize(), authorId, tenantId);

        ImportResult result = importService.importFromZip(file.getBytes(), authorId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(result,
                "Import completed: " + result.getImported() + " imported, " + result.getFailed() + " failed"));
    }
}
