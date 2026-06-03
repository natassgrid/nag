package com.examplatform.analytics.service;

import com.examplatform.analytics.domain.ExamAnalytics;
import com.examplatform.analytics.repository.ExamAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for computing and exporting exam analytics.
 * Currently returns mock data; will be populated with real computation logic
 * once result data consumption from Kafka is fully wired.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final ExamAnalyticsRepository examAnalyticsRepository;

    /**
     * Retrieves the latest computed analytics for the given exam.
     * If no computed analytics exist yet, returns mock data.
     */
    public ExamAnalytics getAnalyticsForExam(UUID examId) {
        Optional<ExamAnalytics> existing = examAnalyticsRepository.findTopByExamIdOrderByComputedAtDesc(examId);
        return existing.orElseGet(() -> buildMockAnalytics(examId));
    }

    /**
     * Exports analytics data in the specified format (csv or pdf).
     * Returns raw byte content for the export.
     */
    public byte[] exportAnalytics(UUID examId, String format) {
        ExamAnalytics analytics = getAnalyticsForExam(examId);

        return switch (format.toLowerCase()) {
            case "csv" -> exportAsCsv(analytics);
            case "pdf" -> exportAsPdf(analytics);
            default -> throw new IllegalArgumentException("Unsupported export format: " + format + ". Supported: csv, pdf");
        };
    }

    private byte[] exportAsCsv(ExamAnalytics analytics) {
        StringBuilder csv = new StringBuilder();
        csv.append("exam_id,total_registered,total_appeared,top_10_percentile,bottom_10_percentile,computed_at\n");
        csv.append(String.format("%s,%d,%d,%s,%s,%s\n",
                analytics.getExamId(),
                analytics.getTotalRegistered(),
                analytics.getTotalAppeared(),
                analytics.getTop10PercentileThreshold(),
                analytics.getBottom10PercentileThreshold(),
                analytics.getComputedAt()));
        return csv.toString().getBytes();
    }

    private byte[] exportAsPdf(ExamAnalytics analytics) {
        // Stub: returns a minimal PDF placeholder.
        // Full implementation will use Apache PDFBox for proper PDF generation.
        log.info("Generating PDF export for exam: {}", analytics.getExamId());
        String pdfContent = String.format(
                "Exam Analytics Report\n" +
                "Exam ID: %s\n" +
                "Total Registered: %d\n" +
                "Total Appeared: %d\n" +
                "Top 10th Percentile: %s\n" +
                "Bottom 10th Percentile: %s\n" +
                "Computed At: %s\n",
                analytics.getExamId(),
                analytics.getTotalRegistered(),
                analytics.getTotalAppeared(),
                analytics.getTop10PercentileThreshold(),
                analytics.getBottom10PercentileThreshold(),
                analytics.getComputedAt());
        return pdfContent.getBytes();
    }

    private ExamAnalytics buildMockAnalytics(UUID examId) {
        return ExamAnalytics.builder()
                .id(UUID.randomUUID())
                .examId(examId)
                .totalRegistered(50000L)
                .totalAppeared(47500L)
                .scoreDistributionJson("{\"0-10\":500,\"10-20\":2000,\"20-30\":5000,\"30-40\":10000,\"40-50\":12000,\"50-60\":8000,\"60-70\":5000,\"70-80\":3000,\"80-90\":1500,\"90-100\":500}")
                .sectionAveragesJson("{\"Physics\":42.5,\"Chemistry\":38.7,\"Mathematics\":35.2}")
                .top10PercentileThreshold(new BigDecimal("78.5000"))
                .bottom10PercentileThreshold(new BigDecimal("15.2000"))
                .computedAt(Instant.now())
                .build();
    }
}
