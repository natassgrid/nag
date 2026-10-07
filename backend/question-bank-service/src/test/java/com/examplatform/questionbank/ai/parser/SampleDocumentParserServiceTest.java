/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 Open Digital Public Infrastructure (DPI) Platform Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */
package com.examplatform.questionbank.ai.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SampleDocumentParserService Tiered Ladder Tests")
class SampleDocumentParserServiceTest {

    private SampleDocumentParserService parserService;

    @BeforeEach
    void setUp() {
        parserService = new SampleDocumentParserService();
    }

    private byte[] createCleanTextPdf(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createPdfWithEmbeddedImage(String text, int imgWidth, int imgHeight) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            BufferedImage image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.BLUE);
            g.fillRect(0, 0, imgWidth, imgHeight);
            g.dispose();

            PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();

                cs.drawImage(pdImage, 50, 500, 100, 100);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createScannedPdf(int imgWidth, int imgHeight) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            // Empty digital text stream — only raster background
            BufferedImage image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, imgWidth, imgHeight);
            g.setColor(Color.BLACK);
            g.drawString("Scanned Question Text", 50, 50);
            g.dispose();

            PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.drawImage(pdImage, 0, 0, 600, 800);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    @DisplayName("Tier 0: Pure text PDF extracts digital text stream with zero vision tokens")
    void testTier0PureTextPdf() throws IOException {
        String content = "What is the acceleration due to gravity on Earth? A) 9.8 m/s^2 B) 10.5 m/s^2";
        byte[] pdfBytes = createCleanTextPdf(content);

        List<NormalizedSampleQuestion> questions = parserService.parsePdfDocument(pdfBytes);

        assertThat(questions).hasSize(1);
        NormalizedSampleQuestion q = questions.getFirst();
        assertThat(q.getParsingTier()).isEqualTo("TIER_0_LOCAL_TEXT");
        assertThat(q.getStem()).contains("acceleration due to gravity");
        assertThat(q.isHasDiagram()).isFalse();
        assertThat(q.getEmbeddedMedia()).isNull();
    }

    @Test
    @DisplayName("Tier 1: PDF with embedded image isolates diagram directly without full-page high-DPI rasterization")
    void testTier1EmbeddedImageExtraction() throws IOException {
        String content = "Refer to the circuit diagram below and calculate current I: A) 2A B) 4A";
        byte[] pdfBytes = createPdfWithEmbeddedImage(content, 1200, 800);

        List<NormalizedSampleQuestion> questions = parserService.parsePdfDocument(pdfBytes);

        assertThat(questions).hasSize(1);
        NormalizedSampleQuestion q = questions.getFirst();
        assertThat(q.getParsingTier()).isEqualTo("TIER_1_SELECTIVE_VISION");
        assertThat(q.isHasDiagram()).isTrue();
        assertThat(q.getEmbeddedMedia()).isNotNull();
        assertThat(q.getEmbeddedMedia()).startsWith("data:image/jpeg;base64,");
    }

    @Test
    @DisplayName("Tier 2: Scanned PDF fallback renders at 150 DPI and downsamples")
    void testTier2ScannedPdfFallback() throws IOException {
        byte[] pdfBytes = createScannedPdf(800, 1000);

        List<NormalizedSampleQuestion> questions = parserService.parsePdfDocument(pdfBytes);

        assertThat(questions).hasSize(1);
        NormalizedSampleQuestion q = questions.getFirst();
        assertThat(q.getParsingTier()).isEqualTo("TIER_2_SCANNED_FALLBACK");
        assertThat(q.isHasDiagram()).isTrue();
        assertThat(q.getEmbeddedMedia()).isNotNull();
    }

    @Test
    @DisplayName("Standalone image optimizes and downsamples image to max 1024px")
    void testStandaloneImageDownsampling() throws IOException {
        BufferedImage largeImage = new BufferedImage(2000, 1500, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(largeImage, "png", baos);

        NormalizedSampleQuestion q = parserService.parseStandaloneImage(baos.toByteArray(), "image/png");

        assertThat(q.getParsingTier()).isEqualTo("STANDALONE_IMAGE");
        assertThat(q.isHasDiagram()).isTrue();
        assertThat(q.getEmbeddedMedia()).isNotNull();

        BufferedImage downsampled = parserService.downsampleImage(largeImage, 1024);
        assertThat(downsampled.getWidth()).isLessThanOrEqualTo(1024);
        assertThat(downsampled.getHeight()).isLessThanOrEqualTo(1024);
    }

    @Test
    @DisplayName("LaTeX delimiters and chemistry notation are correctly preserved and detected")
    void testMathAndChemistryPreservation() {
        String raw = "Solve for $$x$$ in \\(x^2 - 4 = 0\\). Consider reaction \\ce{2H2 + O2 -> 2H2O}.";
        NormalizedSampleQuestion q = parserService.normalizeTextQuestion(raw, "TIER_0_LOCAL_TEXT", null);

        assertThat(q.isHasMathOrChemistry()).isTrue();
        assertThat(q.getStem()).contains("$$x^2 - 4 = 0$$");
        assertThat(q.getStem()).doesNotContain("\\(");
        assertThat(q.getStem()).contains("\\ce{2H2 + O2 -> 2H2O}");
    }

    @Test
    @DisplayName("Multipart file upload routing parses PDF multipart files")
    void testParseMultipartFile() throws IOException {
        String content = "Identify the state function: A) Heat B) Work C) Enthalpy D) Path";
        byte[] pdfBytes = createCleanTextPdf(content);
        MockMultipartFile file = new MockMultipartFile("file", "sample.pdf", "application/pdf", pdfBytes);

        List<NormalizedSampleQuestion> questions = parserService.parseUploadedFile(file);

        assertThat(questions).hasSize(1);
        assertThat(questions.getFirst().getParsingTier()).isEqualTo("TIER_0_LOCAL_TEXT");
    }
}
