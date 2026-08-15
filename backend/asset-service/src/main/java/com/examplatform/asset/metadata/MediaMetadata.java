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

package com.examplatform.asset.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Container for extracted media metadata.
 * Fields are nullable — only those applicable to the asset type will be populated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaMetadata {

    // Image
    private Integer width;
    private Integer height;
    private Integer dpi;
    private String orientation;

    // Audio / Video
    private Double durationSeconds;
    private String codec;
    private Integer bitrate;
    private Integer sampleRate;
    private Integer channels;

    // Video
    private Double frameRate;
}
