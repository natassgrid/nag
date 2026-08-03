package com.examplatform.examination.repository;

import com.examplatform.examination.domain.GeoState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeoStateRepository extends JpaRepository<GeoState, Long> {
    List<GeoState> findByCountryIdAndActiveTrueOrderByNameAsc(Long countryId);
}
