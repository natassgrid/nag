package com.examplatform.examination.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * An examination centre where one or more shifts may be conducted.
 *
 * <p>Centres are tenant-scoped and reusable across multiple examinations
 * and schedules. Each centre records its full physical location hierarchy
 * (region → state → district → city → building → floor → laboratory).
 *
 * Validates: Requirements 7b.5, 7b.11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "examination_centre", schema = "examination_service")
public class ExaminationCentre extends BaseEntity {

    /** FK → geo_country.id (cascading dropdown selection). */
    @Column(name = "country_id")
    private Long countryId;

    /** FK → geo_state.id (cascading dropdown selection). */
    @Column(name = "state_id")
    private Long stateId;

    /** FK → geo_city.id (cascading dropdown selection). */
    @Column(name = "city_id")
    private Long cityId;

    /** Denormalized region name (for display/filter without joins). */
    @Column(name = "region", length = 100)
    private String region;

    /** Denormalized state name. */
    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "district", length = 100)
    private String district;

    /** Denormalized city name. */
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "centre_name", nullable = false, length = 255)
    private String centreName;

    @Column(name = "building", length = 255)
    private String building;

    @Column(name = "floor", length = 50)
    private String floor;

    /** E.g. "LAB-A", "LAB-B". Unique identifier for the lab within the centre. */
    @Column(name = "laboratory_identifier", length = 100)
    private String laboratoryIdentifier;

    /** Maximum number of candidates that can be seated across all labs. */
    @Column(name = "total_capacity", nullable = false)
    @Builder.Default
    private int totalCapacity = 0;

    /** Whether this centre is currently active and eligible for allocation. */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
