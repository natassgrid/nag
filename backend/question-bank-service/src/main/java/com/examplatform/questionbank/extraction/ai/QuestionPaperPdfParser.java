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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent PDF Parser for Question Papers using Apache PDFBox 3.0.8.
 * Implements a 3-tier extraction ladder:
 * <ul>
 *   <li><b>Tier 0 (Local Text):</b> Clean digital text without raster diagrams (zero vision token spend).</li>
 *   <li><b>Tier 1 (Selective Vision):</b> Digital text accompanied by isolated diagram crops (&gt;= 40x40).</li>
 *   <li><b>Tier 2 (Scanned Fallback):</b> Sparse digital text (&lt; 40 characters) rendered at 150 DPI.</li>
 * </ul>
 */
@Slf4j
@Service
public class QuestionPaperPdfParser {

    public static final int MAX_DIAGRAM_DIMENSION = 1024;
    public static final int MAX_RENDERED_PAGE_DIMENSION = 1800;
    public static final int SCANNED_RENDER_DPI = 200;
    public static final int MIN_TEXT_LENGTH_THRESHOLD = 40;
    public static final int MIN_IMAGE_DIMENSION = 40;
    public static final int FULL_PAGE_IMAGE_MIN_WIDTH = 500;
    public static final int FULL_PAGE_IMAGE_MIN_HEIGHT = 700;

    private final OcrServiceClient ocrServiceClient;

    @Getter
    @Setter
    @Value("${app.extraction.vision-strategy:local-ocr}")
    private String visionStrategy = "local-ocr";

    public QuestionPaperPdfParser() {
        this.ocrServiceClient = null;
    }

    @Autowired
    public QuestionPaperPdfParser(@Autowired(required = false) OcrServiceClient ocrServiceClient) {
        this.ocrServiceClient = ocrServiceClient;
    }

    private static final Pattern PAREN_DELIMITER_PATTERN =
            Pattern.compile(Pattern.quote("\\(") + "(.*?)" + Pattern.quote("\\)"), Pattern.DOTALL);

    private static final Pattern BRACKET_DELIMITER_PATTERN =
            Pattern.compile(Pattern.quote("\\[") + "(.*?)" + Pattern.quote("\\]"), Pattern.DOTALL);

    private static final Pattern SINGLE_DOLLAR_MATH_PATTERN =
            Pattern.compile("(?<!\\$)\\$(?!\\$)([^$\\r\\n]*?[a-zA-Z\\\\_\\^\\+\\-\\=\\/\\*\\(\\)\\{\\}][^$\\r\\n]*?)(?<!\\$)\\$(?!\\$)");

    private static final Pattern CE_UNWRAPPED_PATTERN =
            Pattern.compile("(?<!\\$\\$)\\\\ce\\{([^}]+)\\}(?!\\$\\$)");

