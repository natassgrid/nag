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

package com.examplatform.candidate.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Candidate educational qualification and academic record entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "candidate_education", schema = "candidate_service")
public class CandidateEducation extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "qualification", nullable = false, length = 50)
    private String qualification;

    @Column(name = "course_name")
    private String courseName;

    @Column(name = "board_or_university", nullable = false)
    private String boardOrUniversity;

    @Column(name = "institution_name")
    private String institutionName;

    @Column(name = "passing_year", nullable = false)
    private Integer passingYear;

    @Column(name = "percentage_or_cgpa", precision = 5, scale = 2)
    private BigDecimal percentageOrCgpa;

    @Column(name = "grade_or_division", length = 50)
    private String gradeOrDivision;

    @Column(name = "specialization")
    private String specialization;

    @Column(name = "roll_number", length = 100)
    private String rollNumber;

    @Column(name = "certificate_asset_id")
    private UUID certificateAssetId;
}
