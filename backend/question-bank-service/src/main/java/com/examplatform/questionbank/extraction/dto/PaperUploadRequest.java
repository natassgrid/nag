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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Request payload for uploading and initiating extraction on a Question Paper PDF.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperUploadRequest {

    @NotBlank
    private String examinationName;

    @NotNull
    private Integer examYear;

    private String department;

    private String paperCode;

    private String bookletSerialNo;

    private String subjectName;

    private Long defaultSubjectId;

    private Long defaultTopicId;

    private Long defaultSubtopicId;

    @Builder.Default
    private List<String> languages = new ArrayList<>(List.of("en", "hi"));

    private Integer totalQuestionsExpected;

    private Integer durationMinutes;

    private Double maxMarks;

    private Double negativeMarkingRatio;

    private boolean forceReExtract;

    /**
     * Optional asset UUID if the file was pre-uploaded via asset-service.
     */
    private java.util.UUID fileAssetId;
}
