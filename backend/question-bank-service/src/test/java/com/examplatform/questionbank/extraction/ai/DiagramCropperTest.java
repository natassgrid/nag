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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DiagramCropper Unit Tests")
class DiagramCropperTest {

    private DiagramCropper cropper;

    @BeforeEach
    void setUp() {
        cropper = new DiagramCropper();
    }

    private BufferedImage createSyntheticPageImage(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        // Draw a simulated diagram rectangle
        g.setColor(Color.BLUE);
        g.fillRect(50, 50, 100, 100);
        g.dispose();
        return img;
    }

    @Test
    @DisplayName("Crops valid diagram bounding box and returns JPEG data URI")
    void testValidBoundingBoxCropping() {
        BufferedImage pageImage = createSyntheticPageImage(500, 500);
        String pageDataUri = cropper.encodeToDataUri(pageImage);

        // Box around the blue rectangle (50, 50, 150, 150) -> normalized ~ [0.1, 0.1, 0.3, 0.3]
        List<Double> box = List.of(0.10, 0.10, 0.30, 0.30);
        List<String> crops = cropper.cropDiagrams(pageDataUri, List.of(box));

        assertThat(crops).hasSize(1);
        assertThat(crops.getFirst()).startsWith("data:image/jpeg;base64,");

        // Verify that the crop can be decoded
        BufferedImage decoded = cropper.decodeDataUri(crops.getFirst());
        assertThat(decoded).isNotNull();
        assertThat(decoded.getWidth()).isGreaterThan(0);
        assertThat(decoded.getHeight()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Ignores degenerate bounding box (< 1% dimension)")
    void testDegenerateBoxIgnored() {
        BufferedImage pageImage = createSyntheticPageImage(500, 500);

        // Box with width = 0.005 (< 0.01)
        List<Double> degenerateBox = List.of(0.10, 0.10, 0.105, 0.30);
        List<String> crops = cropper.cropDiagrams(pageImage, List.of(degenerateBox));

        assertThat(crops).isEmpty();
    }

    @Test
    @DisplayName("Ignores full-page or >90% page area bounding box")
    void testOversizedPageBoxIgnored() {
        BufferedImage pageImage = createSyntheticPageImage(500, 500);

        // Box covering 95% of width and height -> area ~ 0.9025
        List<Double> fullPageBox = List.of(0.01, 0.01, 0.98, 0.98);
        List<String> crops = cropper.cropDiagrams(pageImage, List.of(fullPageBox));

        assertThat(crops).isEmpty();
    }

    @Test
    @DisplayName("Clamps bounding boxes with out-of-range coordinates")
    void testBoundingBoxClamping() {
        BufferedImage pageImage = createSyntheticPageImage(400, 400);

        // Box with negative x0 and x1 > 1.0
        List<Double> outOfBoundsBox = List.of(-0.10, 0.20, 1.20, 0.50);
        // Normalized width would be 1.0, height 0.3, area 0.3 <= 0.90 -> valid
        List<String> crops = cropper.cropDiagrams(pageImage, List.of(outOfBoundsBox));

        assertThat(crops).hasSize(1);
        assertThat(crops.getFirst()).startsWith("data:image/jpeg;base64,");
    }

    @Test
    @DisplayName("Downsamples oversized diagram crop to MAX_DIAGRAM_DIMENSION")
    void testDownsampleImage() {
        BufferedImage hugeImage = createSyntheticPageImage(2000, 1500);
        BufferedImage downsampled = cropper.downsampleImage(hugeImage, DiagramCropper.MAX_DIAGRAM_DIMENSION);

        assertThat(downsampled).isNotNull();
        assertThat(Math.max(downsampled.getWidth(), downsampled.getHeight()))
                .isLessThanOrEqualTo(DiagramCropper.MAX_DIAGRAM_DIMENSION);
    }
}
