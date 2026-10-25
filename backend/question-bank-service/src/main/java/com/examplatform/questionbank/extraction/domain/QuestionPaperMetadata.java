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

package com.examplatform.questionbank.extraction.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entity storing examination name, year, department, question booklet metadata,
 * and asynchronous batch extraction telemetry in {@code question_service.question_paper_metadata}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "question_paper_metadata", schema = "question_service")
public class QuestionPaperMetadata extends BaseEntity {

    @Column(name = "examination_name", nullable = false)
    private String examinationName;

    @Column(name = "exam_year", nullable = false)
    private Integer examYear;

    @Column(name = "department")
    private String department;

    @Column(name = "paper_code", length = 100)
    private String paperCode;

    @Column(name = "booklet_serial_no", length = 100)
    private String bookletSerialNo;

    @Column(name = "subject_name")
    private String subjectName;

    @Column(name = "default_subject_id")
    private Long defaultSubjectId;

    @Column(name = "default_topic_id")
    private Long defaultTopicId;

    @Column(name = "default_subtopic_id")
    private Long defaultSubtopicId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "languages_json", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<String> languages = new ArrayList<>(List.of("en", "hi"));

    @Column(name = "total_pages", nullable = false)
    @Builder.Default
    private int totalPages = 0;

    @Column(name = "page_selection", length = 500)
    private String pageSelection;

    @Column(name = "last_completed_page", nullable = false)
    @Builder.Default
    private int lastCompletedPage = 0;

    @Column(name = "total_questions_expected", nullable = false)
    @Builder.Default
    private int totalQuestionsExpected = 0;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "max_marks")
    private Double maxMarks;

    @Column(name = "negative_marking_ratio")
    private Double negativeMarkingRatio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sections_metadata_json", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<Map<String, Object>> sectionsMetadata = new ArrayList<>();

    @Column(name = "file_asset_id")
    private UUID fileAssetId;

    @Column(name = "original_filename", length = 500)
    private String originalFilename;

    @Column(name = "file_storage_path", length = 1000)
    private String fileStoragePath;

    @Column(name = "file_sha256", length = 128)
    private String fileSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_status", nullable = false, length = 30)
    @Builder.Default
    private PaperExtractionStatus extractionStatus = PaperExtractionStatus.PENDING;

    @Column(name = "model_used", length = 100)
    private String modelUsed;

    @Column(name = "pages_processed", nullable = false)
    @Builder.Default
    private int pagesProcessed = 0;

    @Column(name = "questions_extracted", nullable = false)
    @Builder.Default
    private int questionsExtracted = 0;

    @Column(name = "questions_approved", nullable = false)
    @Builder.Default
    private int questionsApproved = 0;

    @Column(name = "questions_promoted", nullable = false)
    @Builder.Default
    private int questionsPromoted = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "failed_pages_json", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<Map<String, Object>> failedPages = new ArrayList<>();

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
