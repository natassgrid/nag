package com.examplatform.examination.repository;

import com.examplatform.examination.domain.GeoCity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeoCityRepository extends JpaRepository<GeoCity, Long> {
    List<GeoCity> findByStateIdAndActiveTrueOrderByNameAsc(Long stateId);
    List<GeoCity> findByCountryIdAndActiveTrueOrderByNameAsc(Long countryId);
}
