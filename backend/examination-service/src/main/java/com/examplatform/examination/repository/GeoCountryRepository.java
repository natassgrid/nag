package com.examplatform.examination.repository;

import com.examplatform.examination.domain.GeoCountry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeoCountryRepository extends JpaRepository<GeoCountry, Long> {
    List<GeoCountry> findByActiveTrueOrderByNameAsc();
}
