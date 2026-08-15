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

package com.examplatform.examination.controller;

import com.examplatform.examination.domain.GeoCity;
import com.examplatform.examination.domain.GeoCountry;
import com.examplatform.examination.domain.GeoState;
import com.examplatform.examination.service.GeoLocationService;
import com.examplatform.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public (no auth required) REST controller for cascading geo-location lookups.
 * Used by the frontend to populate Country → State → City dropdowns.
 *
 * <pre>
 *   GET /api/v1/geo/countries
 *   GET /api/v1/geo/countries/{countryId}/states
 *   GET /api/v1/geo/states/{stateId}/cities
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/geo")
@RequiredArgsConstructor
public class GeoLocationController {

    private final GeoLocationService geoLocationService;

    @GetMapping("/countries")
    public ResponseEntity<ApiResponse<List<GeoCountry>>> getCountries() {
        List<GeoCountry> countries = geoLocationService.getCountries();
        return ResponseEntity.ok(ApiResponse.success(countries, "Countries retrieved"));
    }

    @GetMapping("/countries/{countryId}/states")
    public ResponseEntity<ApiResponse<List<GeoState>>> getStates(@PathVariable Long countryId) {
        List<GeoState> states = geoLocationService.getStatesByCountry(countryId);
        return ResponseEntity.ok(ApiResponse.success(states, "States retrieved"));
    }

    @GetMapping("/states/{stateId}/cities")
    public ResponseEntity<ApiResponse<List<GeoCity>>> getCities(@PathVariable Long stateId) {
        List<GeoCity> cities = geoLocationService.getCitiesByState(stateId);
        return ResponseEntity.ok(ApiResponse.success(cities, "Cities retrieved"));
    }
}
