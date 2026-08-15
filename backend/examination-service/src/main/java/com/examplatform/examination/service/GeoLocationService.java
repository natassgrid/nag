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

package com.examplatform.examination.service;

import com.examplatform.examination.domain.GeoCity;
import com.examplatform.examination.domain.GeoCountry;
import com.examplatform.examination.domain.GeoState;
import com.examplatform.examination.repository.GeoCityRepository;
import com.examplatform.examination.repository.GeoCountryRepository;
import com.examplatform.examination.repository.GeoStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Read-only service for cascading geo-location lookups:
 * countries → states (by countryId) → cities (by stateId).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GeoLocationService {

    private final GeoCountryRepository countryRepository;
    private final GeoStateRepository stateRepository;
    private final GeoCityRepository cityRepository;

    // ── List lookups ──────────────────────────────────────────────────────────

    public List<GeoCountry> getCountries() {
        return countryRepository.findByActiveTrueOrderByNameAsc();
    }

    public List<GeoState> getStatesByCountry(Long countryId) {
        return stateRepository.findByCountryIdAndActiveTrueOrderByNameAsc(countryId);
    }

    public List<GeoCity> getCitiesByState(Long stateId) {
        return cityRepository.findByStateIdAndActiveTrueOrderByNameAsc(stateId);
    }

    // ── Single-record lookups (for name resolution) ───────────────────────────

    public Optional<GeoCountry> getCountryById(Long countryId) {
        if (countryId == null) return Optional.empty();
        return countryRepository.findById(countryId);
    }

    public Optional<GeoState> getStateByCountryIdAndStateId(Long countryId, Long stateId) {
        if (countryId == null || stateId == null) return Optional.empty();
        return stateRepository.findById(stateId)
                .filter(s -> countryId.equals(s.getCountryId()));
    }

    public Optional<GeoCity> getCityByStateIdAndCityId(Long stateId, Long cityId) {
        if (stateId == null || cityId == null) return Optional.empty();
        return cityRepository.findById(cityId)
                .filter(c -> stateId.equals(c.getStateId()));
    }
}
