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

package com.examplatform.result.service;

import com.examplatform.result.domain.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ScorecardPdfService.
 * Validates: Requirements 13.3, 13.4
 */
class ScorecardPdfServiceTest {

    private ScorecardPdfService scorecardPdfService;

    @TempDir
    Path tempDir;

    private Result testResult;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        scorecardPdfService = new ScorecardPdfService(objectMapper);

        // Set the storage path using reflection since @Value won't be available in unit test
        Field storagePathField = ScorecardPdfService.class.getDeclaredField("storagePath");
        storagePathField.setAccessible(true);
        storagePathField.set(scorecardPdfService, tempDir.toString());

        testResult = Result.builder()
                .candidateId(UUID.randomUUID())
                .examId(UUID.randomUUID())
                .totalScore(new BigDecimal("85.50"))
                .overallRank(42)
                .overallPercentile(new BigDecimal("92.500"))
                .sectionScoresJson("[{\"sectionName\":\"Physics\",\"score\":30,\"maxScore\":40,\"topics\":[{\"topicName\":\"Mechanics\",\"score\":15},{\"topicName\":\"Optics\",\"score\":15}]},{\"sectionName\":\"Chemistry\",\"score\":25,\"maxScore\":30}]")
                .digiLockerPushed(false)
                .build();

        // Manually set the ID using reflection on BaseEntity
        Field idField = testResult.getClass().getSuperclass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(testResult, UUID.randomUUID());
    }

    @Test
    @DisplayName("Generates PDF with correct password protection")
    void generateScorecard_createsPasswordProtectedPdf() throws IOException {
        String dateOfBirth = "1995-01-15";
        String candidateId = "CAND-12345";
        String password = dateOfBirth + candidateId;

        String pdfPath = scorecardPdfService.generateScorecard(testResult, dateOfBirth, candidateId);

        File pdfFile = new File(pdfPath);
        assertThat(pdfFile).exists();

        // Verify that PDF is encrypted and requires password
        try (PDDocument doc = Loader.loadPDF(pdfFile, password)) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("Generated PDF contains candidate ID and score sections")
    void generateScorecard_pdfContainsExpectedContent() throws IOException {
        String dateOfBirth = "1995-01-15";
        String candidateId = "CAND-12345";
        String password = dateOfBirth + candidateId;

        String pdfPath = scorecardPdfService.generateScorecard(testResult, dateOfBirth, candidateId);

        try (PDDocument doc = Loader.loadPDF(new File(pdfPath), password)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            assertThat(text).contains("Examination Scorecard");
            assertThat(text).contains("Candidate ID: CAND-12345");
            assertThat(text).contains("Total Score: 85.50");
            assertThat(text).contains("Section-wise Breakdown");
            assertThat(text).contains("Physics");
            assertThat(text).contains("Chemistry");
            assertThat(text).contains("Mechanics");
            assertThat(text).contains("Optics");
        }
    }

    @Test
    @DisplayName("PDF cannot be opened with wrong password")
    void generateScorecard_wrongPasswordFails() throws IOException {
        String dateOfBirth = "1995-01-15";
        String candidateId = "CAND-12345";

        String pdfPath = scorecardPdfService.generateScorecard(testResult, dateOfBirth, candidateId);

        File pdfFile = new File(pdfPath);
        assertThat(pdfFile).exists();

        // Trying with wrong password should fail
        assertThatThrownBy(() -> {
            try (PDDocument doc = Loader.loadPDF(pdfFile, "wrong-password")) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.getText(doc);
            }
        }).isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("Generates PDF even with null section scores")
    void generateScorecard_nullSectionScores_generatesSuccessfully() throws IOException {
        testResult.setSectionScoresJson(null);

        String dateOfBirth = "2000-06-15";
        String candidateId = "CAND-99999";
        String password = dateOfBirth + candidateId;

        String pdfPath = scorecardPdfService.generateScorecard(testResult, dateOfBirth, candidateId);

        File pdfFile = new File(pdfPath);
        assertThat(pdfFile).exists();

        try (PDDocument doc = Loader.loadPDF(pdfFile, password)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            assertThat(text).contains("No section data available");
        }
    }

    @Test
    @DisplayName("Returns valid file path reference")
    void generateScorecard_returnsValidPath() {
        String pdfPath = scorecardPdfService.generateScorecard(testResult, "1990-01-01", "CAND-001");

        assertThat(pdfPath).contains("scorecard-");
        assertThat(pdfPath).endsWith(".pdf");
        assertThat(new File(pdfPath)).exists();
    }
}
