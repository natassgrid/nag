/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

export type AssetType = 'IMAGE' | 'AUDIO' | 'VIDEO' | 'DOCUMENT';
export type AssetStatus = 'ACTIVE' | 'ARCHIVED' | 'DELETED';

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
  tenantId?: string;
}

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
  sort?: string;
  order?: string;
  page?: number;
  size?: number;
}

export interface PaginatedAssetResponse {
  content: AssetResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ApiResponse<T> {
  status: string;
  message: string;
  data: T;
  timestamp: string;
}
