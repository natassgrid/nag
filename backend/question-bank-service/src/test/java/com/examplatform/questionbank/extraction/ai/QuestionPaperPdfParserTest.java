/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 Open Digital Public Infrastructure (DPI) Platform Contributors
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
package com.examplatform.questionbank.extraction.ai;

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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("QuestionPaperPdfParser Unit Tests")
class QuestionPaperPdfParserTest {

    private QuestionPaperPdfParser parser;

    @BeforeEach
    void setUp() {
        parser = new QuestionPaperPdfParser();
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
            g.setColor(Color.RED);
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

    private byte[] createScannedPdf() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            // No text in stream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    @DisplayName("getPageCount returns correct page count")
    void testGetPageCount() throws IOException {
        byte[] pdfBytes = createCleanTextPdf("A short question text to test page count.");
        int count = parser.getPageCount(pdfBytes);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Tier 0: Pure digital text with length >= 40 chars and no diagrams")
    void testTier0LocalText() throws IOException {
        String longText = "1. An ideal gas undergoes isothermal expansion from volume V1 to V2. Calculate the work done.";
        byte[] pdfBytes = createCleanTextPdf(longText);

        PdfPageContent pageContent = parser.extractSinglePage(pdfBytes, 1);

        assertThat(pageContent).isNotNull();
        assertThat(pageContent.getPageNumber()).isEqualTo(1);
        assertThat(pageContent.getTotalPages()).isEqualTo(1);
        assertThat(pageContent.getParsingTier()).isEqualTo(PdfPageContent.TIER_0_LOCAL_TEXT);
        assertThat(pageContent.isScannedOrImageHeavy()).isFalse();
        assertThat(pageContent.getRenderedPageDataUri()).isNull();
        assertThat(pageContent.getEmbeddedDiagramDataUris()).isEmpty();
        assertThat(pageContent.getDigitalText()).contains("isothermal expansion");
    }

    @Test
    @DisplayName("Tier 1: Digital text with embedded raster diagram >= 40x40")
    void testTier1SelectiveVision() throws IOException {
        String text = "2. Consider the circuit diagram below. Find the equivalent resistance between A and B.";
        byte[] pdfBytes = createPdfWithEmbeddedImage(text, 120, 120);

        PdfPageContent pageContent = parser.extractSinglePage(pdfBytes, 1);

        assertThat(pageContent).isNotNull();
        assertThat(pageContent.getParsingTier()).isEqualTo(PdfPageContent.TIER_1_SELECTIVE_VISION);
        assertThat(pageContent.isScannedOrImageHeavy()).isFalse();
        assertThat(pageContent.getEmbeddedDiagramDataUris()).hasSize(1);
        assertThat(pageContent.getEmbeddedDiagramDataUris().getFirst()).startsWith("data:image/jpeg;base64,");
    }

    @Test
    @DisplayName("Tier 2: Empty/sparse text renders page at 150 DPI")
    void testTier2ScannedFallback() throws IOException {
        byte[] pdfBytes = createScannedPdf();

        PdfPageContent pageContent = parser.extractSinglePage(pdfBytes, 1);

        assertThat(pageContent).isNotNull();
        assertThat(pageContent.getParsingTier()).isEqualTo(PdfPageContent.TIER_2_SCANNED_FALLBACK);
        assertThat(pageContent.isScannedOrImageHeavy()).isTrue();
        assertThat(pageContent.getRenderedPageDataUri()).isNotNull();
        assertThat(pageContent.getRenderedPageDataUri()).startsWith("data:image/jpeg;base64,");
    }

    @Test
    @DisplayName("normalizeMathAndFormatting standardizes all delimiters to $$...$$")
    void testNormalizeMathAndFormatting() {
        String input = "Calculate \\(E = mc^2\\) and \\[\\int x dx\\] where $F = ma$ and \\ce{H2SO4} reacts.";
        String normalized = parser.normalizeMathAndFormatting(input);

        assertThat(normalized).contains("$$E = mc^2$$");
        assertThat(normalized).contains("$$\\int x dx$$");
        assertThat(normalized).contains("$$F = ma$$");
        assertThat(normalized).contains("$$\\ce{H2SO4}$$");
        assertThat(normalized).doesNotContain("\\(");
        assertThat(normalized).doesNotContain("\\[");
    }

    @Test
    @DisplayName("Option B (local-ocr): Scanned page is promoted to Tier 0 when OCR produces >= 40 characters")
    void testLocalOcrPromotionToTier0() throws IOException {
        OcrServiceClient mockOcr = mock(OcrServiceClient.class);
        when(mockOcr.isAvailable()).thenReturn(true);
        when(mockOcr.extractTextFromImage(anyString(), eq("eng+hin")))
                .thenReturn("Q.1 Which of the following is a fundamental SI unit? (A) Newton (B) Joule (C) Kelvin (D) Watt");

        QuestionPaperPdfParser ocrParser = new QuestionPaperPdfParser(mockOcr);
        ocrParser.setVisionStrategy("local-ocr");

        byte[] pdfBytes = createScannedPdf();
        PdfPageContent pageContent = ocrParser.extractSinglePage(pdfBytes, 1);

        assertThat(pageContent).isNotNull();
        assertThat(pageContent.getParsingTier()).isEqualTo(PdfPageContent.TIER_0_LOCAL_TEXT);
        assertThat(pageContent.isScannedOrImageHeavy()).isFalse();
        assertThat(pageContent.getDigitalText()).contains("fundamental SI unit");
    }

    @Test
    @DisplayName("Option A (cloud-vision): Scanned page remains Tier 2 when strategy is cloud-vision")
    void testCloudVisionStrategyPreservesTier2() throws IOException {
        OcrServiceClient mockOcr = mock(OcrServiceClient.class);
        when(mockOcr.isAvailable()).thenReturn(true);

        QuestionPaperPdfParser cloudParser = new QuestionPaperPdfParser(mockOcr);
        cloudParser.setVisionStrategy("cloud-vision");

        byte[] pdfBytes = createScannedPdf();
        PdfPageContent pageContent = cloudParser.extractSinglePage(pdfBytes, 1);

        assertThat(pageContent).isNotNull();
        assertThat(pageContent.getParsingTier()).isEqualTo(PdfPageContent.TIER_2_SCANNED_FALLBACK);
        assertThat(pageContent.isScannedOrImageHeavy()).isTrue();
        assertThat(pageContent.getRenderedPageDataUri()).isNotNull();
    }
}
