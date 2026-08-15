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

package com.examplatform.asset.domain.entity;

import com.examplatform.asset.domain.enums.AssetStatus;
import com.examplatform.asset.domain.enums.AssetType;
import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistent entity representing a multimedia asset stored in the platform.
 * Binary content is stored externally via a pluggable storage provider;
 * this entity holds only metadata and the storage reference.
 *
 * <p>Assets are immutable after upload — only metadata fields
 * (title, description, altText, tags, status) may be updated.
 */
@Entity
@Table(name = "media_asset", schema = "asset_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaAsset extends BaseEntity {

    // ── File identity ────────────────────────────────────────────────────────

    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "extension", length = 20)
    private String extension;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;

    // ── Classification ───────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AssetStatus status;

    // ── Media-specific metadata ──────────────────────────────────────────────

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "dpi")
    private Integer dpi;

    @Column(name = "orientation", length = 20)
    private String orientation;

    @Column(name = "duration_seconds")
    private Double durationSeconds;

    @Column(name = "codec", length = 50)
    private String codec;

    @Column(name = "bitrate")
    private Integer bitrate;

    @Column(name = "sample_rate")
    private Integer sampleRate;

    @Column(name = "channels")
    private Integer channels;

    @Column(name = "frame_rate")
    private Double frameRate;

    // ── User-supplied metadata ───────────────────────────────────────────────

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "alt_text", length = 1000)
    private String altText;

    @Column(name = "tags", length = 2000)
    private String tags;

    @Column(name = "language", length = 10)
    private String language;

    // ── Storage reference ────────────────────────────────────────────────────

    @Column(name = "storage_provider", nullable = false, length = 50)
    private String storageProvider;

    @Column(name = "storage_location", nullable = false, length = 2000)
    private String storageLocation;

    // ── Authoring ────────────────────────────────────────────────────────────

    @Column(name = "created_by", nullable = false, columnDefinition = "uuid")
    private java.util.UUID createdBy;
}
