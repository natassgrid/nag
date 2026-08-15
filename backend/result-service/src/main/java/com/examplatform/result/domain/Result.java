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

package com.examplatform.result.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents the final result for a candidate in an exam.
 * Includes total score, section-wise scores (JSONB), ranking data,
 * and references to generated scorecard PDFs.
 */
@Entity
@Table(name = "result", schema = "result_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Result extends BaseEntity {

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "total_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "section_scores_json", columnDefinition = "jsonb")
    private String sectionScoresJson;

    @Column(name = "overall_rank")
    private Integer overallRank;

    @Column(name = "overall_percentile", precision = 6, scale = 3)
    private BigDecimal overallPercentile;

    @Column(name = "scorecard_pdf_ref", length = 500)
    private String scorecardPdfRef;

    @Column(name = "digi_locker_pushed", nullable = false)
    private Boolean digiLockerPushed;
}
