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
