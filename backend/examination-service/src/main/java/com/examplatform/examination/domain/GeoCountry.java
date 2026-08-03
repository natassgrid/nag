package com.examplatform.examination.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Read-only geo lookup entity: Country.
 * Populated from production DB dump; seed data in V2 migration for dev.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "geo_country", schema = "examination_service")
public class GeoCountry {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "iso2", length = 2)
    private String iso2;

    @Column(name = "iso3", length = 3)
    private String iso3;

    @Column(name = "phone_code", length = 20)
    private String phoneCode;

    @Column(name = "capital", length = 100)
    private String capital;

    @Column(name = "currency", length = 50)
    private String currency;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "subregion", length = 100)
    private String subregion;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
