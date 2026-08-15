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

package com.examplatform.examination.dto.schedule;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating an examination centre.
 * Uses geo IDs (countryId, stateId, cityId) from cascading dropdown selection.
 * Validates: Requirements 7b.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCentreRequest {

    /** FK → geo_country.id — selected from Country dropdown. */
    private Long countryId;

    /** FK → geo_state.id — selected from State dropdown (filtered by countryId). */
    private Long stateId;

    /** FK → geo_city.id — selected from City dropdown (filtered by stateId). */
    private Long cityId;

    @Size(max = 100)
    private String region;

    @NotBlank
    @Size(max = 100)
    private String state;

    @Size(max = 100)
    private String district;

    @NotBlank
    @Size(max = 100)
    private String city;

    @NotBlank
    @Size(max = 255)
    private String centreName;

    @Size(max = 255)
    private String building;

    @Size(max = 50)
    private String floor;

    @Size(max = 100)
    private String laboratoryIdentifier;

    @Min(0)
    private int totalCapacity;

    @Builder.Default
    private boolean active = true;
}
