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

package com.examplatform.analytics.controller;

import com.examplatform.analytics.domain.ExamAnalytics;
import com.examplatform.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for exam analytics endpoints.
 * Accessible to EXAM_CONTROLLER and SUPER_ADMIN roles.
 */
@RestController
@RequestMapping("/api/v1/analytics/exams")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Returns computed analytics for a given exam.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<ExamAnalytics> getExamAnalytics(@PathVariable UUID id) {
        ExamAnalytics analytics = analyticsService.getAnalyticsForExam(id);
        return ResponseEntity.ok(analytics);
    }

    /**
     * Exports analytics data in the specified format (csv or pdf).
     */
    @GetMapping("/{id}/export")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportExamAnalytics(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "csv") String format) {

        byte[] data = analyticsService.exportAnalytics(id, format);

        HttpHeaders headers = new HttpHeaders();
        switch (format.toLowerCase()) {
            case "pdf" -> {
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analytics_" + id + ".pdf");
            }
            default -> {
                headers.setContentType(MediaType.parseMediaType("text/csv"));
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analytics_" + id + ".csv");
            }
        }

        return ResponseEntity.ok().headers(headers).body(data);
    }
}
