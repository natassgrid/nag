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

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';

/**
 * Asset metadata returned by the Asset Service backend.
 */
export interface AssetMetadata {
  id: string;
  originalFilename: string;
  contentType: string;
  extension: string;
  fileSize: number;
  sha256Hash: string;
  assetType: 'IMAGE' | 'AUDIO' | 'VIDEO' | 'DOCUMENT';
  status: 'ACTIVE' | 'ARCHIVED' | 'DELETED';
  width?: number;
  height?: number;
  durationSeconds?: number;
  title?: string;
  description?: string;
  altText?: string;
  tags?: string;
  storageProvider: string;
  storageLocation: string;
  createdBy: string;
  createdAt: string;
}

/**
 * Paginated response envelope from the backend.
 */
interface ApiResponse<T> {
  status: string;
  message: string;
  data: T;
  timestamp: string;
}

interface PagedData<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

/**
 * Angular service for interacting with the Asset Management Service.
 *
 * Used by the editor to:
 * - Upload new assets
 * - Search existing assets for embedding
 * - Resolve asset metadata for rendering
 * - Get download URLs for display
 */
@Injectable({ providedIn: 'root' })
export class EditorAssetService {

  private readonly baseUrl = '/api/v1/assets';

  constructor(private http: HttpClient) {}

  /**
   * Upload a file to the Asset Service.
   */
  upload(file: File, tenantId: string): Observable<AssetMetadata> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<ApiResponse<AssetMetadata>>(this.baseUrl, formData, {
      headers: { 'X-Tenant-Id': tenantId }
    }).pipe(map(res => res.data));
  }

  /**
   * Get asset metadata by ID.
   */
  getAsset(assetId: string, tenantId: string): Observable<AssetMetadata> {
    return this.http.get<ApiResponse<AssetMetadata>>(`${this.baseUrl}/${assetId}`, {
      headers: { 'X-Tenant-Id': tenantId }
    }).pipe(map(res => res.data));
  }

  /**
   * Search assets with filters.
   */
  searchAssets(params: {
    assetType?: string;
    filename?: string;
    tags?: string;
    page?: number;
    size?: number;
  }, tenantId: string): Observable<PagedData<AssetMetadata>> {
    let httpParams = new HttpParams();
    if (params.assetType) httpParams = httpParams.set('assetType', params.assetType);
    if (params.filename) httpParams = httpParams.set('filename', params.filename);
    if (params.tags) httpParams = httpParams.set('tags', params.tags);
    httpParams = httpParams.set('page', String(params.page ?? 0));
    httpParams = httpParams.set('size', String(params.size ?? 20));

    return this.http.get<ApiResponse<PagedData<AssetMetadata>>>(`${this.baseUrl}/search`, {
      params: httpParams,
      headers: { 'X-Tenant-Id': tenantId }
    }).pipe(map(res => res.data));
  }

  /**
   * Get the download URL for an asset (for rendering in editor).
   */
  getDownloadUrl(assetId: string): string {
    return `${this.baseUrl}/${assetId}/download`;
  }

  /**
   * Get thumbnail URL for image/video assets.
   */
  getThumbnailUrl(assetId: string): string {
    return `${this.baseUrl}/${assetId}/download`;
  }
}
