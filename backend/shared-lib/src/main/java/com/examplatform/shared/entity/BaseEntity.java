package com.examplatform.shared.entity;

import com.examplatform.shared.tenant.TenantContext;
import com.examplatform.shared.util.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA mapped superclass inherited by every persistent entity in the platform.
 *
 * <h3>Fields</h3>
 * <ul>
 *   <li>{@link #id} — UUID v4 primary key, generated before first persist</li>
 *   <li>{@link #createdAt} — set once by {@link PrePersist}; never updated</li>
 *   <li>{@link #updatedAt} — refreshed on every {@link PreUpdate}</li>
 *   <li>{@link #tenantId} — examination authority identifier; populated from
 *       {@link TenantContext} on persist and enforced via application-level
 *       row filtering in service queries</li>
 *   <li>{@link #version} — optimistic-lock counter managed by the JPA
 *       provider; prevents lost-update anomalies under high concurrency</li>
 * </ul>
 *
 * <h3>Virtual-thread / Loom note</h3>
 * <p>{@link PrePersist} and {@link PreUpdate} hooks run on the same thread as
 * the JPA flush. Because {@link TenantContext} uses {@link InheritableThreadLocal},
 * the tenant value is available on virtual threads spawned from the
 * request-handling thread.
 */
@MappedSuperclass
public abstract class BaseEntity {

    /**
     * Surrogate primary key — UUID v7.
     * Generated in {@link #prePersist()} to keep primary key assignment
     * in application code rather than delegating to the database sequence.
     * UUID v7 is time-ordered (48-bit ms timestamp prefix), yielding
     * near-sequential B-tree inserts and eliminating the index page-splits
     * caused by random v4 UUIDs at scale.
     * This also supports offline key generation in offline delivery mode.
     */
    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    /**
     * Timestamp of the row's first insertion.
     * Set once by {@link #prePersist()}; marked {@code updatable = false}
     * so the JPA provider never generates an UPDATE for this column.
     */
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    /**
     * Timestamp of the most recent modification.
     * Refreshed on every flush via {@link #preUpdate()}.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Examination authority (tenant) that owns this record.
     * Sourced from {@link TenantContext#get()} at persist time.
     * Marked {@code updatable = false} — tenant ownership must not change
     * after creation.
     */
    @Column(name = "tenant_id", updatable = false, nullable = false, columnDefinition = "varchar(255)")
    private String tenantId;

    /**
     * Optimistic-lock version counter.
     * Incremented automatically by Hibernate on every UPDATE.
     * A stale write (concurrent modification) results in
     * {@link jakarta.persistence.OptimisticLockException}.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // -----------------------------------------------------------------------
    // JPA lifecycle hooks
    // -----------------------------------------------------------------------

    /**
     * Called by the JPA provider immediately before the entity is first
     * inserted into the database.
     *
     * <ul>
     *   <li>Generates a UUID v7 if no {@link #id} was set by a subclass.</li>
     *   <li>Sets {@link #createdAt} and {@link #updatedAt} to {@code Instant.now()}.</li>
     *   <li>Copies the tenant identifier from {@link TenantContext}.
     *       Falls back to {@code "default"} for background jobs that run
     *       without a request context (e.g. scheduled tasks).</li>
     * </ul>
     */
    @PrePersist
    protected void prePersist() {
        if (this.id == null) {
            this.id = UuidV7Generator.generate();
        }
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.tenantId == null) {
            String contextTenant = TenantContext.get();
            this.tenantId = (contextTenant != null) ? contextTenant : "default";
        }
    }

    /**
     * Called by the JPA provider immediately before an UPDATE statement is
     * issued for this entity.
     *
     * <p>Refreshes {@link #updatedAt} to the current UTC instant.
     */
    @PreUpdate
    protected void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    /**
     * Protected setter allows subclasses to assign a predetermined UUID
     * (e.g. when reconstructing an entity from an event or a test fixture).
     *
     * @param id the UUID to assign
     */
    protected void setId(UUID id) {
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
     * Allows explicit tenant assignment before the entity is persisted —
     * useful when creating entities in background jobs where
     * {@link TenantContext} is not populated.
     *
     * @param tenantId the examination authority identifier
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getVersion() {
        return version;
    }

    // -----------------------------------------------------------------------
    // equals / hashCode — identity based on id only
    // -----------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity other)) return false;
        if (this.id == null || other.id == null) return false;
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "{id=" + id
                + ", tenantId='" + tenantId + '\''
                + ", version=" + version
                + '}';
    }
}
