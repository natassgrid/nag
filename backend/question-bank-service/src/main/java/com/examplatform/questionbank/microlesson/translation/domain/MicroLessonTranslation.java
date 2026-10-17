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

package com.examplatform.questionbank.microlesson.translation.domain;

import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Entity representing localized translation of a SubtopicMicroLesson.
 */
@Entity
@Table(name = "microlesson_translation", schema = "question_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class MicroLessonTranslation extends BaseEntity {

    @Column(name = "microlesson_id", nullable = false, columnDefinition = "uuid")
    private UUID microlessonId;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @Column(name = "core_concept", nullable = false, columnDefinition = "text")
    private String coreConcept;

    @Column(name = "trap", nullable = false, columnDefinition = "text")
    private String trap;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cheat_sheet", nullable = false, columnDefinition = "jsonb")
    private String cheatSheetJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "step_by_step_breakdown", nullable = false, columnDefinition = "jsonb")
    private String stepByStepBreakdownJson;

    @Column(name = "source_version", nullable = false)
    @Builder.Default
    private int sourceVersion = 1;

    @Column(name = SharedConstants.STATUS, nullable = false, length = 20)
    @Builder.Default
    private String status = "PUBLISHED";

    @Column(name = "translator_id", columnDefinition = "uuid")
    private UUID translatorId;

    @Column(name = "reviewer_id", columnDefinition = "uuid")
    private UUID reviewerId;

    @Column(name = "review_comments", columnDefinition = "text")
    private String reviewComments;
}
