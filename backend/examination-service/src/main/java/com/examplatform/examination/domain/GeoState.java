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
 * Read-only geo lookup entity: State / Union Territory.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "geo_state", schema = "examination_service")
public class GeoState {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "country_id", nullable = false)
    private Long countryId;

    @Column(name = "state_code", length = 10)
    private String stateCode;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
