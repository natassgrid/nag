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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Data container representing a single parsed page from an uploaded Question Paper PDF.
 * Captures raw extracted digital text, visual image metadata, and the assigned parsing tier
 * in the tiered extraction ladder.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfPageContent {

    public static final String TIER_0_LOCAL_TEXT = "TIER_0_LOCAL_TEXT";
    public static final String TIER_1_SELECTIVE_VISION = "TIER_1_SELECTIVE_VISION";
    public static final String TIER_2_SCANNED_FALLBACK = "TIER_2_SCANNED_FALLBACK";

    /** 1-based page number within the PDF document. */
    private int pageNumber;

    /** Total number of pages in the parent PDF document. */
    private int totalPages;

    /** Digital text stream extracted for this single page. */
    private String digitalText;

    /** Flag indicating if the page has sparse digital text or is predominantly scanned/diagrammatic. */
    private boolean scannedOrImageHeavy;

    /** Base64 JPEG data URI of the page rendered at 150 DPI when classified as scanned or diagram-heavy. */
    private String renderedPageDataUri;

    /** Extracted raster diagrams (PDImageXObject) >= 40x40 downsampled to max 1024px. */
    @Builder.Default
    private List<String> embeddedDiagramDataUris = new ArrayList<>();

    /** Tier classification: TIER_0_LOCAL_TEXT, TIER_1_SELECTIVE_VISION, or TIER_2_SCANNED_FALLBACK. */
    private String parsingTier;
}
