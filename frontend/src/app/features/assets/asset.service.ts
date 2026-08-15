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
import { HttpClient, HttpParams, HttpEvent, HttpRequest } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { AssetResponse, AssetMetadataUpdate, AssetSearchParams } from './asset.model';
import { PaginatedResponse } from '../../shared/components/paginated-table';

interface ApiResponse<T> {
  status: string;
  message: string;
  data: T;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class AssetService {

  private readonly baseUrl = '/api/v1/assets';

  constructor(private http: HttpClient) {}

  /**
   * Upload a file. Returns progress events for tracking.
   */
  uploadWithProgress(file: File): Observable<HttpEvent<ApiResponse<AssetResponse>>> {
    const formData = new FormData();
    formData.append('file', file);

    const req = new HttpRequest('POST', this.baseUrl, formData, {
      reportProgress: true
    });
    return this.http.request<ApiResponse<AssetResponse>>(req);
  }

  /**
   * Simple upload returning the response.
   */
  upload(file: File): Observable<AssetResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<AssetResponse>>(this.baseUrl, formData)
      .pipe(map(res => res.data));
  }

  /**
   * Get asset by ID.
   */
  getAsset(id: string): Observable<AssetResponse> {
    return this.http.get<ApiResponse<AssetResponse>>(`${this.baseUrl}/${id}`)
      .pipe(map(res => res.data));
  }

  /**
   * List assets with pagination.
   */
  listAssets(params: { page: number; size: number; search?: string }): Observable<PaginatedResponse<AssetResponse>> {
    let httpParams = new HttpParams()
      .set('page', String(params.page))
      .set('size', String(params.size));

    return this.http.get<ApiResponse<PaginatedResponse<AssetResponse>>>(this.baseUrl, { params: httpParams })
      .pipe(map(res => res.data));
  }

  /**
   * Search assets with filters.
   */
  searchAssets(params: AssetSearchParams): Observable<PaginatedResponse<AssetResponse>> {
    let httpParams = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20));

    if (params.filename) httpParams = httpParams.set('filename', params.filename);
    if (params.assetType) httpParams = httpParams.set('assetType', params.assetType);
    if (params.contentType) httpParams = httpParams.set('contentType', params.contentType);
    if (params.tags) httpParams = httpParams.set('tags', params.tags);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.storageProvider) httpParams = httpParams.set('storageProvider', params.storageProvider);

    return this.http.get<ApiResponse<PaginatedResponse<AssetResponse>>>(`${this.baseUrl}/search`, { params: httpParams })
      .pipe(map(res => res.data));
  }

  /**
   * Update asset metadata.
   */
  updateMetadata(id: string, update: AssetMetadataUpdate): Observable<AssetResponse> {
    return this.http.put<ApiResponse<AssetResponse>>(`${this.baseUrl}/${id}/metadata`, update)
      .pipe(map(res => res.data));
  }

  /**
   * Soft-delete an asset.
   */
  deleteAsset(id: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`)
      .pipe(map(() => undefined));
  }

  /**
   * Archive an asset.
   */
  archiveAsset(id: string): Observable<AssetResponse> {
    return this.http.put<ApiResponse<AssetResponse>>(`${this.baseUrl}/${id}/archive`, {})
      .pipe(map(res => res.data));
  }

  /**
   * Restore an archived asset.
   */
  restoreAsset(id: string): Observable<AssetResponse> {
    return this.http.put<ApiResponse<AssetResponse>>(`${this.baseUrl}/${id}/restore`, {})
      .pipe(map(res => res.data));
  }

  /**
   * Get the download URL for an asset.
   */
  getDownloadUrl(id: string): string {
    return `${this.baseUrl}/${id}/download`;
  }

  /**
   * Format file size for display.
   */
  formatFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }
}
