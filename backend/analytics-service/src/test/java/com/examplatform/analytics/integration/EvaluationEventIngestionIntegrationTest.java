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

package com.examplatform.analytics.integration;

import com.examplatform.analytics.consumer.EvaluationCompletedConsumer;
import com.examplatform.analytics.domain.CandidateAnalyticsResult;
import com.examplatform.analytics.domain.ExamAnalytics;
import com.examplatform.analytics.repository.CandidateAnalyticsResultRepository;
import com.examplatform.analytics.repository.ExamAnalyticsRepository;
import com.examplatform.analytics.service.AnalyticsService;
import com.examplatform.analytics.support.AbstractIntegrationTest;
import com.examplatform.shared.messaging.GenericDomainEvent;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Analytics Pipeline End-to-End Ingestion and Aggregation Integration Tests")
class EvaluationEventIngestionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EvaluationCompletedConsumer evaluationCompletedConsumer;

    @Autowired
    private CandidateAnalyticsResultRepository candidateResultRepository;

    @Autowired
    private ExamAnalyticsRepository examAnalyticsRepository;

    @Autowired
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("End-to-End: Ingest evaluation completed events -> Persist results -> Update ExamAnalytics -> Export PDF")
    void testEndToEndAnalyticsPipeline() throws IOException {
        UUID examId = UUID.randomUUID();
        UUID candidate1 = UUID.randomUUID();
        UUID candidate2 = UUID.randomUUID();
        UUID candidate3 = UUID.randomUUID();

        // 1. Ingest 3 evaluation completed events via Spring domain event mechanism
        sendEvaluationCompletedEvent(examId, candidate1, 40.0, Map.of("Physics", 20.0, "Chemistry", 20.0));
        sendEvaluationCompletedEvent(examId, candidate2, 70.0, Map.of("Physics", 35.0, "Chemistry", 35.0));
        sendEvaluationCompletedEvent(examId, candidate3, 90.0, Map.of("Physics", 45.0, "Chemistry", 45.0));

        // 2. Verify candidate results were persisted in DB
        List<CandidateAnalyticsResult> candidateResults = candidateResultRepository.findByExamId(examId);
        assertThat(candidateResults).hasSize(3);

        // 3. Verify ExamAnalytics is automatically created and aggregated
        Optional<ExamAnalytics> analyticsOpt = examAnalyticsRepository.findTopByExamIdOrderByComputedAtDesc(examId);
        assertThat(analyticsOpt).isPresent();
        ExamAnalytics analytics = analyticsOpt.get();

        assertThat(analytics.getTotalAppeared()).isEqualTo(3L);
        assertThat(analytics.getTotalRegistered()).isGreaterThanOrEqualTo(3L);
        assertThat(analytics.getScoreDistributionJson()).contains("30-40");
        assertThat(analytics.getSectionAveragesJson()).contains("Physics");
        assertThat(analytics.getSectionAveragesJson()).contains("Chemistry");

        // 4. Verify batch recomputation endpoint works
        ExamAnalytics recomputed = analyticsService.computeAnalyticsForExam(examId);
        assertThat(recomputed.getTotalAppeared()).isEqualTo(3L);

        // 5. Verify PDF export generates real structured PDF Box document
        byte[] pdfBytes = analyticsService.exportAnalytics(examId, "pdf");
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(0);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            assertThat(text).contains("NATIONAL ASSESSMENT GRID (NAG)");
            assertThat(text).contains(examId.toString());
            assertThat(text).contains("Physics");
            assertThat(text).contains("Chemistry");
        }
    }

    private void sendEvaluationCompletedEvent(UUID examId, UUID candidateId, double totalRawScore, Map<String, Double> sectionScores) {
        Map<String, Object> payload = Map.of(
                "eventType", "EVALUATION_COMPLETED",
                "examId", examId.toString(),
                "candidateId", candidateId.toString(),
                "sessionId", UUID.randomUUID().toString(),
                "totalRawScore", totalRawScore,
                "sectionScores", sectionScores,
                "tenantId", "default",
                "evaluatedAt", Instant.now().toString()
        );

        evaluationCompletedConsumer.onSpringEvaluationCompleted(new GenericDomainEvent(
                EvaluationCompletedConsumer.EVALUATION_COMPLETED_TOPIC,
                candidateId.toString(),
                payload
        ));
    }
}