    /**
     * Inspects a PDF byte buffer and returns the total page count.
     *
     * @param pdfBytes binary content of the PDF file
     * @return number of pages
     * @throws IOException if the PDF cannot be loaded or is corrupted
     */
    public int getPageCount(byte[] pdfBytes) throws IOException {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("PDF byte array cannot be null or empty");
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return document.getNumberOfPages();
        }
    }

    /**
     * Extracts a single page from a PDF document by its 1-based page number.
     *
     * @param document the loaded PDFBox document
     * @param oneBasedPageNumber 1-based page index
     * @return the structured {@link PdfPageContent}
     * @throws IOException if rendering or stream extraction fails
     */
    public PdfPageContent extractSinglePage(PDDocument document, int oneBasedPageNumber) throws IOException {
        int totalPages = document.getNumberOfPages();
        if (oneBasedPageNumber < 1 || oneBasedPageNumber > totalPages) {
            throw new IllegalArgumentException("Invalid page number: " + oneBasedPageNumber + ". Total pages: " + totalPages);
        }

        // Step 1: Extract digital text for just this page
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(oneBasedPageNumber);
        stripper.setEndPage(oneBasedPageNumber);
        String rawText = stripper.getText(document);
        String cleanText = rawText != null ? rawText.trim() : "";
        String normalizedText = normalizeMathAndFormatting(cleanText);

        // Step 2: Extract embedded raster images (PDImageXObject) from this page
        List<String> embeddedDiagramDataUris = new ArrayList<>();
        boolean hasFullPageScanImage = false;
        PDPage page = document.getPage(oneBasedPageNumber - 1);
        PDResources resources = page.getResources();
        if (resources != null) {
            for (COSName name : resources.getXObjectNames()) {
                try {
                    if (resources.isImageXObject(name)) {
                        PDImageXObject imageObject = (PDImageXObject) resources.getXObject(name);
                        BufferedImage image = imageObject.getImage();
                        if (image != null) {
                            if (image.getWidth() >= FULL_PAGE_IMAGE_MIN_WIDTH && image.getHeight() >= FULL_PAGE_IMAGE_MIN_HEIGHT) {
                                hasFullPageScanImage = true;
                            } else if (image.getWidth() >= MIN_IMAGE_DIMENSION && image.getHeight() >= MIN_IMAGE_DIMENSION) {
                                BufferedImage downsampled = downsampleImage(image, MAX_DIAGRAM_DIMENSION);
                                String dataUri = encodeImageToDataUri(downsampled);
                                if (dataUri != null) {
                                    embeddedDiagramDataUris.add(dataUri);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to extract embedded image '{}' on page {}: {}", name.getName(), oneBasedPageNumber, e.getMessage());
                }
            }
        }

        // Step 3: Classify tier and render page if scanned or text is sparse
        String tier;
        boolean scannedOrImageHeavy = false;
        String renderedPageDataUri = null;

        if (cleanText.length() < MIN_TEXT_LENGTH_THRESHOLD || hasFullPageScanImage) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage renderedPage = renderer.renderImageWithDPI(oneBasedPageNumber - 1, SCANNED_RENDER_DPI);
            BufferedImage downsampled = downsampleImage(renderedPage, MAX_RENDERED_PAGE_DIMENSION);
            renderedPageDataUri = encodeImageToDataUri(downsampled);

            // Option B (Default: local-ocr): Perform fast local Tesseract OCR if text is missing or page is scanned
            if ("local-ocr".equalsIgnoreCase(visionStrategy) && ocrServiceClient != null && ocrServiceClient.isAvailable()) {
                log.info("Page {} has sparse text or full page scan; invoking local Tesseract OCR (eng+hin)...", oneBasedPageNumber);
                String ocrText = ocrServiceClient.extractTextFromImage(renderedPageDataUri, "eng+hin");
                if (ocrText != null && !ocrText.isBlank()) {
                    cleanText = ocrText.trim();
                    normalizedText = normalizeMathAndFormatting(cleanText);
                    log.info("Local OCR recognized {} characters on page {}", cleanText.length(), oneBasedPageNumber);
                }
            }

            if (cleanText.length() >= MIN_TEXT_LENGTH_THRESHOLD) {
                // If OCR or digital stream produced sufficient text, classify as text tier
                tier = embeddedDiagramDataUris.isEmpty() ? PdfPageContent.TIER_0_LOCAL_TEXT : PdfPageContent.TIER_1_SELECTIVE_VISION;
                scannedOrImageHeavy = false;
                log.info("Page {} promoted to {} via text extraction (length={})", oneBasedPageNumber, tier, cleanText.length());
            } else {
                // Tier 2: Scanned fallback (for cloud vision Option A: Gemini Flash or local VL)
                tier = PdfPageContent.TIER_2_SCANNED_FALLBACK;
                scannedOrImageHeavy = true;
                log.info("Page {} classified as {} (cleanText length={}, hasFullPageScanImage={})",
                        oneBasedPageNumber, tier, cleanText.length(), hasFullPageScanImage);
            }
        } else if (!embeddedDiagramDataUris.isEmpty()) {
            // Tier 1: Digital text with embedded diagrams
            tier = PdfPageContent.TIER_1_SELECTIVE_VISION;
            log.info("Page {} classified as {} ({} embedded diagram(s))", oneBasedPageNumber, tier, embeddedDiagramDataUris.size());
        } else {
            // Tier 0: Pure digital text
            tier = PdfPageContent.TIER_0_LOCAL_TEXT;
            log.debug("Page {} classified as {} (pure digital text)", oneBasedPageNumber, tier);
        }

        return PdfPageContent.builder()
                .pageNumber(oneBasedPageNumber)
                .totalPages(totalPages)
                .digitalText(normalizedText)
                .scannedOrImageHeavy(scannedOrImageHeavy)
                .renderedPageDataUri(renderedPageDataUri)
                .embeddedDiagramDataUris(embeddedDiagramDataUris)
                .parsingTier(tier)
                .build();
    }

    /**
     * Overload to extract a single page directly from PDF byte buffer.
     *
     * @param pdfBytes binary content of the PDF file
     * @param oneBasedPageNumber 1-based page index
     * @return structured {@link PdfPageContent}
     * @throws IOException if reading fails
     */
    public PdfPageContent extractSinglePage(byte[] pdfBytes, int oneBasedPageNumber) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return extractSinglePage(document, oneBasedPageNumber);
        }
    }

    /**
     * Normalizes LaTeX delimiters and chemistry notations per AGENTS.md standards:
     * <ul>
     *   <li>Converts {@code \( ... \)} and {@code \[ ... \]} to {@code $$ ... $$}.</li>
     *   <li>Converts single {@code $ ... $} wrapping math tokens to {@code $$ ... $$}.</li>
     *   <li>Wraps bare {@code \ce{...}} formulas in {@code $$ ... $$}.</li>
     * </ul>
     *
     * @param text raw extracted text
     * @return normalized text with standard {@code $$ ... $$} delimiters
     */
    public String normalizeMathAndFormatting(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        // 1. Convert \( ... \) to $$ ... $$
        String res = PAREN_DELIMITER_PATTERN.matcher(text)
                .replaceAll(mr -> Matcher.quoteReplacement("$$" + mr.group(1).trim() + "$$"));

        // 2. Convert \[ ... \] to $$ ... $$
        res = BRACKET_DELIMITER_PATTERN.matcher(res)
                .replaceAll(mr -> Matcher.quoteReplacement("$$" + mr.group(1).trim() + "$$"));

        // 3. Convert single $ ... $ to $$ ... $$
        res = SINGLE_DOLLAR_MATH_PATTERN.matcher(res)
                .replaceAll(mr -> Matcher.quoteReplacement("$$" + mr.group(1).trim() + "$$"));

        // 4. Wrap unwrapped \ce{...} with $$ ... $$
        res = CE_UNWRAPPED_PATTERN.matcher(res)
                .replaceAll(mr -> Matcher.quoteReplacement("$$\\ce{" + mr.group(1).trim() + "}$$"));

        return res;
    }

    /**
     * Downsamples an image preserving aspect ratio if either dimension exceeds the maximum.
     *
     * @param original original image
     * @param maxDimension maximum allowed width or height in pixels
     * @return downsampled image or original if within bounds
     */
    public BufferedImage downsampleImage(BufferedImage original, int maxDimension) {
        if (original == null) {
            return null;
        }
        int width = original.getWidth();
        int height = original.getHeight();

        if (width <= maxDimension && height <= maxDimension) {
            return original;
        }

        double scale = (double) maxDimension / Math.max(width, height);
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage resized = new BufferedImage(
                targetWidth,
                targetHeight,
                original.getType() == 0 ? BufferedImage.TYPE_INT_RGB : original.getType()
        );

        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return resized;
    }

    /**
     * Encodes a buffered image into a base64 JPEG data URI.
     *
     * @param image image to encode
     * @return data:image/jpeg;base64,... string or null on failure
     */
    public String encodeImageToDataUri(BufferedImage image) {
        if (image == null) {
            return null;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpeg", baos);
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.warn("Failed to encode image to base64 JPEG data URI: {}", e.getMessage());
            return null;
        }
    }
}
