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

package com.examplatform.shared.entity;

import com.examplatform.shared.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;

/**
 * JPA mapped superclass for entities that use a compact numeric ({@code BIGINT})
 * surrogate primary key instead of a UUID.
 *
 * <h3>Why numeric IDs for reference/lookup tables</h3>
 * <p>Small, slowly-changing reference tables (subjects, topics, subtopics) are
 * referenced by foreign keys from very large fact tables (e.g. {@code question}
 * at 100M+ rows). A {@code BIGINT} foreign key is 8 bytes versus 16 bytes for a
 * UUID, which halves index and row storage on the referencing side, improves
 * cache locality, and speeds up joins. Database-generated {@code IDENTITY}
 * values are monotonically increasing, giving sequential B-tree inserts without
 * the application-side generation that UUIDs require.
 *
 * <h3>Fields</h3>
 * <ul>
 *   <li>{@link #id} — {@code BIGINT} identity primary key, assigned by the
 *       database on insert.</li>
 *   <li>{@link #createdAt} — set once by {@link #prePersist()}; never updated.</li>
 *   <li>{@link #updatedAt} — refreshed on every {@link #preUpdate()}.</li>
 *   <li>{@link #tenantId} — examination authority identifier, populated from
 *       {@link TenantContext} on persist.</li>
 *   <li>{@link #version} — optimistic-lock counter managed by the JPA provider.</li>
 * </ul>
 *
 * <p>This superclass intentionally mirrors {@link BaseEntity} for timestamp,
 * tenant, and version handling so services can treat both families uniformly.
 */
@MappedSuperclass
public abstract class NumericBaseEntity {

    /**
     * Surrogate primary key — database-generated {@code BIGINT} identity.
     * Assigned by PostgreSQL {@code GENERATED ... AS IDENTITY} on insert.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    /** Timestamp of the row's first insertion. Set once; never updated. */
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    /** Timestamp of the most recent modification. Refreshed on every flush. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Examination authority (tenant) that owns this record.
     * Sourced from {@link TenantContext#get()} at persist time.
     */
    @Column(name = "tenant_id", updatable = false, nullable = false, columnDefinition = "varchar(255)")
    private String tenantId;

    /** Optimistic-lock version counter, incremented by Hibernate on UPDATE. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.tenantId == null) {
            String contextTenant = TenantContext.get();
            this.tenantId = (contextTenant != null) ? contextTenant : "default";
        }
    }

    @PreUpdate
    protected void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    /**
     * Allows explicit id assignment when reconstructing an entity (e.g. tests
     * or event replay). Normal persistence relies on the database identity.
     */
    protected void setId(Long id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getTenantId() {
        return tenantId;
    }

    /**
     * Allows explicit tenant assignment before persist — useful in background
     * jobs where {@link TenantContext} is not populated.
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getVersion() {
        return version;
    }

    protected void setVersion(Long version) {
        this.version = version;
    }

    // -----------------------------------------------------------------------
    // Identity semantics — equal when non-null ids match, same as BaseEntity
    // -----------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NumericBaseEntity that = (NumericBaseEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
