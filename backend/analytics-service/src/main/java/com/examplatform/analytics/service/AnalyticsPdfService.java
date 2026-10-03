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

package com.examplatform.analytics.service;

import com.examplatform.analytics.domain.ExamAnalytics;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for generating structured PDF analytics reports using Apache PDFBox.
 *
 * Includes:
 * <ul>
 *   <li>Platform branding header and exam metadata</li>
 *   <li>Executive metrics card (Registration, Attendance rate, Percentiles)</li>
 *   <li>Score distribution histogram (vector bar chart)</li>
 *   <li>Section and topic averages table</li>
 *   <li>Platform compliance footer</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsPdfService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.of("UTC"));

    private static final Color PRIMARY_COLOR = new Color(26, 82, 118);       // Deep Navy
    private static final Color SECONDARY_COLOR = new Color(41, 128, 185);    // Blue
    private static final Color ACCENT_COLOR = new Color(39, 174, 96);        // Green
    private static final Color LIGHT_BG_COLOR = new Color(245, 247, 250);    // Soft Gray
    private static final Color BORDER_COLOR = new Color(200, 210, 220);      // Border gray
    private static final Color TEXT_DARK = new Color(44, 62, 80);            // Dark Charcoal
    private static final Color TEXT_MUTED = new Color(127, 140, 141);        // Muted gray
    private static final Color TABLE_HEADER_BG = new Color(230, 238, 248);   // Header fill
    private static final Color ROW_ALT_BG = new Color(249, 250, 252);        // Alternating row

    private final ObjectMapper objectMapper;

    /**
     * Generates a complete, structured PDF analytics report for the given exam analytics record.
     *
     * @param analytics the computed exam analytics
     * @return raw PDF byte array
     */
    public byte[] generatePdfReport(ExamAnalytics analytics) {
        log.info("Generating PDF analytics report for exam: {}", analytics.getExamId());

        try (PDDocument document = new PDDocument()) {
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontOblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float leftMargin = 40;
                float rightMargin = 555;
                float contentWidth = rightMargin - leftMargin;
                float y = 800;

                // 1. Header Banner
                y = drawHeader(contentStream, fontBold, fontRegular, leftMargin, y, contentWidth, analytics);

                // 2. Executive Metrics Summary Card
                y = drawMetricsSummary(contentStream, fontBold, fontRegular, leftMargin, y, contentWidth, analytics);

                // 3. Score Distribution Histogram (Bar Chart)
                y = drawScoreDistributionChart(contentStream, fontBold, fontRegular, leftMargin, y, contentWidth, analytics);

                // 4. Section and Topic Averages Table
                y = drawSectionAveragesTable(contentStream, fontBold, fontRegular, leftMargin, y, contentWidth, analytics);

                // 5. Document Footer
                drawFooter(contentStream, fontRegular, fontOblique, leftMargin, rightMargin);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Failed to generate PDF report for exam: {}", analytics.getExamId(), e);
            throw new RuntimeException("Failed to generate PDF analytics report", e);
        }
    }

    private float drawHeader(PDPageContentStream cs, PDType1Font fontBold, PDType1Font fontRegular,
                             float x, float y, float width, ExamAnalytics analytics) throws IOException {
        // Top banner background
        cs.setNonStrokingColor(PRIMARY_COLOR);
        cs.addRect(x, y - 55, width, 55);
        cs.fill();

        // Platform Header Text
        cs.beginText();
        cs.setNonStrokingColor(Color.WHITE);
        cs.setFont(fontBold, 15);
        cs.newLineAtOffset(x + 15, y - 22);
        cs.showText("NATIONAL ASSESSMENT GRID (NAG)");
        cs.endText();

        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.newLineAtOffset(x + 15, y - 42);
        cs.showText("Official Post-Exam Performance & Analytics Report");
        cs.endText();

        y -= 70;

        // Exam Metadata Row
        cs.setNonStrokingColor(LIGHT_BG_COLOR);
        cs.addRect(x, y - 35, width, 35);
        cs.fill();

        cs.setStrokingColor(BORDER_COLOR);
        cs.setLineWidth(0.8f);
        cs.addRect(x, y - 35, width, 35);
        cs.stroke();

        cs.beginText();
        cs.setNonStrokingColor(TEXT_DARK);
        cs.setFont(fontBold, 9);
        cs.newLineAtOffset(x + 10, y - 18);
        cs.showText("Exam ID: ");
        cs.setFont(fontRegular, 9);
        cs.showText(analytics.getExamId() != null ? analytics.getExamId().toString() : "N/A");

        cs.newLineAtOffset(280, 0);
        cs.setFont(fontBold, 9);
        cs.showText("Computed At: ");
        cs.setFont(fontRegular, 9);
        String computedStr = analytics.getComputedAt() != null
                ? DATE_FORMATTER.format(analytics.getComputedAt())
                : "N/A";
        cs.showText(computedStr);
        cs.endText();

        return y - 48;
    }

    private float drawMetricsSummary(PDPageContentStream cs, PDType1Font fontBold, PDType1Font fontRegular,
                                     float x, float y, float width, ExamAnalytics analytics) throws IOException {
        // Section Title
        cs.beginText();
        cs.setNonStrokingColor(PRIMARY_COLOR);
        cs.setFont(fontBold, 12);
        cs.newLineAtOffset(x, y);
        cs.showText("1. Executive Summary & Key Percentiles");
        cs.endText();

        y -= 10;

        // 4 KPI Cards in a row
        float cardWidth = (width - 15) / 4;
        float cardHeight = 48;
        float cardY = y - cardHeight;

        long registered = analytics.getTotalRegistered() != null ? analytics.getTotalRegistered() : 0L;
        long appeared = analytics.getTotalAppeared() != null ? analytics.getTotalAppeared() : 0L;
        double turnoutPct = registered > 0 ? ((double) appeared / registered) * 100.0 : 100.0;
        BigDecimal top10 = analytics.getTop10PercentileThreshold() != null
                ? analytics.getTop10PercentileThreshold() : BigDecimal.ZERO;
        BigDecimal bottom10 = analytics.getBottom10PercentileThreshold() != null
                ? analytics.getBottom10PercentileThreshold() : BigDecimal.ZERO;

        drawKpiCard(cs, fontBold, fontRegular, x, cardY, cardWidth, cardHeight,
                "Total Registered", String.format("%,d", registered), SECONDARY_COLOR);
        drawKpiCard(cs, fontBold, fontRegular, x + cardWidth + 5, cardY, cardWidth, cardHeight,
                "Total Appeared", String.format("%,d (%.1f%%)", appeared, turnoutPct), ACCENT_COLOR);
        drawKpiCard(cs, fontBold, fontRegular, x + (cardWidth + 5) * 2, cardY, cardWidth, cardHeight,
                "Top 10% (P90) Score", String.format("%.2f", top10), PRIMARY_COLOR);
        drawKpiCard(cs, fontBold, fontRegular, x + (cardWidth + 5) * 3, cardY, cardWidth, cardHeight,
                "Bottom 10% (P10) Score", String.format("%.2f", bottom10), TEXT_DARK);

        return cardY - 20;
    }

    private void drawKpiCard(PDPageContentStream cs, PDType1Font fontBold, PDType1Font fontRegular,
                             float cx, float cy, float cw, float ch,
                             String title, String value, Color accentColor) throws IOException {
        cs.setNonStrokingColor(LIGHT_BG_COLOR);
        cs.addRect(cx, cy, cw, ch);
        cs.fill();

        cs.setStrokingColor(BORDER_COLOR);
        cs.setLineWidth(0.8f);
        cs.addRect(cx, cy, cw, ch);
        cs.stroke();

        // Accent top bar
        cs.setNonStrokingColor(accentColor);
        cs.addRect(cx, cy + ch - 3, cw, 3);
        cs.fill();

        // Label
        cs.beginText();
        cs.setNonStrokingColor(TEXT_MUTED);
        cs.setFont(fontRegular, 8);
        cs.newLineAtOffset(cx + 6, cy + ch - 15);
        cs.showText(title);
        cs.endText();

        // Value
        cs.beginText();
        cs.setNonStrokingColor(TEXT_DARK);
        cs.setFont(fontBold, 11);
        cs.newLineAtOffset(cx + 6, cy + 8);
        cs.showText(value);
        cs.endText();
    }

    private float drawScoreDistributionChart(PDPageContentStream cs, PDType1Font fontBold, PDType1Font fontRegular,
                                             float x, float y, float width, ExamAnalytics analytics) throws IOException {
        // Section Title
        cs.beginText();
        cs.setNonStrokingColor(PRIMARY_COLOR);
        cs.setFont(fontBold, 12);
        cs.newLineAtOffset(x, y);
        cs.showText("2. Score Distribution Histogram");
        cs.endText();

        y -= 15;

        Map<String, Number> distribution = parseDistribution(analytics.getScoreDistributionJson());

        float chartX = x + 35;
        float chartY = y - 130;
        float chartWidth = width - 45;
        float chartHeight = 110;

        // Chart background
        cs.setNonStrokingColor(new Color(250, 252, 255));
        cs.addRect(chartX, chartY, chartWidth, chartHeight);
        cs.fill();

        // Find max count for scaling
        long maxCount = 1;
        for (Number val : distribution.values()) {
            if (val != null && val.longValue() > maxCount) {
                maxCount = val.longValue();
            }
        }

        // Draw horizontal grid lines
        cs.setStrokingColor(new Color(225, 230, 235));
        cs.setLineWidth(0.5f);
        int gridSteps = 4;
        for (int i = 0; i <= gridSteps; i++) {
            float gy = chartY + (chartHeight * i / gridSteps);
            cs.moveTo(chartX, gy);
            cs.lineTo(chartX + chartWidth, gy);
            cs.stroke();

            // Grid y-axis label
            long gridVal = (maxCount * i) / gridSteps;
            cs.beginText();
            cs.setNonStrokingColor(TEXT_MUTED);
            cs.setFont(fontRegular, 7);
            cs.newLineAtOffset(chartX - 30, gy - 2);
            cs.showText(String.format("%,d", gridVal));
            cs.endText();
        }

        // Draw Axes
        cs.setStrokingColor(TEXT_DARK);
        cs.setLineWidth(1.0f);
        cs.moveTo(chartX, chartY);
        cs.lineTo(chartX + chartWidth, chartY); // X Axis
        cs.moveTo(chartX, chartY);
        cs.lineTo(chartX, chartY + chartHeight); // Y Axis
        cs.stroke();

        // Draw Bars
        int bucketCount = Math.max(distribution.size(), 1);
        float slotWidth = chartWidth / bucketCount;
        float barWidth = Math.max(slotWidth * 0.65f, 10);
        float barMargin = (slotWidth - barWidth) / 2;

        int index = 0;
        for (Map.Entry<String, Number> entry : distribution.entrySet()) {
            float bx = chartX + (index * slotWidth) + barMargin;
            long val = entry.getValue() != null ? entry.getValue().longValue() : 0L;
            float bh = maxCount > 0 ? ((float) val / maxCount) * (chartHeight - 15) : 0;

            // Bar fill
            cs.setNonStrokingColor(SECONDARY_COLOR);
            cs.addRect(bx, chartY, barWidth, bh);
            cs.fill();

            // Value label above bar
            if (val > 0) {
                cs.beginText();
                cs.setNonStrokingColor(TEXT_DARK);
                cs.setFont(fontBold, 7);
                String valStr = String.format("%,d", val);
                float valOffset = Math.max(bx + (barWidth / 2) - (valStr.length() * 2f), bx);
                cs.newLineAtOffset(valOffset, chartY + bh + 3);
                cs.showText(valStr);
                cs.endText();
            }

            // X-axis Bucket Label
            cs.beginText();
            cs.setNonStrokingColor(TEXT_DARK);
            cs.setFont(fontRegular, 7);
            String label = entry.getKey();
            float labelOffset = Math.max(bx + (barWidth / 2) - (label.length() * 2.2f), bx - 4);
            cs.newLineAtOffset(labelOffset, chartY - 11);
            cs.showText(label);
            cs.endText();

            index++;
        }

        // X-Axis Title
        cs.beginText();
        cs.setNonStrokingColor(TEXT_MUTED);
        cs.setFont(fontRegular, 8);
        cs.newLineAtOffset(chartX + (chartWidth / 2) - 25, chartY - 23);
        cs.showText("Score Interval");
        cs.endText();

        return chartY - 35;
    }

    private float drawSectionAveragesTable(PDPageContentStream cs, PDType1Font fontBold, PDType1Font fontRegular,
                                           float x, float y, float width, ExamAnalytics analytics) throws IOException {
        // Section Title
        cs.beginText();
        cs.setNonStrokingColor(PRIMARY_COLOR);
        cs.setFont(fontBold, 12);
        cs.newLineAtOffset(x, y);
        cs.showText("3. Section & Subject Performance Breakdown");
        cs.endText();

        y -= 12;

        Map<String, Object> sections = parseSections(analytics.getSectionAveragesJson());

        // Table Header
        float col1Width = width * 0.55f;
        float col2Width = width * 0.45f;
        float rowHeight = 20;

        cs.setNonStrokingColor(TABLE_HEADER_BG);
        cs.addRect(x, y - rowHeight, width, rowHeight);
        cs.fill();

        cs.setStrokingColor(BORDER_COLOR);
        cs.setLineWidth(0.8f);
        cs.addRect(x, y - rowHeight, width, rowHeight);
        cs.stroke();

        cs.beginText();
        cs.setNonStrokingColor(PRIMARY_COLOR);
        cs.setFont(fontBold, 9);
        cs.newLineAtOffset(x + 10, y - 14);
        cs.showText("Section / Subject Name");
        cs.newLineAtOffset(col1Width, 0);
        cs.showText("Average Score");
        cs.endText();

        y -= rowHeight;

        if (sections.isEmpty()) {
            cs.setNonStrokingColor(ROW_ALT_BG);
            cs.addRect(x, y - rowHeight, width, rowHeight);
            cs.fill();

            cs.setStrokingColor(BORDER_COLOR);
            cs.addRect(x, y - rowHeight, width, rowHeight);
            cs.stroke();

            cs.beginText();
            cs.setNonStrokingColor(TEXT_MUTED);
            cs.setFont(fontRegular, 9);
            cs.newLineAtOffset(x + 10, y - 14);
            cs.showText("No section breakdown data available.");
            cs.endText();
            y -= rowHeight;
        } else {
            boolean alt = false;
            for (Map.Entry<String, Object> entry : sections.entrySet()) {
                if (alt) {
                    cs.setNonStrokingColor(ROW_ALT_BG);
                    cs.addRect(x, y - rowHeight, width, rowHeight);
                    cs.fill();
                }

                cs.setStrokingColor(BORDER_COLOR);
                cs.setLineWidth(0.5f);
                cs.addRect(x, y - rowHeight, width, rowHeight);
                cs.stroke();

                cs.beginText();
                cs.setNonStrokingColor(TEXT_DARK);
                cs.setFont(fontRegular, 9);
                cs.newLineAtOffset(x + 10, y - 14);
                cs.showText(entry.getKey());

                cs.newLineAtOffset(col1Width, 0);
                cs.setFont(fontBold, 9);
                String scoreStr = formatScore(entry.getValue());
                cs.showText(scoreStr);
                cs.endText();

                y -= rowHeight;
                alt = !alt;
            }
        }

        return y - 20;
    }

    private void drawFooter(PDPageContentStream cs, PDType1Font fontRegular, PDType1Font fontOblique,
                            float x, float right) throws IOException {
        float y = 30;

        cs.setStrokingColor(BORDER_COLOR);
        cs.setLineWidth(0.5f);
        cs.moveTo(x, y + 10);
        cs.lineTo(right, y + 10);
        cs.stroke();

        cs.beginText();
        cs.setNonStrokingColor(TEXT_MUTED);
        cs.setFont(fontRegular, 8);
        cs.newLineAtOffset(x, y);
        cs.showText("Generated by National Assessment Grid (NAG) Analytics Platform \u2014 AGPL-3.0 DPI");
        cs.endText();

        cs.beginText();
        cs.setFont(fontOblique, 8);
        cs.newLineAtOffset(right - 100, y);
        cs.showText("Confidential Report");
        cs.endText();
    }

    private Map<String, Number> parseDistribution(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Number>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse score distribution JSON: {}", json);
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> parseSections(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse section averages JSON: {}", json);
            return Collections.emptyMap();
        }
    }

    private String formatScore(Object scoreObj) {
        if (scoreObj == null) return "0.00";
        if (scoreObj instanceof Number n) {
            return String.format("%.2f", n.doubleValue());
        }
        try {
            double d = Double.parseDouble(scoreObj.toString());
            return String.format("%.2f", d);
        } catch (Exception e) {
            return scoreObj.toString();
        }
    }
}
