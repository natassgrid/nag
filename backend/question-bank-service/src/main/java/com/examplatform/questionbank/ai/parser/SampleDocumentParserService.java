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

import com.examplatform.questionbank.dto.QuestionOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent multimodal parser implementing a tiered extraction ladder
 * to minimize vision token usage and cloud API costs.
 *
 * <ul>
 *   <li><b>Tier 0 (Local Extraction):</b> Extracts digital text stream via PDFBox.
 *       If text is present and no diagram is detected, completely bypasses vision models
 *       incurring $0.00 vision token spend.</li>
 *   <li><b>Tier 1 (Selective Vision):</b> Directly isolates raster images (PDImageXObject)
 *       from PDF streams, downsampling only the cropped diagram (max 1024px) rather than
 *       rasterizing whole pages at high DPI.</li>
 *   <li><b>Tier 2 (Scanned Fallback):</b> When text stream is empty/scanned, renders
 *       at 150 DPI (saving 75% image footprint compared to 300 DPI).</li>
 *   <li><b>Standalone Images:</b> Downsamples PNG/JPEG/WEBP before payload preparation.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SampleDocumentParserService {

    public static final int MAX_IMAGE_DIMENSION = 1024;
    public static final int SCANNED_RENDER_DPI = 150;

    private static final Pattern LATEX_MATH_PATTERN = Pattern.compile(
            "(\\$[\\s\\S]*?\\$|\\\\\\[[\\s\\S]*?\\\\\\]|\\\\\\([\\s\\S]*?\\\\\\)|\\\\frac|\\\\sqrt|\\\\int|\\\\sum|\\\\alpha|\\\\beta|\\\\theta|\\\\partial)");

    private static final Pattern CHEMISTRY_PATTERN = Pattern.compile(
            "(\\\\ce\\{[^}]*\\}|\\b(H2O|CO2|NaCl|H2SO4|HCl|NaOH|CH4|C2H6|C6H12O6|KMnO4|Fe2O3)\\b|\\b(->|→|⇌|\\\\longrightarrow)\\b)");

    private static final Pattern OPTION_PATTERN = Pattern.compile(
            "(?m)^[\\s]*[\\(\\[]?([A-Da-d])[\\)\\.\\]][\\s]+(.*)$");

    private static final Pattern PAREN_DELIMITER_PATTERN =
            Pattern.compile(Pattern.quote("\\(") + "(.*?)" + Pattern.quote("\\)"), Pattern.DOTALL);

    private static final Pattern BRACKET_DELIMITER_PATTERN =
            Pattern.compile(Pattern.quote("\\[") + "(.*?)" + Pattern.quote("\\]"), Pattern.DOTALL);

    /**
     * Parses an uploaded multipart file (PDF or Image).
     */
    public List<NormalizedSampleQuestion> parseUploadedFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return List.of();
        }
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        byte[] bytes = file.getBytes();

        if ("application/pdf".equalsIgnoreCase(contentType) || filename.endsWith(".pdf")) {
            return parsePdfDocument(bytes);
        } else if (contentType != null && contentType.startsWith("image/")
                || filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".webp")) {
            return List.of(parseStandaloneImage(bytes, contentType));
        } else {
            // Treat as plain text
            String text = new String(bytes);
            return List.of(normalizeTextQuestion(text, "TIER_0_LOCAL_TEXT", null));
        }
    }

    /**
     * Parses a PDF document using the tiered extraction ladder.
     */
    public List<NormalizedSampleQuestion> parsePdfDocument(byte[] pdfBytes) throws IOException {
        List<NormalizedSampleQuestion> questions = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(document);
            String cleanText = rawText != null ? rawText.trim() : "";

            // Collect embedded images across pages (Tier 1)
            List<BufferedImage> embeddedImages = extractEmbeddedImages(document);

            if (!cleanText.isEmpty() && cleanText.length() >= 25) {
                if (embeddedImages.isEmpty()) {
                    // Tier 0: Pure Text PDF — Zero vision tokens!
                    log.info("Tier 0 Local Extraction: Pure text PDF (length={}), zero vision tokens", cleanText.length());
                    questions.add(normalizeTextQuestion(cleanText, "TIER_0_LOCAL_TEXT", null));
                } else {
                    // Tier 1: Text + Embedded Images
                    log.info("Tier 1 Selective Vision: PDF has text and {} embedded diagram(s)", embeddedImages.size());
                    BufferedImage primaryImage = embeddedImages.getFirst();
                    BufferedImage downsampled = downsampleImage(primaryImage, MAX_IMAGE_DIMENSION);
                    String mediaRef = encodeImageToDataUri(downsampled);

                    NormalizedSampleQuestion nq = normalizeTextQuestion(cleanText, "TIER_1_SELECTIVE_VISION", mediaRef);
                    nq.setHasDiagram(true);
                    questions.add(nq);
                }
            } else {
                // Tier 2: Scanned / Image-Only PDF Fallback — Render at 150 DPI
                log.info("Tier 2 Scanned OCR Fallback: Digital text stream empty, rendering at {} DPI", SCANNED_RENDER_DPI);
                PDFRenderer renderer = new PDFRenderer(document);
                if (document.getNumberOfPages() > 0) {
                    BufferedImage renderedPage = renderer.renderImageWithDPI(0, SCANNED_RENDER_DPI);
                    BufferedImage downsampled = downsampleImage(renderedPage, MAX_IMAGE_DIMENSION);
                    String mediaRef = encodeImageToDataUri(downsampled);

                    NormalizedSampleQuestion nq = NormalizedSampleQuestion.builder()
                            .stem("Scanned Document Sample Question")
                            .embeddedMedia(mediaRef)
                            .parsingTier("TIER_2_SCANNED_FALLBACK")
                            .hasDiagram(true)
                            .questionType("SINGLE_MCQ")
                            .difficulty("MEDIUM")
                            .bloomLevel("APPLY")
                            .build();
                    questions.add(nq);
                }
            }
        }

        return questions;
    }

    /**
     * Parses a standalone image file (PNG, JPEG, WEBP).
     */
    public NormalizedSampleQuestion parseStandaloneImage(byte[] imageBytes, String contentType) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (original == null) {
            throw new IllegalArgumentException("Unable to decode standalone image format");
        }
        BufferedImage downsampled = downsampleImage(original, MAX_IMAGE_DIMENSION);
        String mediaRef = encodeImageToDataUri(downsampled);

        return NormalizedSampleQuestion.builder()
                .stem("Sample Question with Diagram Reference")
                .embeddedMedia(mediaRef)
                .parsingTier("STANDALONE_IMAGE")
                .hasDiagram(true)
                .questionType("SINGLE_MCQ")
                .difficulty("MEDIUM")
                .bloomLevel("APPLY")
                .build();
    }

    /**
     * Extracts embedded raster images (PDImageXObject) directly from PDF streams.
     */
    public List<BufferedImage> extractEmbeddedImages(PDDocument document) {
        List<BufferedImage> images = new ArrayList<>();
        for (PDPage page : document.getPages()) {
            PDResources resources = page.getResources();
            if (resources == null) continue;

            for (COSName name : resources.getXObjectNames()) {
                try {
                    if (resources.isImageXObject(name)) {
                        PDImageXObject imageObject = (PDImageXObject) resources.getXObject(name);
                        BufferedImage image = imageObject.getImage();
                        // Ignore tiny icons / bullet images (< 40x40)
                        if (image != null && image.getWidth() >= 40 && image.getHeight() >= 40) {
                            images.add(image);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to extract embedded image: {}", e.getMessage());
                }
            }
        }
        return images;
    }

    /**
     * Downsamples image to maximum dimension (preserving aspect ratio) to cap token usage.
     */
    public BufferedImage downsampleImage(BufferedImage original, int maxDimension) {
        int width = original.getWidth();
        int height = original.getHeight();

        if (width <= maxDimension && height <= maxDimension) {
            return original;
        }

        double scale = (double) maxDimension / Math.max(width, height);
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight,
                original.getType() == 0 ? BufferedImage.TYPE_INT_RGB : original.getType());
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return resized;
    }

    /**
     * Normalizes raw text question into {@link NormalizedSampleQuestion}.
     * Preserves KaTeX/LaTeX ($$...$$) and chemistry (\ce{...}) syntax.
     */
    public NormalizedSampleQuestion normalizeTextQuestion(String rawText, String tier, String mediaRef) {
        if (rawText == null || rawText.isBlank()) {
            return NormalizedSampleQuestion.builder()
                    .stem("")
                    .parsingTier(tier)
                    .build();
        }

        // Standardize LaTeX delimiters: convert \( ... \) and \[ ... \] to $$...$$
        String normalizedText = normalizeLatexDelimiters(rawText.trim());

        boolean hasMath = LATEX_MATH_PATTERN.matcher(normalizedText).find();
        boolean hasChemistry = CHEMISTRY_PATTERN.matcher(normalizedText).find();

        // Extract options if present
        List<QuestionOption> options = extractOptions(normalizedText);

        // Infer question type
        String questionType = options.size() >= 2 ? "SINGLE_MCQ" : "DESCRIPTIVE";

        // Separate stem from options
        String stem = cleanStemText(normalizedText, options);

        return NormalizedSampleQuestion.builder()
                .stem(stem)
                .options(options)
                .embeddedMedia(mediaRef)
                .hasMathOrChemistry(hasMath || hasChemistry)
                .hasDiagram(mediaRef != null && !mediaRef.isBlank())
                .parsingTier(tier)
                .questionType(questionType)
                .difficulty("MEDIUM")
                .bloomLevel(hasMath || hasChemistry ? "ANALYZE" : "APPLY")
                .build();
    }

    /**
     * Normalizes LaTeX delimiters so KaTeX renders uniformly.
     */
    public String normalizeLatexDelimiters(String text) {
        if (text == null) return null;
        String res = PAREN_DELIMITER_PATTERN.matcher(text)
                .replaceAll(mr -> Matcher.quoteReplacement("$$" + mr.group(1) + "$$"));
        return BRACKET_DELIMITER_PATTERN.matcher(res)
                .replaceAll(mr -> Matcher.quoteReplacement("$$" + mr.group(1) + "$$"));
    }

    private List<QuestionOption> extractOptions(String text) {
        List<QuestionOption> options = new ArrayList<>();
        Matcher matcher = OPTION_PATTERN.matcher(text);
        while (matcher.find()) {
            String id = matcher.group(1).toUpperCase();
            String optText = matcher.group(2).trim();
            options.add(QuestionOption.builder()
                    .id(id)
                    .text(optText)
                    .correct(false)
                    .build());
        }
        return options;
    }

    private String cleanStemText(String text, List<QuestionOption> options) {
        if (options.isEmpty()) {
            return text;
        }
        // If options matched, stem is everything before the first option match
        Matcher matcher = OPTION_PATTERN.matcher(text);
        if (matcher.find()) {
            return text.substring(0, matcher.start()).trim();
        }
        return text;
    }

    private String encodeImageToDataUri(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpeg", baos);
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("Failed to encode image to data URI: {}", e.getMessage());
            return null;
        }
    }
}
