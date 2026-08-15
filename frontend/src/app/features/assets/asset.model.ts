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

/**
 * Asset models matching the backend asset-service DTOs.
 */

export interface AssetResponse {
  id: string;
  originalFilename: string;
  contentType: string;
  extension: string;
  fileSize: number;
  sha256Hash: string;
  assetType: AssetType;
  status: AssetStatus;
  width?: number;
  height?: number;
  dpi?: number;
  orientation?: string;
  durationSeconds?: number;
  codec?: string;
  bitrate?: number;
  sampleRate?: number;
  channels?: number;
  frameRate?: number;
  title?: string;
  description?: string;
  altText?: string;
  tags?: string;
  language?: string;
  storageProvider: string;
  storageLocation: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  tenantId: string;
}

export type AssetType = 'IMAGE' | 'AUDIO' | 'VIDEO' | 'DOCUMENT';
export type AssetStatus = 'ACTIVE' | 'ARCHIVED' | 'DELETED';

export interface AssetMetadataUpdate {
  title?: string;
  description?: string;
  altText?: string;
  tags?: string;
  language?: string;
}

export interface AssetSearchParams {
  filename?: string;
  assetType?: AssetType;
  contentType?: string;
  tags?: string;
  status?: AssetStatus;
  storageProvider?: string;
  page?: number;
  size?: number;
}
