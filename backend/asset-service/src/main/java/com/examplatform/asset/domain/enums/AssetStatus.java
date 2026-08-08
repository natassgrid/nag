package com.examplatform.asset.domain.enums;

/**
 * Lifecycle states for a media asset.
 *
 * <p>Valid transitions:
 * <pre>
 *   ACTIVE   → ARCHIVED
 *   ACTIVE   → DELETED (soft-delete, only if unreferenced)
 *   ARCHIVED → ACTIVE  (restore)
 *   ARCHIVED → DELETED (soft-delete, only if unreferenced)
 * </pre>
 */
public enum AssetStatus {

    /** Asset is available for use and referencing. */
    ACTIVE,

    /** Asset has been archived; not available for new references but retained. */
    ARCHIVED,

    /** Soft-deleted; not visible in normal queries. Retained for audit. */
    DELETED
}
