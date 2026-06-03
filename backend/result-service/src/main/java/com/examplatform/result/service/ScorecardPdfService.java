package com.examplatform.result.service;

import com.examplatform.result.domain.Result;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Generates password-protected PDF scorecards for candidates.
 * Password is composed of dateOfBirth + candidateId (e.g., "1995-01-15CAND-12345").
 *
 * Content includes: candidate identifier (no PII), exam name, total score,
 * section-wise breakdown, and topic-wise breakdown.
 *
 * Validates: Requirements 13.3, 13.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScorecardPdfService {

    @Value("${scorecard.storage.path:./scorecards}")
    private String storagePath;

    private final ObjectMapper objectMapper;

    /**
     * Generates a password-protected PDF scorecard for the given result.
     *
     * @param result       the candidate's exam result
     * @param dateOfBirth  candidate's date of birth (format: yyyy-MM-dd)
     * @param candidateId  candidate's identifier (e.g., "CAND-12345")
     * @return the PDF reference/path stored in the filesystem
     */
    public String generateScorecard(Result result, String dateOfBirth, String candidateId) {
        String password = dateOfBirth + candidateId;
        String filename = "scorecard-" + result.getId() + ".pdf";
        Path outputPath = Paths.get(storagePath, filename);

        try {
            Files.createDirectories(outputPath.getParent());

            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);

                writeScorecardContent(document, page, result, candidateId);

                // Apply password protection
                AccessPermission permissions = new AccessPermission();
                permissions.setCanPrint(true);
                permissions.setCanModify(false);
                permissions.setCanExtractContent(false);

                StandardProtectionPolicy policy = new StandardProtectionPolicy(
                        password, password, permissions);
                policy.setEncryptionKeyLength(128);
                document.protect(policy);

                document.save(outputPath.toFile());
            }

            log.info("Scorecard PDF generated: path={}, candidateId={}, resultId={}",
                    outputPath, candidateId, result.getId());

            return outputPath.toString();

        } catch (IOException e) {
            log.error("Failed to generate scorecard PDF for result: {}", result.getId(), e);
            throw new RuntimeException("Failed to generate scorecard PDF", e);
        }
    }

    /**
     * Writes the scorecard content to the PDF document.
     */
    private void writeScorecardContent(PDDocument document, PDPage page, Result result, String candidateId) throws IOException {
        PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float yPosition = 750;
            float leftMargin = 50;

            // Title
            contentStream.beginText();
            contentStream.setFont(fontBold, 18);
            contentStream.newLineAtOffset(leftMargin, yPosition);
            contentStream.showText("Examination Scorecard");
            contentStream.endText();
            yPosition -= 40;

            // Candidate Identifier (no PII)
            contentStream.beginText();
            contentStream.setFont(fontRegular, 12);
            contentStream.newLineAtOffset(leftMargin, yPosition);
            contentStream.showText("Candidate ID: " + candidateId);
            contentStream.endText();
            yPosition -= 25;

            // Exam Name
            contentStream.beginText();
            contentStream.setFont(fontRegular, 12);
            contentStream.newLineAtOffset(leftMargin, yPosition);
            contentStream.showText("Exam ID: " + result.getExamId());
            contentStream.endText();
            yPosition -= 25;

            // Total Score
            contentStream.beginText();
            contentStream.setFont(fontBold, 14);
            contentStream.newLineAtOffset(leftMargin, yPosition);
            contentStream.showText("Total Score: " + result.getTotalScore());
            contentStream.endText();
            yPosition -= 25;

            // Overall Rank
            if (result.getOverallRank() != null) {
                contentStream.beginText();
                contentStream.setFont(fontRegular, 12);
                contentStream.newLineAtOffset(leftMargin, yPosition);
                contentStream.showText("Overall Rank: " + result.getOverallRank());
                contentStream.endText();
                yPosition -= 25;
            }

            // Overall Percentile
            if (result.getOverallPercentile() != null) {
                contentStream.beginText();
                contentStream.setFont(fontRegular, 12);
                contentStream.newLineAtOffset(leftMargin, yPosition);
                contentStream.showText("Overall Percentile: " + result.getOverallPercentile());
                contentStream.endText();
                yPosition -= 25;
            }

            // Section-wise Breakdown
            yPosition -= 15;
            contentStream.beginText();
            contentStream.setFont(fontBold, 14);
            contentStream.newLineAtOffset(leftMargin, yPosition);
            contentStream.showText("Section-wise Breakdown");
            contentStream.endText();
            yPosition -= 25;

            yPosition = writeSectionScores(contentStream, fontRegular, leftMargin, yPosition,
                    result.getSectionScoresJson());
        }
    }

    /**
     * Parses and writes section scores from the JSON field.
     */
    private float writeSectionScores(PDPageContentStream contentStream, PDType1Font font,
                                     float leftMargin, float yPosition, String sectionScoresJson) throws IOException {
        if (sectionScoresJson == null || sectionScoresJson.isBlank()) {
            contentStream.beginText();
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(leftMargin, yPosition);
            contentStream.showText("No section data available");
            contentStream.endText();
            return yPosition - 20;
        }

        try {
            List<Map<String, Object>> sections = objectMapper.readValue(
                    sectionScoresJson, new TypeReference<>() {});

            for (Map<String, Object> section : sections) {
                String sectionName = String.valueOf(section.getOrDefault("sectionName", "Unknown"));
                Object score = section.getOrDefault("score", "N/A");
                Object maxScore = section.getOrDefault("maxScore", "N/A");

                contentStream.beginText();
                contentStream.setFont(font, 11);
                contentStream.newLineAtOffset(leftMargin + 10, yPosition);
                contentStream.showText(sectionName + ": " + score + " / " + maxScore);
                contentStream.endText();
                yPosition -= 20;

                // Topic-wise breakdown within section
                if (section.containsKey("topics")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> topics = (List<Map<String, Object>>) section.get("topics");
                    for (Map<String, Object> topic : topics) {
                        String topicName = String.valueOf(topic.getOrDefault("topicName", "Unknown"));
                        Object topicScore = topic.getOrDefault("score", "N/A");

                        contentStream.beginText();
                        contentStream.setFont(font, 10);
                        contentStream.newLineAtOffset(leftMargin + 30, yPosition);
                        contentStream.showText("- " + topicName + ": " + topicScore);
                        contentStream.endText();
                        yPosition -= 18;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse section scores JSON, writing raw", e);
            contentStream.beginText();
            contentStream.setFont(font, 10);
            contentStream.newLineAtOffset(leftMargin, yPosition);
            contentStream.showText("Section data: " + sectionScoresJson.substring(0, Math.min(80, sectionScoresJson.length())));
            contentStream.endText();
            yPosition -= 20;
        }

        return yPosition;
    }
}
