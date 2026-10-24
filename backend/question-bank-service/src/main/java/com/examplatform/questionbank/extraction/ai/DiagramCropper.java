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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Crops diagram and figure regions detected on scanned examination pages.
 * Validates normalized bounding boxes [x0, y0, x1, y1], ignores degenerate or full-page boxes,
 * applies small boundary padding, downsamples oversized crops, and encodes to JPEG data URIs.
 */
@Slf4j
@Component
public class DiagramCropper {

    public static final int MAX_DIAGRAM_DIMENSION = 1024;
    public static final double PADDING_RATIO = 0.02;
    public static final double MIN_BOX_DIMENSION = 0.01;
    public static final double MAX_BOX_AREA = 0.90;

    /**
     * Crops one or more diagram bounding boxes from a base64-encoded rendered page data URI.
     *
     * @param renderedPageDataUri data URI of the rendered PDF page (e.g. "data:image/jpeg;base64,...")
     * @param boundingBoxes list of normalized [x0, y0, x1, y1] coordinates in range 0.0 - 1.0
     * @return list of base64 JPEG data URIs for valid cropped regions
     */
    public List<String> cropDiagrams(String renderedPageDataUri, List<List<Double>> boundingBoxes) {
        if (renderedPageDataUri == null || renderedPageDataUri.isBlank() || boundingBoxes == null || boundingBoxes.isEmpty()) {
            return List.of();
        }

        BufferedImage pageImage = decodeDataUri(renderedPageDataUri);
        if (pageImage == null) {
            log.warn("Unable to decode rendered page data URI for diagram cropping");
            return List.of();
        }

        return cropDiagrams(pageImage, boundingBoxes);
    }

    /**
     * Crops one or more diagram bounding boxes from a BufferedImage.
     *
     * @param pageImage source image of the rendered page
     * @param boundingBoxes list of normalized [x0, y0, x1, y1] coordinates in range 0.0 - 1.0
     * @return list of base64 JPEG data URIs for valid cropped regions
     */
    public List<String> cropDiagrams(BufferedImage pageImage, List<List<Double>> boundingBoxes) {
        if (pageImage == null || boundingBoxes == null || boundingBoxes.isEmpty()) {
            return List.of();
        }

        int imgW = pageImage.getWidth();
        int imgH = pageImage.getHeight();
        if (imgW <= 0 || imgH <= 0) {
            return List.of();
        }

        List<String> results = new ArrayList<>();
        for (List<Double> box : boundingBoxes) {
            if (box == null || box.size() < 4) {
                continue;
            }

            Double rawX0 = box.get(0);
            Double rawY0 = box.get(1);
            Double rawX1 = box.get(2);
            Double rawY1 = box.get(3);
            if (rawX0 == null || rawY0 == null || rawX1 == null || rawY1 == null) {
                continue;
            }

            double x0 = Math.min(rawX0, rawX1);
            double x1 = Math.max(rawX0, rawX1);
            double y0 = Math.min(rawY0, rawY1);
            double y1 = Math.max(rawY0, rawY1);

            // Clamp coordinates to [0.0, 1.0]
            x0 = Math.max(0.0, Math.min(1.0, x0));
            x1 = Math.max(0.0, Math.min(1.0, x1));
            y0 = Math.max(0.0, Math.min(1.0, y0));
            y1 = Math.max(0.0, Math.min(1.0, y1));

            double width = x1 - x0;
            double height = y1 - y0;

            // Reject degenerate boxes
            if (width < MIN_BOX_DIMENSION || height < MIN_BOX_DIMENSION) {
                log.debug("Skipping degenerate diagram box: width={}, height={}", width, height);
                continue;
            }

            // Reject boxes covering >90% of page
            double area = width * height;
            if (area > MAX_BOX_AREA || (width > 0.95 && height > 0.95)) {
                log.debug("Skipping oversized diagram box (>90% page area): area={}", area);
                continue;
            }

            // Apply padding
            double paddedX0 = Math.max(0.0, x0 - PADDING_RATIO);
            double paddedY0 = Math.max(0.0, y0 - PADDING_RATIO);
            double paddedX1 = Math.min(1.0, x1 + PADDING_RATIO);
            double paddedY1 = Math.min(1.0, y1 + PADDING_RATIO);

            int px = (int) Math.round(paddedX0 * imgW);
            int py = (int) Math.round(paddedY0 * imgH);
            int pw = (int) Math.round((paddedX1 - paddedX0) * imgW);
            int ph = (int) Math.round((paddedY1 - paddedY0) * imgH);

            px = Math.max(0, Math.min(imgW - 1, px));
            py = Math.max(0, Math.min(imgH - 1, py));
            pw = Math.max(1, Math.min(imgW - px, pw));
            ph = Math.max(1, Math.min(imgH - py, ph));

            try {
                BufferedImage cropped = pageImage.getSubimage(px, py, pw, ph);
                BufferedImage standalone = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = standalone.createGraphics();
                g.drawImage(cropped, 0, 0, null);
                g.dispose();

                BufferedImage downsampled = downsampleImage(standalone, MAX_DIAGRAM_DIMENSION);
                String dataUri = encodeToDataUri(downsampled);
                if (dataUri != null) {
                    results.add(dataUri);
                }
            } catch (Exception e) {
                log.warn("Failed to crop diagram box [{}, {}, {}, {}]: {}", px, py, pw, ph, e.getMessage());
            }
        }

        return results;
    }

    /**
     * Decodes a base64 image data URI into a {@link BufferedImage}.
     */
    public BufferedImage decodeDataUri(String dataUri) {
        if (dataUri == null || !dataUri.contains(",")) {
            return null;
        }
        try {
            String base64Part = dataUri.substring(dataUri.indexOf(',') + 1).trim();
            byte[] bytes = Base64.getDecoder().decode(base64Part);
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            log.warn("Failed to decode image data URI: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Encodes a {@link BufferedImage} to a JPEG data URI.
     */
    public String encodeToDataUri(BufferedImage image) {
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

    /**
     * Downsamples an image preserving aspect ratio if either dimension exceeds maxDimension.
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
}
