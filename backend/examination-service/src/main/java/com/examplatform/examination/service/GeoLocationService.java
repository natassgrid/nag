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
