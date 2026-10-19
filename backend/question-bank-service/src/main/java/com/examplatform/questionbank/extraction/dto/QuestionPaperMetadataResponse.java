/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

package com.examplatform.questionbank.extraction.dto;

import com.examplatform.questionbank.extraction.domain.PaperExtractionStatus;
import com.examplatform.questionbank.extraction.domain.QuestionPaperMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO representing question paper extraction metadata and telemetry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionPaperMetadataResponse {

    private UUID id;
    private Instant createdAt;
    private Instant updatedAt;
    private String tenantId;
    private Long version;

    private String examinationName;
    private Integer examYear;
    private String department;
    private String paperCode;
    private String bookletSerialNo;
    private String subjectName;
    private Long defaultSubjectId;
    private Long defaultTopicId;
    private Long defaultSubtopicId;

    @Builder.Default
    private List<String> languages = new ArrayList<>();

    private int totalPages;
    private int totalQuestionsExpected;
    private Integer durationMinutes;
    private Double maxMarks;
    private Double negativeMarkingRatio;

    @Builder.Default
    private List<Map<String, Object>> sectionsMetadata = new ArrayList<>();

    private UUID fileAssetId;
    private String originalFilename;
    private String fileStoragePath;
    private String fileSha256;
    private PaperExtractionStatus extractionStatus;
    private String modelUsed;
    private int pagesProcessed;
    private int questionsExtracted;
    private int questionsApproved;
    private int questionsPromoted;

    @Builder.Default
    private List<Map<String, Object>> failedPages = new ArrayList<>();

    private String errorMessage;
    private UUID uploadedBy;
    private Instant startedAt;
    private Instant completedAt;

    private double progressPercentage;

    /**
     * Converts a {@link QuestionPaperMetadata} domain entity into a {@link QuestionPaperMetadataResponse} DTO.
     */
    public static QuestionPaperMetadataResponse fromEntity(QuestionPaperMetadata entity) {
        if (entity == null) {
            return null;
        }

        double progress = 0.0;
        if (entity.getTotalPages() > 0) {
            progress = Math.min(100.0, (entity.getPagesProcessed() * 100.0) / entity.getTotalPages());
        } else if (entity.getExtractionStatus() == PaperExtractionStatus.COMPLETED) {
            progress = 100.0;
        }

        return QuestionPaperMetadataResponse.builder()
                .id(entity.getId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .tenantId(entity.getTenantId())
                .version(entity.getVersion())
                .examinationName(entity.getExaminationName())
                .examYear(entity.getExamYear())
                .department(entity.getDepartment())
                .paperCode(entity.getPaperCode())
                .bookletSerialNo(entity.getBookletSerialNo())
                .subjectName(entity.getSubjectName())
                .defaultSubjectId(entity.getDefaultSubjectId())
                .defaultTopicId(entity.getDefaultTopicId())
                .defaultSubtopicId(entity.getDefaultSubtopicId())
                .languages(entity.getLanguages() != null ? new ArrayList<>(entity.getLanguages()) : new ArrayList<>())
                .totalPages(entity.getTotalPages())
                .totalQuestionsExpected(entity.getTotalQuestionsExpected())
                .durationMinutes(entity.getDurationMinutes())
                .maxMarks(entity.getMaxMarks())
                .negativeMarkingRatio(entity.getNegativeMarkingRatio())
                .sectionsMetadata(entity.getSectionsMetadata() != null ? new ArrayList<>(entity.getSectionsMetadata()) : new ArrayList<>())
                .fileAssetId(entity.getFileAssetId())
                .originalFilename(entity.getOriginalFilename())
                .fileStoragePath(entity.getFileStoragePath())
                .fileSha256(entity.getFileSha256())
                .extractionStatus(entity.getExtractionStatus())
                .modelUsed(entity.getModelUsed())
                .pagesProcessed(entity.getPagesProcessed())
                .questionsExtracted(entity.getQuestionsExtracted())
                .questionsApproved(entity.getQuestionsApproved())
                .questionsPromoted(entity.getQuestionsPromoted())
                .failedPages(entity.getFailedPages() != null ? new ArrayList<>(entity.getFailedPages()) : new ArrayList<>())
                .errorMessage(entity.getErrorMessage())
                .uploadedBy(entity.getUploadedBy())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .progressPercentage(progress)
                .build();
    }
}
