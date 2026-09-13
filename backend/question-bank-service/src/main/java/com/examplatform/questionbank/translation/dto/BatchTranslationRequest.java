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

package com.examplatform.questionbank.translation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTranslationRequest {

    @Builder.Default
    private String sourceLanguage = "en";

    @Builder.Default
    private String targetLanguage = "hi";

    @Builder.Default
    private String targetStatus = "PUBLISHED";

    private String subject;

    @Builder.Default
    private Boolean overwriteExisting = true;

    @Min(1)
    @Max(500)
    @Builder.Default
    private Integer batchSize = 50;

    @Min(0)
    @Max(5000)
    @Builder.Default
    private Integer throttleDelayMs = 50;

    @Min(1)
    @Max(10)
    @Builder.Default
    private Integer maxConcurrency = 2;
}
